package com.remote.server;

import com.remote.common.Config;
import com.remote.common.ImageUtils;
import com.remote.common.Protocol;

import java.awt.*;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Arrays;

public class ScreenSender extends Thread {
    private Socket socket;
    private DataOutputStream dos;
    private Rectangle rect;
    private NativeWrapper nativeCapturer; // Dùng lại Native Wrapper (C++)

    public ScreenSender(Socket socket, DataOutputStream dos, Rectangle rect) {
        this.socket = socket;
        this.dos = dos;
        this.rect = rect;
        this.nativeCapturer = new NativeWrapper(); // Khởi tạo C++
    }

    @Override
    public void run() {
        try {
            dos.writeInt(Protocol.CMD_SCREEN_SIZE);
            dos.writeInt(rect.width);
            dos.writeInt(rect.height);
            dos.flush();

            final int[][] prevPixelsRef = { null };

            while (!socket.isClosed()) {
                long start = System.currentTimeMillis();

                // GỌI HÀM C++ (Nhanh hơn Robot nhiều)
                int[] currentPixels = nativeCapturer.captureScreenSafe(rect.x, rect.y, rect.width, rect.height);

                if (currentPixels == null)
                    continue;

                if (prevPixelsRef[0] == null) {
                    prevPixelsRef[0] = new int[currentPixels.length];
                    Arrays.fill(prevPixelsRef[0], 0);
                }
                int[] prevPixels = prevPixelsRef[0];

                // Logic chia Tile gửi đi (Giữ nguyên)
                int cols = (int) Math.ceil((double) rect.width / Config.TILE_SIZE);
                int rows = (int) Math.ceil((double) rect.height / Config.TILE_SIZE);

                for (int y = 0; y < rows; y++) {
                    for (int x = 0; x < cols; x++) {
                        int tileX = x * Config.TILE_SIZE;
                        int tileY = y * Config.TILE_SIZE;
                        int w = Math.min(Config.TILE_SIZE, rect.width - tileX);
                        int h = Math.min(Config.TILE_SIZE, rect.height - tileY);

                        if (isTileChangedRaw(currentPixels, prevPixels, tileX, tileY, w, h, rect.width)) {
                            int[] tilePixels = getTilePixels(currentPixels, tileX, tileY, w, h, rect.width);
                            byte[] data = ImageUtils.compressRaw(tilePixels, w, h);

                            synchronized (dos) {
                                dos.writeInt(Protocol.CMD_SEND_TILE);
                                dos.writeInt(tileX);
                                dos.writeInt(tileY);
                                dos.writeInt(w);
                                dos.writeInt(h);
                                dos.writeInt(data.length);
                                dos.write(data);
                            }
                            updatePrevPixels(currentPixels, prevPixels, tileX, tileY, w, h, rect.width);
                        }
                    }
                }

                synchronized (dos) {
                    dos.writeInt(Protocol.CMD_FRAME_END);
                }
                dos.flush();

                long duration = System.currentTimeMillis() - start;
                long sleepTime = 16 - duration;
                if (sleepTime > 0)
                    Thread.sleep(sleepTime);
            }
        } catch (Exception e) {
            System.out.println("ScreenSender stopped.");
        }
    }

    // Các hàm phụ trợ giữ nguyên
    private boolean isTileChangedRaw(int[] curr, int[] prev, int tx, int ty, int w, int h, int scanline) {
        int step = 2;
        for (int y = 0; y < h; y += step) {
            for (int x = 0; x < w; x += step) {
                int idx = (ty + y) * scanline + (tx + x);
                if (curr[idx] != prev[idx])
                    return true;
            }
        }
        return false;
    }

    private int[] getTilePixels(int[] src, int tx, int ty, int w, int h, int scanline) {
        int[] tile = new int[w * h];
        for (int y = 0; y < h; y++) {
            System.arraycopy(src, (ty + y) * scanline + tx, tile, y * w, w);
        }
        return tile;
    }

    private void updatePrevPixels(int[] src, int[] dest, int tx, int ty, int w, int h, int scanline) {
        for (int y = 0; y < h; y++) {
            int offset = (ty + y) * scanline + tx;
            System.arraycopy(src, offset, dest, offset, w);
        }
    }
}