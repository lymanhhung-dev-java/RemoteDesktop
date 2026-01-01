package com.remote.server;

import com.remote.common.Config;
import java.awt.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

public class ServerMain {
    public static String myPassword = "123";

    public static void startBackgroundServer() {
        myPassword = String.valueOf(100000 + new Random().nextInt(900000));
        System.out.println(">>> PASSWORD: " + myPassword + " <<<");

        // 1. Chạy UDP Discovery
        new UdpDiscoveryServer().start();

        // 2. Chạy TCP Server
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(Config.TCP_PORT)) {
                Robot robot = new Robot();
                Rectangle rect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
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
    }
    
    public static void main(String[] args) {
        startBackgroundServer();
    }
}
