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

        try {
            // DÙNG LẠI SCREEN RECEIVER CŨ (Nhớ truyền dis vào)
            new ScreenReceiver(socket, dis, screenPanel).start();

            InputSender inputSender = new InputSender(dos, screenPanel);
            
            screenPanel.addMouseListener(inputSender);
            screenPanel.addMouseMotionListener(inputSender);
            screenPanel.addMouseWheelListener(inputSender);
            this.addKeyListener(inputSender);

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