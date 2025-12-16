package com.remote;

import com.remote.client.ClientApp;
import com.remote.common.Protocol;
import com.remote.server.ServerApp;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.util.Enumeration;

public class MainApp {

    public static void main(String[] args) {
        ServerApp.startBackgroundServer();

        SwingUtilities.invokeLater(MainApp::createDashboard);
    }

    private static void createDashboard() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels())
                if ("Nimbus".equals(info.getName()))
                    UIManager.setLookAndFeel(info.getClassName());
        } catch (Exception e) {
        }

        JFrame frame = new JFrame("Remote Desktop Pro");
        frame.setSize(800, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new GridLayout(1, 2, 10, 10)); // Chia đôi màn hình
        JPanel myInfoPanel = new JPanel();
        myInfoPanel.setLayout(new BoxLayout(myInfoPanel, BoxLayout.Y_AXIS));
        myInfoPanel.setBorder(BorderFactory.createTitledBorder("Cho phép điều khiển"));
        myInfoPanel.setBackground(new Color(245, 245, 250));

        JLabel lblIp = new JLabel("Địa chỉ IP của bạn:");
        JTextField txtIp = new JTextField(getLocalIp());
        txtIp.setEditable(false);
        txtIp.setFont(new Font("Arial", Font.BOLD, 16));
        txtIp.setHorizontalAlignment(JTextField.CENTER);

        JLabel lblPass = new JLabel("Mật khẩu truy cập:");
        // Lấy mật khẩu từ ServerApp
        JTextField txtPass = new JTextField(ServerApp.myPassword);
        txtPass.setEditable(false);
        txtPass.setFont(new Font("Arial", Font.BOLD, 24));
        txtPass.setForeground(Color.RED);
        txtPass.setHorizontalAlignment(JTextField.CENTER);

        addComponent(myInfoPanel, lblIp, txtIp, lblPass, txtPass);

        // --- PHẦN BÊN PHẢI: ĐIỀU KHIỂN MÁY KHÁC ---
        JPanel remotePanel = new JPanel(new BorderLayout(5, 5));
        remotePanel.setBorder(BorderFactory.createTitledBorder("Điều khiển máy khác"));

        // Bảng danh sách
        String[] cols = { "Tên máy", "Địa chỉ IP" };
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setRowHeight(25);

        JPanel btnPanel = new JPanel(new FlowLayout());
        JButton btnScan = new JButton("Làm mới (Scan)");
        JButton btnConnect = new JButton("Kết nối");
        btnConnect.setBackground(new Color(0, 120, 215));
        btnConnect.setForeground(Color.WHITE);

        btnPanel.add(btnScan);
        btnPanel.add(btnConnect);

        remotePanel.add(new JScrollPane(table), BorderLayout.CENTER);
        remotePanel.add(btnPanel, BorderLayout.SOUTH);

        frame.add(myInfoPanel);
        frame.add(remotePanel);

        // Nút Scan: Quét UDP
        btnScan.addActionListener(e -> scanDevices(model));

        // Nút Connect:
        btnConnect.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                String ip = (String) model.getValueAt(row, 1);
                connectToDevice(ip);
            } else {
                // Nếu chưa chọn, cho nhập tay
                String ip = JOptionPane.showInputDialog("Nhập IP máy đối tác:");
                if (ip != null && !ip.isEmpty())
                    connectToDevice(ip);
            }
        });

        // Quét lần đầu
        scanDevices(model);

        frame.setVisible(true);
    }

    private static void connectToDevice(String ip) {
        String pass = JOptionPane.showInputDialog("Đang kết nối tới " + ip + "\nNhập mật khẩu đối tác:");
        if (pass == null || pass.isEmpty())
            return;

        new Thread(() -> {
            new ClientApp().startConnection(ip, pass);
        }).start();
    }

    // quét UDP
    private static void scanDevices(DefaultTableModel model) {
        model.setRowCount(0);
        new Thread(() -> {
            try {
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = interfaces.nextElement();
                    if (networkInterface.isLoopback() || !networkInterface.isUp())
                        continue;

                    for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                        InetAddress broadcast = interfaceAddress.getBroadcast();
                        if (broadcast != null) {
                            try (DatagramSocket socket = new DatagramSocket()) {
                                socket.setBroadcast(true);
                                byte[] sendData = Protocol.DISCOVERY_REQ.getBytes();

                                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcast,
                                        Protocol.UDP_PORT);
                                socket.send(sendPacket);
                                System.out.println("Đã gửi tín hiệu tìm kiếm qua: " + networkInterface.getDisplayName()
                                        + " -> " + broadcast);
                            } catch (Exception e) {
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static String getLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private static void addComponent(JPanel p, JComponent... comps) {
        for (JComponent c : comps) {
            c.setAlignmentX(Component.CENTER_ALIGNMENT);
            p.add(Box.createVerticalGlue());
            p.add(c);
        }
        p.add(Box.createVerticalGlue());
    }
}