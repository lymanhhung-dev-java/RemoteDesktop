package com.remote.client.components;

import com.remote.client.handlers.InputSender;
import com.remote.client.handlers.ScreenReceiver;

import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class ViewerFrame extends JFrame {
    
    public ViewerFrame(Socket socket, DataInputStream dis, DataOutputStream dos, String ip) {
        setTitle("Remote Desktop (Standard) - Connected to: " + ip);
        setSize(1024, 768);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        ScreenPanel screenPanel = new ScreenPanel();
        add(screenPanel, BorderLayout.CENTER);
        
        // Thêm panel chat
        JPanel chatPanel = new JPanel(new BorderLayout());
        JTextField chatInput = new JTextField();
        chatInput.setPreferredSize(new Dimension(0, 30));
        chatPanel.add(chatInput, BorderLayout.CENTER);
        add(chatPanel, BorderLayout.SOUTH);

        try {
            // DÙNG LẠI SCREEN RECEIVER CŨ (Nhớ truyền dis vào)
            new ScreenReceiver(socket, dis, screenPanel).start();

            InputSender inputSender = new InputSender(dos, screenPanel);
            
            screenPanel.addMouseListener(inputSender);
            screenPanel.addMouseMotionListener(inputSender);
            screenPanel.addMouseWheelListener(inputSender);
            this.addKeyListener(inputSender);
            
            // Xử lý chat
            chatInput.addActionListener(e -> {
                try {
                    String message = chatInput.getText();
                    if (!message.isEmpty()) {
                        dos.writeInt(com.remote.common.Protocol.CMD_CHAT_MSG);
                        dos.writeUTF(message);
                        dos.flush();
                        chatInput.setText("");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

        setVisible(true);
        
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                try { socket.close(); } catch (Exception ex) {}
            }
        });
    }
}