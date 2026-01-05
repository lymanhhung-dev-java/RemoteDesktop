package com.remote.client;

import com.remote.client.components.ViewerFrame;
import com.remote.client.handlers.ClipboardWatcher;
import com.remote.common.Config;
import com.remote.common.Protocol;
import com.remote.server.ServerMain;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class ClientMain {

    public static void startOnline(String targetId, String pass) {
        new Thread(() -> {
            try {
                // Kết nối đến Relay Server
                Socket socket = new Socket(ServerMain.RELAY_IP, ServerMain.RELAY_PORT);
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                DataInputStream dis = new DataInputStream(socket.getInputStream());

                // Gửi yêu cầu kết nối ID
                dos.writeInt(Protocol.CMD_CONNECT_TO_ID);
                dos.writeUTF(targetId);
                dos.flush();

                // Chờ Relay trả lời
                int response = dis.readInt();
                if (response == Protocol.CMD_ID_NOT_FOUND) {
                    JOptionPane.showMessageDialog(null, "ID không tồn tại hoặc máy đó chưa online!");
                    socket.close();
                    return;
                }

                // Nếu kết nối thành công -> Bắt đầu quy trình xác thực mật khẩu như cũ
                // Gửi mật khẩu xác thực (lúc này luồng đã được nối thông với Server kia)
                dos.writeInt(Protocol.CMD_AUTH_REQUEST);
                dos.writeUTF(pass);
                dos.flush();

                int authResponse = dis.readInt();
                if (authResponse == Protocol.CMD_AUTH_OK) {
                    System.out.println("Đăng nhập thành công!");
                    SwingUtilities.invokeLater(() -> new ViewerFrame(socket, dis, dos, "ID: " + targetId));
                } else {
                    JOptionPane.showMessageDialog(null, "Sai mật khẩu!");
                    socket.close();
                }

            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Lỗi kết nối Online: " + e.getMessage());
            }
        }).start();
    }
    public static void start(String ip, String pass) {
        new Thread(() -> {
            try {
                // 1. Kết nối Socket
                Socket socket = new Socket(ip, Config.TCP_PORT);
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                DataInputStream dis = new DataInputStream(socket.getInputStream());
                new Thread(new ClipboardWatcher(socket)).start();

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