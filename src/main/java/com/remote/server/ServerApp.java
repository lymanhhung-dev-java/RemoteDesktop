package com.remote.server;

import com.remote.common.ImageUtils;
import com.remote.common.Protocol;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerApp {

    private static final int TILE_SIZE = 100; 

    public static void main(String[] args) {

        try (ServerSocket serverSocket = new ServerSocket(Protocol.PORT)) {
            System.out.println("SERVER dang chay (Mode: C++ Fast Capture)...");
            System.out.println("Dang cho ket noi tai Port: " + Protocol.PORT);

            // 1. Khởi tạo bộ chụp màn hình C++ 
            NativeScreenCapture nativeCapturer = new NativeScreenCapture();

            Robot robot = new Robot();
        
            Rectangle rect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            
            final BufferedImage[] prevScreen = { null };

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client da ket noi: " + socket.getInetAddress());

                new Thread(() -> {
                    try (DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {
                        
                        // Gửi kích thước màn hình trước (để Client scale chuột chuẩn)
                        dos.writeInt(Protocol.CMD_SCREEN_SIZE);
                        dos.writeInt(rect.width);
                        dos.writeInt(rect.height);
                        dos.flush();

                        while (!socket.isClosed()) {
                            long start = System.currentTimeMillis();

                            int[] rawPixels = nativeCapturer.captureScreen(rect.x, rect.y, rect.width, rect.height);
                            BufferedImage currentScreen = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_RGB);
                            currentScreen.setRGB(0, 0, rect.width, rect.height, rawPixels, 0, rect.width);

                            int cols = (int) Math.ceil((double) currentScreen.getWidth() / TILE_SIZE);
                            int rows = (int) Math.ceil((double) currentScreen.getHeight() / TILE_SIZE);

                            boolean isFirstFrame = (prevScreen[0] == null);

                            for (int y = 0; y < rows; y++) {
                                for (int x = 0; x < cols; x++) {
                                    // Tính tọa độ cắt ô
                                    int tileX = x * TILE_SIZE;
                                    int tileY = y * TILE_SIZE;
                                    int w = Math.min(TILE_SIZE, currentScreen.getWidth() - tileX);
                                    int h = Math.min(TILE_SIZE, currentScreen.getHeight() - tileY);

                                    // Lấy ô ảnh con
                                    BufferedImage currentTile = currentScreen.getSubimage(tileX, tileY, w, h);

                                    boolean changed = true;
                                    if (!isFirstFrame) {
                                        BufferedImage prevTile = prevScreen[0].getSubimage(tileX, tileY, w, h);
                                        // So sánh xem có thay đổi không
                                        changed = isTileChanged(currentTile, prevTile);
                                    }

                                    // 3. Nếu thay đổi -> Nén và Gửi ô đó đi
                                    if (changed) {
                                        byte[] data = ImageUtils.compress(currentTile, 0.7f); // Chất lượng 70%

                                        synchronized (dos) {
                                            dos.writeInt(Protocol.CMD_SEND_TILE);
                                            dos.writeInt(tileX);
                                            dos.writeInt(tileY);
                                            dos.writeInt(w);
                                            dos.writeInt(h);
                                            dos.writeInt(data.length);
                                            dos.write(data);
                                        }
                                    }
                                }
                            }
                            
                            dos.flush();
                            prevScreen[0] = currentScreen;

                            // 4. Giữ FPS ổn định ~60 FPS (Mỗi khung hình ~16ms)
                            long duration = System.currentTimeMillis() - start;
                            if (duration < 16) {
                                Thread.sleep(16 - duration); 
                            }
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
                                try { robot.keyPress(keyCode); } catch (Exception ex) {}
                            } else if (type == Protocol.CMD_KEY_RELEASE) {
                                int keyCode = dis.readInt();
                                try { robot.keyRelease(keyCode); } catch (Exception ex) {}
                            } else if (type == Protocol.CMD_MOUSE_WHEEL) {
                                int wheelAmt = dis.readInt();
                                try { robot.mouseWheel(wheelAmt); } catch (Exception ex) {}
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

    private static boolean isTileChanged(BufferedImage img1, BufferedImage img2) {
        int w = img1.getWidth();
        int h = img1.getHeight();
        int step = 2; 

        for (int y = 0; y < h; y += step) {
            for (int x = 0; x < w; x += step) {
                if (img1.getRGB(x, y) != img2.getRGB(x, y)) {
                    return true;
                }
            }
        }
        return false;
    }
}