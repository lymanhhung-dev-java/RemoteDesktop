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
import java.awt.event.KeyAdapter; // <--- Import thêm cái này
import java.awt.event.KeyEvent;

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

        setupKeyboardEvents();

        this.setFocusable(true);
        this.requestFocusInWindow();

        setVisible(true);
        
       // connectToServer("localhost");
        String ipMayAo = JOptionPane.showInputDialog("Nhập IP của máy ảo:", "192.168.1.4");
        connectToServer(ipMayAo);
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

    private void setupKeyboardEvents() {
        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                // Gửi lệnh nhấn phím
                sendCommand(Protocol.CMD_KEY_PRESS, e.getKeyCode(), 0);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                // Gửi lệnh thả phím
                sendCommand(Protocol.CMD_KEY_RELEASE, e.getKeyCode(), 0);
            }
        });
    }

    private void sendCommand(int type, int p1, int p2) {
        try {
            if (dos != null) {
                dos.writeInt(type);
                
                if (type == Protocol.CMD_MOUSE_MOVE) {
                    dos.writeInt(p1); // x
                    dos.writeInt(p2); // y
                } 
                else if (type == Protocol.CMD_KEY_PRESS || type == Protocol.CMD_KEY_RELEASE) {
                    dos.writeInt(p1); // Gửi KeyCode (Mã phím)
                    // Không cần gửi tham số thứ 2
                }
                
                dos.flush();
            }
        } catch (Exception ex) { }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientApp::new);
    }
}
