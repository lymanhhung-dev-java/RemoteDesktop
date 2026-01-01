package com.remote.client.components;

import com.remote.client.handlers.InputSender;
import com.remote.client.handlers.VideoReceiver;

import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class ViewerFrame extends JFrame {
    
    // Sửa Constructor: Thêm tham số dis, dos
    public ViewerFrame(Socket socket, DataInputStream dis, DataOutputStream dos, String ip) {
        setTitle("Remote Desktop - Connected to: " + ip);
        setSize(1024, 768);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // 1. Tạo Panel hiển thị
        ScreenPanel screenPanel = new ScreenPanel();
        add(screenPanel, BorderLayout.CENTER);

        try {
            // KHÔNG TẠO MỚI dis/dos Ở ĐÂY NỮA (để tránh mất dữ liệu)
            
            // 2. Kích hoạt module Nhận Video (Truyền dis cũ vào)
            new VideoReceiver(socket, dis, screenPanel).start();

            // 3. Kích hoạt module Gửi Input (Truyền dos cũ vào)
            InputSender inputSender = new InputSender(dos, screenPanel);
            
            // Đăng ký sự kiện chuột phím
            screenPanel.addMouseListener(inputSender);
            screenPanel.addMouseMotionListener(inputSender);
            screenPanel.addMouseWheelListener(inputSender);
            this.addKeyListener(inputSender);

        } catch (Exception e) {
            e.printStackTrace();
        }

        setVisible(true);
        
        // Khi đóng cửa sổ thì đóng luôn socket
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                try { socket.close(); } catch (Exception ex) {}
            }
        });
    }
}