package com.remote;

import com.remote.client.ClientApp;
import com.remote.common.Protocol;
import com.remote.server.ServerApp;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.net.*;

public class MainApp {

    public static void main(String[] args) {
        ServerApp.startBackgroundServer();

        SwingUtilities.invokeLater(MainApp::createDashboard);
    }

    private static void createDashboard() {
        try { for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) 
              if ("Nimbus".equals(info.getName())) UIManager.setLookAndFeel(info.getClassName()); } catch (Exception e) {}

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
        txtIp.setEditable(false); txtIp.setFont(new Font("Arial", Font.BOLD, 16)); txtIp.setHorizontalAlignment(JTextField.CENTER);
        
        JLabel lblPass = new JLabel("Mật khẩu truy cập:");
        // Lấy mật khẩu từ ServerApp
        JTextField txtPass = new JTextField(ServerApp.myPassword); 
        txtPass.setEditable(false); txtPass.setFont(new Font("Arial", Font.BOLD, 24)); txtPass.setForeground(Color.RED); txtPass.setHorizontalAlignment(JTextField.CENTER);

        addComponent(myInfoPanel, lblIp, txtIp, lblPass, txtPass);
        
        // --- PHẦN BÊN PHẢI: ĐIỀU KHIỂN MÁY KHÁC ---
        JPanel remotePanel = new JPanel(new BorderLayout(5, 5));
        remotePanel.setBorder(BorderFactory.createTitledBorder("Điều khiển máy khác"));

        // Bảng danh sách
        String[] cols = {"Tên máy", "Địa chỉ IP"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setRowHeight(25);
        
        JPanel btnPanel = new JPanel(new FlowLayout());
        JButton btnScan = new JButton("Làm mới (Scan)");
        JButton btnConnect = new JButton("Kết nối");
        btnConnect.setBackground(new Color(0, 120, 215)); btnConnect.setForeground(Color.WHITE);
        
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
                if (ip != null && !ip.isEmpty()) connectToDevice(ip);
            }
        });

        // Quét lần đầu
        scanDevices(model);
        
        frame.setVisible(true);
    }
    
    private static void connectToDevice(String ip) {
        String pass = JOptionPane.showInputDialog("Đang kết nối tới " + ip + "\nNhập mật khẩu đối tác:");
        if (pass == null || pass.isEmpty()) return;

        new Thread(() -> {
            new ClientApp().startConnection(ip, pass); 
        }).start();
    }

    // Logic quét UDP
    private static void scanDevices(DefaultTableModel model) {
        model.setRowCount(0);
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true); socket.setSoTimeout(1500);
                byte[] data = Protocol.DISCOVERY_REQ.getBytes();
                socket.send(new DatagramPacket(data, data.length, InetAddress.getByName("255.255.255.255"), Protocol.UDP_PORT));

                while(true) {
                    byte[] buf = new byte[1024];
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    socket.receive(p);
                    String msg = new String(p.getData(), 0, p.getLength()).trim();
                    if (msg.startsWith(Protocol.DISCOVERY_RES)) {
                        String name = msg.split(";")[1];
                        String ip = p.getAddress().getHostAddress();
                        // Tránh thêm chính mình
                        if (!ip.equals(getLocalIp())) {
                             SwingUtilities.invokeLater(() -> model.addRow(new Object[]{name, ip}));
                        }
                    }
                }
            } catch(Exception e) {}
        }).start();
    }

    private static String getLocalIp() {
        try { return InetAddress.getLocalHost().getHostAddress(); } catch(Exception e) { return "Unknown"; }
    }
    
    private static void addComponent(JPanel p, JComponent... comps) {
        for(JComponent c : comps) {
            c.setAlignmentX(Component.CENTER_ALIGNMENT);
            p.add(Box.createVerticalGlue());
            p.add(c);
        }
        p.add(Box.createVerticalGlue());
    }
}