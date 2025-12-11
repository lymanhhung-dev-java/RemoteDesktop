package com.remote.server;

import com.remote.common.ImageUtils;
import com.remote.common.Protocol;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

public class ServerApp {

    private static final int TILE_SIZE = 100;

    public static void main(String[] args) {

        try (ServerSocket serverSocket = new ServerSocket(Protocol.PORT)) {
            System.out.println("SERVER (Optimized: Direct Array + Zlib) running...");
            System.out.println("Dang cho ket noi tai Port: " + Protocol.PORT);

            // 1. Khởi tạo bộ chụp màn hình C++
            NativeScreenCapture nativeCapturer = new NativeScreenCapture();

            Robot robot = new Robot();

            Rectangle rect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());

            final int[][] prevPixelsRef = { null };

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client da ket noi: " + socket.getInetAddress());

                new Thread(() -> {
                    try (DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

                        dos.writeInt(Protocol.CMD_SCREEN_SIZE);
                        dos.writeInt(rect.width);
                        dos.writeInt(rect.height);
                        dos.flush();

                        while (!socket.isClosed()) {
                            long start = System.currentTimeMillis();

                            int[] currentPixels = nativeCapturer.captureScreen(rect.x, rect.y, rect.width, rect.height);

                            if (prevPixelsRef[0] == null) {
                                prevPixelsRef[0] = new int[currentPixels.length];
                                Arrays.fill(prevPixelsRef[0], 0);
                            }
                            int[] prevPixels = prevPixelsRef[0];

                            int cols = (int) Math.ceil((double) rect.width / TILE_SIZE);
                            int rows = (int) Math.ceil((double) rect.height / TILE_SIZE);
                            for (int y = 0; y < rows; y++) {
                                for (int x = 0; x < cols; x++) {
                                    int tileX = x * TILE_SIZE;
                                    int tileY = y * TILE_SIZE;
                                    int w = Math.min(TILE_SIZE, rect.width - tileX);
                                    int h = Math.min(TILE_SIZE, rect.height - tileY);

 
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

                            dos.flush();
                            long duration = System.currentTimeMillis() - start;
                            if (duration < 16)
                                Thread.sleep(16 - duration);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();

                new Thread(() -> {
                    try (DataInputStream dis = new DataInputStream(socket.getInputStream())) {
                        while (!socket.isClosed()) {
                            int type = dis.readInt();

                            if (type == Protocol.CMD_MOUSE_MOVE) {
                                int x = dis.readInt();
                                int y = dis.readInt();
                                robot.mouseMove(x, y);
                            } else if (type == Protocol.CMD_MOUSE_PRESS) {
                                robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
                            } else if (type == Protocol.CMD_MOUSE_RELEASE) {
                                robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
                            } else if (type == Protocol.CMD_KEY_PRESS) {
                                int keyCode = dis.readInt();
                                try {
                                    robot.keyPress(keyCode);
                                } catch (Exception ex) {
                                }
                            } else if (type == Protocol.CMD_KEY_RELEASE) {
                                int keyCode = dis.readInt();
                                try {
                                    robot.keyRelease(keyCode);
                                } catch (Exception ex) {
                                }
                            } else if (type == Protocol.CMD_MOUSE_WHEEL) {
                                int wheelAmt = dis.readInt();
                                try {
                                    robot.mouseWheel(wheelAmt);
                                } catch (Exception ex) {
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Ngat ket noi nhan lenh.");
                    }
                }).start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean isTileChangedRaw(int[] curr, int[] prev, int tx, int ty, int w, int h, int scanline) {
        int step = 2; 
        for (int y = 0; y < h; y += step) {
            for (int x = 0; x < w; x += step) {
                int index = (ty + y) * scanline + (tx + x);
                if (curr[index] != prev[index])
                    return true;
            }
        }
        return false;
    }

    private static int[] getTilePixels(int[] src, int tx, int ty, int w, int h, int scanline) {
        int[] tile = new int[w * h];
        for (int y = 0; y < h; y++) {
            System.arraycopy(src, (ty + y) * scanline + tx, tile, y * w, w);
        }
        return tile;
    }

    private static void updatePrevPixels(int[] src, int[] dest, int tx, int ty, int w, int h, int scanline) {
        for (int y = 0; y < h; y++) {
            int offset = (ty + y) * scanline + tx;
            System.arraycopy(src, offset, dest, offset, w);
        }
    }
}