package com.remote.client.components;

import com.remote.common.Protocol;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.DataOutputStream;
import java.io.IOException;

public class ChatPanel extends JPanel {
    private DataOutputStream dos;

    public ChatPanel(DataOutputStream dos) {
        this.dos = dos;
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        //  Ô nhập tin nhắn
        JTextField txtInput = new JTextField();
        txtInput.setToolTipText("Nhập tin nhắn gửi đến máy kia...");
        
        //  Nút Gửi
        JButton btnSend = new JButton("Gửi tin nhắn");

        // 3. Xử lý sự kiện gửi
        ActionListener sendAction = e -> {
            String msg = txtInput.getText().trim();
            if (!msg.isEmpty()) {
                try {
                    synchronized (dos) {
                        dos.writeByte(Protocol.CMD_CHAT_MSG); // Gửi mã lệnh
                        dos.writeUTF(msg);                   // Gửi nội dung
                        dos.flush();
                    }
                    txtInput.setText(""); // Xóa ô nhập sau khi gửi
                    Window window = SwingUtilities.getWindowAncestor(this);
                    if (window != null) {
                        window.requestFocus();
                    }
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Lỗi gửi tin: " + ex.getMessage());
                }
            }
        };

        // Gửi khi bấm nút hoặc nhấn Enter
        btnSend.addActionListener(sendAction);
        txtInput.addActionListener(sendAction);

        add(new JLabel("Chat: "), BorderLayout.WEST);
        add(txtInput, BorderLayout.CENTER);
        add(btnSend, BorderLayout.EAST);
    }
}