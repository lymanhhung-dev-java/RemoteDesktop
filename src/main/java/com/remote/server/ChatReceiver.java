package com.remote.server;

import com.remote.common.Protocol;

import java.io.DataInputStream;
import java.net.Socket;

public class ChatReceiver extends Thread {
    private Socket socket;
    private DataInputStream dis;

    public ChatReceiver(Socket socket, DataInputStream dis) {
        this.socket = socket;
        this.dis = dis;
    }

    @Override
    public void run() {
        try {
            while (true) {
                int cmd = dis.readInt();
                if (cmd == Protocol.CMD_CHAT_MSG) {
                    String message = dis.readUTF();
                    showNotification(message);
                }
            }
        } catch (Exception e) {
            System.out.println("ChatReceiver stopped: " + e.getMessage());
        }
    }

    private void showNotification(String message) {
        try {
            // Lấy GIF icon từ resources nếu có, nếu không dùng default
            javax.swing.JOptionPane.showMessageDialog(
                null,
                message,
                "Chat from Client",
                javax.swing.JOptionPane.INFORMATION_MESSAGE
            );
        } catch (Exception e) {
            System.out.println("Chat: " + message);
        }
    }
}
