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

public class ClientApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Hỏi IP
            String ip = JOptionPane.showInputDialog("Nhập IP Server:", "192.168.1.X");
            if (ip == null || ip.isEmpty())
                System.exit(0);

            // Hỏi Mật khẩu
            String pass = JOptionPane.showInputDialog("Nhập Mật khẩu truy cập:");
            if (pass == null || pass.isEmpty())
                System.exit(0);

            // Bắt đầu kết nối
            new ClientApp().startConnection(ip, pass);
        });
    }

    public void startConnection(String ip, String password) {
        new Thread(() -> {
            try {

                Socket socket = new Socket(ip, Protocol.PORT);
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                DataInputStream dis = new DataInputStream(socket.getInputStream());

                dos.writeInt(Protocol.CMD_AUTH_REQUEST);
                dos.writeUTF(password);
                dos.flush();

                int authResponse = dis.readInt();

                if (authResponse == Protocol.CMD_AUTH_OK) {

                    System.out.println("Dang nhap thanh cong!");
                    SwingUtilities.invokeLater(() -> showViewerWindow(socket, dos, dis, ip));
                } else {

                    JOptionPane.showMessageDialog(null, "Sai mật khẩu! Kết nối bị từ chối.", "Lỗi Xác Thực",
                            JOptionPane.ERROR_MESSAGE);
                    socket.close();
                }

            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Không thể kết nối tới " + ip + "\nLỗi: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void showViewerWindow(Socket socket, DataOutputStream dos, DataInputStream dis, String ip) {
        JFrame frame = new JFrame("Remote Desktop - Connected to: " + ip);
        frame.setSize(1024, 768);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        ScreenPanel screenPanel = new ScreenPanel();
        frame.add(screenPanel, BorderLayout.CENTER);

        setupMouseEvents(screenPanel, dos);
        setupKeyboardEvents(frame, dos);

        frame.setFocusable(true);
        frame.requestFocusInWindow();
        frame.setVisible(true);

        new Thread(() -> {
            try {
                while (!socket.isClosed()) {
                    int cmd = dis.readInt();

                    if (cmd == Protocol.CMD_SCREEN_SIZE) {
                        int w = dis.readInt();
                        int h = dis.readInt();

                        screenPanel.setServerSize(w, h);
                        screenPanel.updateBufferSize(w, h);
                    } else if (cmd == Protocol.CMD_SEND_TILE) {

                        int x = dis.readInt();
                        int y = dis.readInt();
                        int w = dis.readInt();
                        int h = dis.readInt();
                        int len = dis.readInt();

                        byte[] data = new byte[len];
                        dis.readFully(data);

                        int[] tilePixels = ImageUtils.decompressRaw(data, w, h);

                        SwingUtilities.invokeLater(() -> {
                            screenPanel.drawTileToBackBuffer(tilePixels, x, y, w, h);
                        });
                    } else if (cmd == Protocol.CMD_FRAME_END) {
                        // Khi nhận lệnh này -> Mới lật hình ra cho người xem
                        SwingUtilities.invokeLater(() -> {
                            screenPanel.swapBuffers();
                        });
                    }

                }
            } catch (Exception e) {
                // Server ngắt kết nối hoặc lỗi mạng
                if (frame.isVisible()) {
                    JOptionPane.showMessageDialog(frame, "Mất kết nối với Server!");
                    frame.dispose();
                }
            }
        }).start();
    }

    private void setupMouseEvents(ScreenPanel screenPanel, DataOutputStream dos) {
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                sendMouse(Protocol.CMD_MOUSE_MOVE, e, screenPanel, dos);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                sendMouse(Protocol.CMD_MOUSE_MOVE, e, screenPanel, dos);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                sendMouse(Protocol.CMD_MOUSE_PRESS, e, screenPanel, dos);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                sendMouse(Protocol.CMD_MOUSE_RELEASE, e, screenPanel, dos);
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                sendCommand(Protocol.CMD_MOUSE_WHEEL, e.getWheelRotation(), 0, dos);
            }
        };

        screenPanel.addMouseListener(mouseHandler);
        screenPanel.addMouseMotionListener(mouseHandler);
        screenPanel.addMouseWheelListener(mouseHandler);
    }

    private void sendMouse(int type, MouseEvent e, ScreenPanel screenPanel, DataOutputStream dos) {

        if (screenPanel.serverWidth == 0 || screenPanel.serverHeight == 0)
            return;

        float clientW = screenPanel.getWidth();
        float clientH = screenPanel.getHeight();

        // Tính tỉ lệ co giãn
        float scaleX = screenPanel.serverWidth / clientW;
        float scaleY = screenPanel.serverHeight / clientH;

        int realX = (int) (e.getX() * scaleX);
        int realY = (int) (e.getY() * scaleY);

        sendCommand(type, realX, realY, dos);
    }

    private void sendCommand(int type, int p1, int p2, DataOutputStream dos) {
        try {
            if (dos != null) {
                dos.writeInt(type);
                if (type == Protocol.CMD_MOUSE_MOVE) {
                    dos.writeInt(p1);
                    dos.writeInt(p2);
                } else if (type == Protocol.CMD_MOUSE_WHEEL || type == Protocol.CMD_KEY_PRESS
                        || type == Protocol.CMD_KEY_RELEASE) {
                    dos.writeInt(p1);
                }
                dos.flush();
            }
        } catch (Exception ex) {

        }
    }

    private void setupKeyboardEvents(JFrame frame, DataOutputStream dos) {
        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                sendCommand(Protocol.CMD_KEY_PRESS, e.getKeyCode(), 0, dos);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                sendCommand(Protocol.CMD_KEY_RELEASE, e.getKeyCode(), 0, dos);
            }
        });
    }

    static class ScreenPanel extends JPanel {
        // 2 Bộ đệm:
        // - backBuffer: Vẽ ngầm (nháp)
        // - frontBuffer: Hiển thị ra màn hình (chính thức)
        private BufferedImage backBuffer;
        private BufferedImage frontBuffer;

        public float serverWidth = 0;
        public float serverHeight = 0;

        public void setServerSize(int w, int h) {
            this.serverWidth = w;
            this.serverHeight = h;
        }

        public void updateBufferSize(int w, int h) {
            // Tạo cả 2 buffer
            if (backBuffer == null || backBuffer.getWidth() != w || backBuffer.getHeight() != h) {
                backBuffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                frontBuffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            }
        }

        // Hàm 1: Chỉ vẽ vào bộ đệm ẩn (người dùng chưa thấy)
        public void drawTileToBackBuffer(int[] pixels, int x, int y, int w, int h) {
            if (backBuffer == null)
                return;
            backBuffer.setRGB(x, y, w, h, pixels, 0, w);
            // KHÔNG gọi repaint() ở đây
        }

        // Hàm 2: Copy từ ẩn sang hiện (Lật hình)
        public void swapBuffers() {
            if (backBuffer == null || frontBuffer == null)
                return;

            // Copy cực nhanh từ Back sang Front
            Graphics2D g2d = frontBuffer.createGraphics();
            g2d.drawImage(backBuffer, 0, 0, null);
            g2d.dispose();

            // Bây giờ mới vẽ ra màn hình -> Đảm bảo hình ảnh liền mạch 100%
            this.repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (frontBuffer != null) {
                // Luôn vẽ frontBuffer (ảnh đã hoàn thiện)
                g.drawImage(frontBuffer, 0, 0, this.getWidth(), this.getHeight(), null);
            }
        }
    }
}