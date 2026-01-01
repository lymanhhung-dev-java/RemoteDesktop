package com.remote;

import com.remote.client.ClientMain;
import com.remote.common.Config;
import com.remote.common.Protocol;
import com.remote.server.ServerMain;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.*;
import java.util.Enumeration;

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
            try {
                // Duyệt qua TẤT CẢ các card mạng đang có trên máy tính
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = interfaces.nextElement();

                    // Bỏ qua card mạng ảo loopback hoặc card đang tắt
                    if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                        continue;
                    }

                    // Lấy địa chỉ Broadcast của từng card và gửi gói tin
                    for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                        InetAddress broadcast = interfaceAddress.getBroadcast();
                        
                        // Nếu card mạng này có địa chỉ broadcast (tức là nó nối mạng LAN)
                        if (broadcast != null) {
                            try {
                                DatagramSocket socket = new DatagramSocket();
                                socket.setBroadcast(true);
                                socket.setSoTimeout(1000); // Chờ 1 giây

                                byte[] data = Protocol.DISCOVERY_REQ.getBytes();
                                DatagramPacket sendPacket = new DatagramPacket(data, data.length, broadcast, Config.UDP_PORT);
                                socket.send(sendPacket);
                                System.out.println("Đã gửi tín hiệu qua card: " + networkInterface.getDisplayName());

                                // Chờ phản hồi trên cổng này
                                long startWait = System.currentTimeMillis();
                                while (System.currentTimeMillis() - startWait < 1000) {
                                    try {
                                        byte[] buf = new byte[1024];
                                        DatagramPacket recvPacket = new DatagramPacket(buf, buf.length);
                                        socket.receive(recvPacket);

                                        String msg = new String(recvPacket.getData(), 0, recvPacket.getLength()).trim();
                                        // DEBUG: In ra xem nhận được gì không
                                        System.out.println("Nhận từ: " + recvPacket.getAddress().getHostAddress());

                                        if (msg.startsWith(Protocol.DISCOVERY_RES)) {
                                            String ip = recvPacket.getAddress().getHostAddress();
                                            
                                            // Lọc bỏ IP của chính mình (quan trọng)
                                            if (!isLocalAddress(ip)) {
                                                String name = msg.split(";")[1];
                                                
                                                // Check trùng lặp trước khi thêm vào bảng
                                                SwingUtilities.invokeLater(() -> {
                                                    boolean exists = false;
                                                    for (int i = 0; i < model.getRowCount(); i++) {
                                                        if (model.getValueAt(i, 1).equals(ip)) {
                                                            exists = true;
                                                            break;
                                                        }
                                                    }
                                                    if (!exists) {
                                                        model.addRow(new Object[]{name, ip});
                                                    }
                                                });
                                            }
                                        }
                                    } catch (SocketTimeoutException e) {
                                        break; // Hết giờ chờ
                                    }
                                }
                                socket.close();
                            } catch (Exception e) {
                                // Bỏ qua lỗi ở các card mạng không gửi được
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
        try { return InetAddress.getLocalHost().getHostAddress(); } catch (Exception e) { return "Unknown"; }
    }

    // Hàm kiểm tra xem một IP có phải là của chính máy mình không
private static boolean isLocalAddress(String ipToCheck) {
    try {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();
            Enumeration<InetAddress> addresses = iface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress addr = addresses.nextElement();
                // Nếu IP nhận được trùng với BẤT KỲ IP nào của máy mình -> Là chính mình
                if (addr.getHostAddress().equals(ipToCheck)) {
                    return true;
                }
            }
        }
    } catch (SocketException e) {
        e.printStackTrace();
    }
    return false;
}
}