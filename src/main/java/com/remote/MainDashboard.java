package com.remote;

import com.remote.client.ClientMain;
import com.remote.common.Config;
import com.remote.common.Protocol;
import com.remote.server.ServerMain;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.*;

public class MainDashboard {
    public static void main(String[] args) {
        // 1. Tự động chạy Server ngầm
        ServerMain.startBackgroundServer();

        // 2. Hiện giao diện
        SwingUtilities.invokeLater(MainDashboard::createUI);
    }

    private static void createUI() {
        try { UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel"); } catch (Exception e) {}

        JFrame frame = new JFrame("Remote Desktop Pro (Modular)");
        frame.setSize(800, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new GridLayout(1, 2, 10, 10));

        // --- Panel Trái: Thông tin máy mình ---
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Máy của bạn"));
        
        JTextField txtIp = new JTextField(getLocalIp()); txtIp.setEditable(false);
        JTextField txtPass = new JTextField(ServerMain.myPassword); txtPass.setEditable(false);
        txtPass.setFont(new Font("Arial", Font.BOLD, 20)); txtPass.setForeground(Color.RED);
        
        infoPanel.add(new JLabel("IP:")); infoPanel.add(txtIp);
        infoPanel.add(new JLabel("Password:")); infoPanel.add(txtPass);

        // --- Panel Phải: Danh sách máy online ---
        JPanel remotePanel = new JPanel(new BorderLayout());
        remotePanel.setBorder(BorderFactory.createTitledBorder("Kết nối máy khác"));
        
        DefaultTableModel model = new DefaultTableModel(new String[]{"Hostname", "IP"}, 0);
        JTable table = new JTable(model);
        
        JPanel btnPanel = new JPanel();
        JButton btnScan = new JButton("Scan LAN");
        JButton btnConnect = new JButton("Connect");
        btnPanel.add(btnScan); btnPanel.add(btnConnect);
        
        remotePanel.add(new JScrollPane(table), BorderLayout.CENTER);
        remotePanel.add(btnPanel, BorderLayout.SOUTH);

        // Logic nút bấm
        btnScan.addActionListener(e -> scanLan(model));
        btnConnect.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                String ip = (String) model.getValueAt(row, 1);
                String pass = JOptionPane.showInputDialog("Nhập mật khẩu máy " + ip + ":");
                if (pass != null) ClientMain.start(ip, pass);
            }
        });

        frame.add(infoPanel);
        frame.add(remotePanel);
        frame.setVisible(true);
    }

    private static void scanLan(DefaultTableModel model) {
        model.setRowCount(0);
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);
                socket.setSoTimeout(1000);
                byte[] data = Protocol.DISCOVERY_REQ.getBytes();
                socket.send(new DatagramPacket(data, data.length, InetAddress.getByName("255.255.255.255"), Config.UDP_PORT));

                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() - start < 1500) {
                    try {
                        byte[] buf = new byte[1024];
                        DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                        socket.receive(pkt);
                        String msg = new String(pkt.getData(), 0, pkt.getLength()).trim();
                        if (msg.startsWith(Protocol.DISCOVERY_RES)) {
                            String ip = pkt.getAddress().getHostAddress();
                            if (!ip.equals(getLocalIp())) { // Loại bỏ chính mình
                                String name = msg.split(";")[1];
                                SwingUtilities.invokeLater(() -> model.addRow(new Object[]{name, ip}));
                            }
                        }
                    } catch (SocketTimeoutException ex) { break; }
                }
            } catch (Exception e) {}
        }).start();
    }

    private static String getLocalIp() {
        try { return InetAddress.getLocalHost().getHostAddress(); } catch (Exception e) { return "Unknown"; }
    }
}