package com.remote.server;

import com.remote.common.ImageUtils;
import com.remote.common.Protocol;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.*;
import java.util.Arrays;
import java.util.Random;

public class ServerApp {

    private static final int TILE_SIZE = 100;

    public static String myPassword = "----";

    public static void main(String[] args) {
        startBackgroundServer();

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
        }
    }

    public static void startBackgroundServer() {
        new Thread(() -> {

            generatePassword();
            System.out.println(">>> PASSWORD KET NOI: " + myPassword + " <<<");

            startDiscoveryService();

            try (ServerSocket serverSocket = new ServerSocket(Protocol.PORT)) {
                System.out.println("SERVER dang chay ngam tai Port " + Protocol.PORT + "...");

                NativeScreenCapture nativeCapturer = new NativeScreenCapture();
                Robot robot = new Robot();
                Rectangle rect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
                final int[][] prevPixelsRef = { null };

                while (true) {
                    Socket socket = serverSocket.accept();

                    new Thread(() -> {
                        try {
                            DataInputStream dis = new DataInputStream(socket.getInputStream());
                            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

                            int cmd = dis.readInt();
                            if (cmd == Protocol.CMD_AUTH_REQUEST) {
                                String clientPass = dis.readUTF();

                                if (myPassword.equals(clientPass)) {
                                    // Mật khẩu đúng
                                    dos.writeInt(Protocol.CMD_AUTH_OK);
                                    dos.flush();
                                    System.out
                                            .println("Client " + socket.getInetAddress() + " da xac thuc THANH CONG!");

                                    handleSession(socket, dis, dos, rect, nativeCapturer, prevPixelsRef, robot);
                                } else {

                                    dos.writeInt(Protocol.CMD_AUTH_FAIL);
                                    dos.flush();
                                    System.out.println("Client nhap sai mat khau -> Tu choi ket noi.");
                                    socket.close();
                                }
                            } else {
                                socket.close();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void generatePassword() {
        Random rand = new Random();
        int num = 100000 + rand.nextInt(900000);
        myPassword = String.valueOf(num);
    }

    private static void startDiscoveryService() {
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(Protocol.UDP_PORT)) {
                socket.setBroadcast(true);
                byte[] buf = new byte[1024];
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);

                    String msg = new String(packet.getData(), 0, packet.getLength()).trim();
                    if (msg.equals(Protocol.DISCOVERY_REQ)) {
                        String hostName = InetAddress.getLocalHost().getHostName();
                        String response = Protocol.DISCOVERY_RES + ";" + hostName;
                        byte[] data = response.getBytes();

                        DatagramPacket sendPacket = new DatagramPacket(data, data.length, packet.getAddress(),
                                packet.getPort());
                        socket.send(sendPacket);
                    }
                }
            } catch (Exception e) {
                System.out.println("Luu y UDP: " + e.getMessage());
            }
        }).start();
    }

    // Logic xử lý chính (Gửi ảnh & Nhận chuột)
    private static void handleSession(Socket socket, DataInputStream dis, DataOutputStream dos,
            Rectangle rect, NativeScreenCapture nativeCapturer,
            int[][] prevPixelsRef, Robot robot) {

        // --- LUỒNG 1: Gửi màn hình (Screen Sender) ---
        new Thread(() -> {
            try {
                // Gửi kích thước màn hình
                dos.writeInt(Protocol.CMD_SCREEN_SIZE);
                dos.writeInt(rect.width);
                dos.writeInt(rect.height);
                dos.flush();

                while (!socket.isClosed()) {
                    long start = System.currentTimeMillis();

                    // 1. Chụp màn hình (C++ Fast Capture)
                    int[] currentPixels = nativeCapturer.captureScreen(rect.x, rect.y, rect.width, rect.height);

                    // Khởi tạo mảng prev nếu chưa có
                    if (prevPixelsRef[0] == null) {
                        prevPixelsRef[0] = new int[currentPixels.length];
                        Arrays.fill(prevPixelsRef[0], 0);
                    }
                    int[] prevPixels = prevPixelsRef[0];

                    // 2. Chia lưới & So sánh (Grid & Diff)
                    int cols = (int) Math.ceil((double) rect.width / TILE_SIZE);
                    int rows = (int) Math.ceil((double) rect.height / TILE_SIZE);

                    for (int y = 0; y < rows; y++) {
                        for (int x = 0; x < cols; x++) {
                            int tileX = x * TILE_SIZE;
                            int tileY = y * TILE_SIZE;
                            int w = Math.min(TILE_SIZE, rect.width - tileX);
                            int h = Math.min(TILE_SIZE, rect.height - tileY);

                            if (isTileChangedRaw(currentPixels, prevPixels, tileX, tileY, w, h, rect.width)) {

                                // Cắt mảng con
                                int[] tilePixels = getTilePixels(currentPixels, tileX, tileY, w, h, rect.width);

                                // Nén Zlib (nhanh hơn JPEG)
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

                                // Cập nhật bộ nhớ đệm
                                updatePrevPixels(currentPixels, prevPixels, tileX, tileY, w, h, rect.width);
                            }
                        }
                    }

                    dos.flush();

                    // Giữ FPS ổn định ~60
                    long duration = System.currentTimeMillis() - start;
                    if (duration < 16)
                        Thread.sleep(16 - duration);
                }
            } catch (Exception e) {
                // Client ngắt kết nối
            }
        }).start();

        // --- LUỒNG 2: Nhận chuột/phím (Input Receiver) ---
        new Thread(() -> {
            try {
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