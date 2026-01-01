package com.remote.client;

import com.remote.client.components.ViewerFrame;
import com.remote.common.Config;
import com.remote.common.Protocol;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class ClientMain {
    public static void start(String ip, String pass) {
        new Thread(() -> {
            try {
                // 1. Kết nối Socket
                Socket socket = new Socket(ip, Config.TCP_PORT);
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                DataInputStream dis = new DataInputStream(socket.getInputStream());

                // 2. Gửi mật khẩu xác thực
                dos.writeInt(Protocol.CMD_AUTH_REQUEST);
                dos.writeUTF(pass);
                dos.flush();

                // 3. Nhận phản hồi
                int response = dis.readInt();
                if (response == Protocol.CMD_AUTH_OK) {
                    System.out.println("Đăng nhập thành công!");
                    
                    SwingUtilities.invokeLater(() -> new ViewerFrame(socket, dis, dos, ip));
                    
                } else {
                    JOptionPane.showMessageDialog(null, "Sai mật khẩu!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    socket.close();
                }

            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Lỗi kết nối: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }).start();
    }
}