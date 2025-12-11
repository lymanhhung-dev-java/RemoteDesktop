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

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(Protocol.PORT)) {
            System.out.println("SERVER dang chay...");
            System.out.println("Dang cho ket noi tai Port: " + Protocol.PORT);

            Robot robot = new Robot();
            Rectangle rect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            // Rectangle rect = new Rectangle(0, 0, 800, 600);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client da ket noi: " + socket.getInetAddress());
                new Thread(() -> {
                    try (DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {
                        while (!socket.isClosed()) {
                            BufferedImage img = robot.createScreenCapture(rect);
                            byte[] data = ImageUtils.compress(img, 0.5f);

                            dos.writeInt(data.length);
                            dos.write(data);
                            dos.flush();

                            Thread.sleep(50);
                        }
                    } catch (Exception e) {
                        System.out.println("Ngat ket noi gui anh.");
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
}