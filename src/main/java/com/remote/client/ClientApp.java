package com.remote.client;

import com.remote.common.ImageUtils;
import com.remote.common.Protocol;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class ClientApp extends JFrame {
    private ScreenPanel screenPanel;
    private DataOutputStream dos;
    
    private float serverWidth = 0;
    private float serverHeight = 0;

    public ClientApp() {
        setTitle("Remote Desktop Viewer (Auto Scale)");
        setSize(1024, 768);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        screenPanel = new ScreenPanel();
        add(screenPanel, BorderLayout.CENTER); 

        setupMouseEvents();
        setupKeyboardEvents(); 

        this.setFocusable(true);
        this.requestFocusInWindow();
        
        setVisible(true);

        String ip = JOptionPane.showInputDialog("Nhập IP Server:", "192.168.1.x");
        if (ip != null && !ip.isEmpty()) connectToServer(ip);
        else System.exit(0);
    }

    private void connectToServer(String ip) {
        new Thread(() -> {
            try {
                Socket socket = new Socket(ip, Protocol.PORT);
                dos = new DataOutputStream(socket.getOutputStream());
                DataInputStream dis = new DataInputStream(socket.getInputStream());

                while (true) {
                    int len = dis.readInt();
                    byte[] data = new byte[len];
                    dis.readFully(data);
                    
                    BufferedImage img = ImageUtils.decompress(data);
                    
                    serverWidth = img.getWidth();
                    serverHeight = img.getHeight();

                    // Vẽ lại giao diện
                    SwingUtilities.invokeLater(() -> {
                        screenPanel.updateImage(img);
                        screenPanel.repaint();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void setupMouseEvents() {
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                sendMouse(Protocol.CMD_MOUSE_MOVE, e);
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                sendMouse(Protocol.CMD_MOUSE_MOVE, e);
            }
            @Override
            public void mousePressed(MouseEvent e) {
                sendMouse(Protocol.CMD_MOUSE_PRESS, e);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                sendMouse(Protocol.CMD_MOUSE_RELEASE, e);
            }
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                sendCommand(Protocol.CMD_MOUSE_WHEEL, e.getWheelRotation(), 0);
            }
        };

        screenPanel.addMouseListener(mouseHandler);
        screenPanel.addMouseMotionListener(mouseHandler);
        screenPanel.addMouseWheelListener(mouseHandler);
    }
    private void sendMouse(int type, MouseEvent e) {

        if (serverWidth == 0 || serverHeight == 0) return;
        float clientW = screenPanel.getWidth();
        float clientH = screenPanel.getHeight();

        float scaleX = serverWidth / clientW;
        float scaleY = serverHeight / clientH;

        int realX = (int) (e.getX() * scaleX);
        int realY = (int) (e.getY() * scaleY);

        sendCommand(type, realX, realY);
    }

    private void sendCommand(int type, int p1, int p2) {
        try {
            if (dos != null) {
                dos.writeInt(type);
                if (type == Protocol.CMD_MOUSE_MOVE) {
                    dos.writeInt(p1);
                    dos.writeInt(p2);
                } else if (type == Protocol.CMD_MOUSE_WHEEL || type == Protocol.CMD_KEY_PRESS || type == Protocol.CMD_KEY_RELEASE) {
                    dos.writeInt(p1); 
                }
                dos.flush();
            }
        } catch (Exception ex) {}
    }

    private void setupKeyboardEvents() {
        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                sendCommand(Protocol.CMD_KEY_PRESS, e.getKeyCode(), 0);
            }
            @Override
            public void keyReleased(KeyEvent e) {
                sendCommand(Protocol.CMD_KEY_RELEASE, e.getKeyCode(), 0);
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientApp::new);
    }


    class ScreenPanel extends JPanel {
        private BufferedImage image;

        public void updateImage(BufferedImage img) {
            this.image = img;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image != null) {

                g.drawImage(image, 0, 0, this.getWidth(), this.getHeight(), null);
            }
        }
    }
}