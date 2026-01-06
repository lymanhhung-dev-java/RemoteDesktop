package com.remote.server;

import com.remote.common.Config;
import com.remote.common.Protocol;

import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

public class ServerMain {
    public static String myPassword = "123";

    public static String myID = ""; 
    public static final String RELAY_IP = "180.93.35.122";
    public static final int RELAY_PORT = 7777;

    public static void startBackgroundServer() {
        myPassword = String.valueOf(100000 + new Random().nextInt(900000));
        myID = String.valueOf(1000 + new Random().nextInt(9000));
        System.out.println(">>> PASSWORD: " + myPassword + " <<<");

        try {
            Robot robot = new Robot();
            Rectangle rect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());

            // 1. Chạy UDP Discovery
            new UdpDiscoveryServer().start();
            // 2. Chạy TCP Server
            new Thread(() -> {
                try (ServerSocket serverSocket = new ServerSocket(Config.TCP_PORT)) {
                    System.out.println("TCP Server listening on port " + Config.TCP_PORT);

                    while (true) {
                        Socket clientSocket = serverSocket.accept();
                        // Giao việc cho SessionHandler
                        new SessionHandler(clientSocket, robot, rect, myPassword).start();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
            new Thread(() -> connectToRelay(robot, rect)).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void connectToRelay(Robot robot, Rectangle rect) {
        while (true) { // Vòng lặp để tự kết nối lại nếu rớt mạng
            try {
                System.out.println("Connecting to Relay Server...");
                Socket socket = new Socket(RELAY_IP, RELAY_PORT);
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                DataInputStream dis = new DataInputStream(socket.getInputStream());

                // Gửi lệnh đăng ký ID
                dos.writeInt(Protocol.CMD_REGISTER_HOST);
                dos.writeUTF(myID);
                dos.flush();
                System.out.println("Connected to Relay! Waiting for Client...");

                // Đứng đợi tín hiệu từ Relay báo có người kết nối
                int response = dis.readInt();
                if (response == Protocol.CMD_CONNECT_SUCCESS) {
                    System.out.println("Client connected via Relay!");
                    // Chuyển giao Socket này cho SessionHandler xử lý tiếp (xác thực pass, gửi
                    // ảnh...)
                    new SessionHandler(socket, robot, rect, myPassword).run();
                }else{
                    socket.close();
                }

                
            } catch (Exception e) {
                System.out.println("Relay connection failed. Retrying in 5s...");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                }
            }
        }
    }

    public static void main(String[] args) {
        startBackgroundServer();
    }
}
