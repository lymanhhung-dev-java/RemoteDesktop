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
            System.out.println("SERVER dang chay...");
            System.out.println("Dang cho ket noi tai Port: " + Protocol.PORT);

            Robot robot = new Robot();
            Rectangle rect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            // Rectangle rect = new Rectangle(0, 0, 800, 600);
            final BufferedImage[] prevScreen = { null };

            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> {
                    try (DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {
                        dos.writeInt(Protocol.CMD_SCREEN_SIZE);
                        dos.writeInt(rect.width);
                        dos.writeInt(rect.height);
                        dos.flush();
                        while (!socket.isClosed()) {
                            // 1. Chụp toàn bộ màn hình
                            BufferedImage currentScreen = robot.createScreenCapture(rect);

                            // 2. Duyệt qua từng ô (Grid)
                            int cols = (int) Math.ceil((double) currentScreen.getWidth() / TILE_SIZE);
                            int rows = (int) Math.ceil((double) currentScreen.getHeight() / TILE_SIZE);

                            boolean isFirstFrame = (prevScreen[0] == null);

                            for (int y = 0; y < rows; y++) {
                                for (int x = 0; x < cols; x++) {
                                    // Tính tọa độ cắt
                                    int tileX = x * TILE_SIZE;
                                    int tileY = y * TILE_SIZE;
                                    int w = Math.min(TILE_SIZE, currentScreen.getWidth() - tileX);
                                    int h = Math.min(TILE_SIZE, currentScreen.getHeight() - tileY);

                                    // Lấy ô ảnh con (Subimage)
                                    BufferedImage currentTile = currentScreen.getSubimage(tileX, tileY, w, h);

                                    boolean changed = true;
                                    if (!isFirstFrame) {
                                        // Lấy ô tương ứng ở frame cũ
                                        BufferedImage prevTile = prevScreen[0].getSubimage(tileX, tileY, w, h);
                                        // So sánh nhanh
                                        changed = isTileChanged(currentTile, prevTile);
                                    }

                                    // 3. Nếu thay đổi -> Gửi ô đó đi
                                    if (changed) {
                                        byte[] data = ImageUtils.compress(currentTile, 0.7f);

                                        synchronized (dos) {
                                            dos.writeInt(Protocol.CMD_SEND_TILE);
                                            dos.writeInt(tileX); // Tọa độ X
                                            dos.writeInt(tileY); // Tọa độ Y
                                            dos.writeInt(w); // Chiều rộng ô
                                            dos.writeInt(h); // Chiều cao ô
                                            dos.writeInt(data.length); // Kích thước ảnh
                                            dos.write(data); // Dữ liệu ảnh
                                        }
                                    }
                                }
                            }

                            dos.flush();
                            prevScreen[0] = currentScreen;

                            Thread.sleep(10); // ~30 FPS
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
                                } catch (IllegalArgumentException ex) {

                                }
                            } else if (type == Protocol.CMD_KEY_RELEASE) {
                                int keyCode = dis.readInt();
                                try {
                                    robot.keyRelease(keyCode);
                                } catch (IllegalArgumentException ex) {
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

    private static boolean isTileChanged(BufferedImage img1, BufferedImage img2) {
        int w = img1.getWidth();
        int h = img1.getHeight();
        int step = 1;

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