package com.remote.client.components;

import com.remote.client.handlers.InputSender;
import com.remote.client.handlers.VideoReceiver;

import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class ViewerFrame extends JFrame {
    public ViewerFrame(Socket socket, String ip) {
        setTitle("Remote Desktop - Connected to: " + ip);
        setSize(1024, 768);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // 1. Tạo Panel hiển thị
        ScreenPanel screenPanel = new ScreenPanel();
        add(screenPanel, BorderLayout.CENTER);

        try {
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            DataInputStream dis = new DataInputStream(socket.getInputStream());

            // 2. Kích hoạt module Nhận ảnh (ScreenReceiver)
            new VideoReceiver(socket, dis, screenPanel).start();

            // 3. Kích hoạt module Gửi Input (InputSender)
            InputSender inputSender = new InputSender(dos, screenPanel);
            
            // Đăng ký listener vào Panel và Frame
            screenPanel.addMouseListener(inputSender);
            screenPanel.addMouseMotionListener(inputSender);
            screenPanel.addMouseWheelListener(inputSender);
            this.addKeyListener(inputSender);

        } catch (Exception e) {
            e.printStackTrace();
        }

        setVisible(true);
    }
}
