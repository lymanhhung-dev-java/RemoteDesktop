package com.remote.client;

import com.remote.client.components.ViewerFrame;
import javax.swing.*;
import java.net.Socket;

public class ClientMain {
    public static void start(String ip, String pass) {
        new Thread(() -> {
            try {
                // Gọi module NetworkManager
                Socket socket = NetworkManager.connect(ip, pass);
                
                // Mở giao diện
                SwingUtilities.invokeLater(() -> new ViewerFrame(socket, ip));
                
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Lỗi kết nối: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }).start();
    }
}