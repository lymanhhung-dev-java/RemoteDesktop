package com.remote.client;

import com.remote.common.ImageUtils;
import com.remote.common.Protocol;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class ClientApp extends JFrame {
    private JLabel screenLabel;
    private DataOutputStream dos;

    public ClientApp() {
        setTitle("Remote Desktop Viewer");
        setSize(1024, 768);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        screenLabel = new JLabel("Dang ket noi...", SwingConstants.CENTER);
        add(new JScrollPane(screenLabel), BorderLayout.CENTER);

        setupMouseEvents();

        setVisible(true);
        
        connectToServer("localhost");
    }

    private void connectToServer(String ip) {
        new Thread(() -> {
            try {
                Socket socket = new Socket(ip, Protocol.PORT);
                dos = new DataOutputStream(socket.getOutputStream());
                DataInputStream dis = new DataInputStream(socket.getInputStream());

                System.out.println("Da ket noi toi Server!");

                while (true) {
                    int len = dis.readInt();
                    byte[] data = new byte[len];
                    
                    dis.readFully(data);

                    BufferedImage img = ImageUtils.decompress(data);

                    SwingUtilities.invokeLater(() -> {
                        screenLabel.setIcon(new ImageIcon(img));
                        screenLabel.setText("");
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Mat ket noi Server!");
            }
        }).start();
    }

    private void setupMouseEvents() {
        screenLabel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                sendCommand(Protocol.CMD_MOUSE_MOVE, e.getX(), e.getY());
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                sendCommand(Protocol.CMD_MOUSE_MOVE, e.getX(), e.getY());
            }
        });

        screenLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                sendCommand(Protocol.CMD_MOUSE_PRESS, 0, 0);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                sendCommand(Protocol.CMD_MOUSE_RELEASE, 0, 0);
            }
        });
    }

    private void sendCommand(int type, int x, int y) {
        try {
            if (dos != null) {
                dos.writeInt(type);
                if (type == Protocol.CMD_MOUSE_MOVE) {
                    dos.writeInt(x);
                    dos.writeInt(y);
                }
                dos.flush();
            }
        } catch (Exception ex) {
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientApp::new);
    }
}
