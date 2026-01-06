package com.remote.client.components;

import com.remote.client.handlers.ClipboardWatcher;
import com.remote.client.handlers.InputSender;
import com.remote.client.handlers.ScreenReceiver;
import com.remote.common.Protocol; // Import Protocol
import java.awt.event.MouseAdapter; 
import java.awt.event.MouseEvent;
import javax.swing.*;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.net.Socket;
import java.util.List;


public class ViewerFrame extends JFrame {

    // Khai báo biến dos ở đây để toàn bộ class dùng được
    private DataOutputStream dos;
    private ClipboardWatcher clipboardWatcher;
    
    public ViewerFrame(Socket socket, DataInputStream dis, DataOutputStream dos, String ip) {
        this.dos = dos; // Lưu biến dos vào class ngay đầu tiên

        setTitle("Remote Desktop (Standard) - Connected to: " + ip);
        setSize(1024, 768);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        setFocusable(true);

        ScreenPanel screenPanel = new ScreenPanel();
        add(screenPanel, BorderLayout.CENTER);

        ChatPanel chatPanel = new ChatPanel(dos);
        add(chatPanel, BorderLayout.SOUTH);

        try {
            // Khởi động luồng nhận màn hình
            new ScreenReceiver(socket, dis, screenPanel).start();

            // Khởi động bộ gửi chuột/phím
            InputSender inputSender = new InputSender(dos, screenPanel);
            
            screenPanel.addMouseListener(inputSender);
            screenPanel.addMouseMotionListener(inputSender);
            screenPanel.addMouseWheelListener(inputSender);
            this.addKeyListener(inputSender);

            screenPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    ViewerFrame.this.requestFocusInWindow();
                }
            });

            clipboardWatcher = new ClipboardWatcher(dos);
            new Thread(clipboardWatcher).start();

        } catch (Exception e) {
            e.printStackTrace();
        }

       
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (clipboardWatcher != null) {
                    clipboardWatcher.stopRunning();
                }
                try { socket.close(); } catch (Exception ex) {}
            }
        });

       
        setupFileDrop();
        
        setVisible(true);
    }

  
    private void setupFileDrop() {
       
        new DropTarget(this, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable transferable = dtde.getTransferable();
                    
                  
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        List<File> fileList = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                        
                      
                        for (File file : fileList) {
                            if (file.isFile()) {
                                System.out.println("Chuẩn bị gửi file: " + file.getName());
                                sendFile(file); // Gọi hàm gửi
                            }
                        }
                    }
                    dtde.dropComplete(true);
                } catch (Exception e) {
                    e.printStackTrace();
                    dtde.dropComplete(false);
                }
            }
        });
    }

    private void sendFile(File file) {
        try {
            synchronized (dos) { 
                // 1. Gửi lệnh BẮT ĐẦU
                dos.writeByte(Protocol.CMD_FILE_START); // Nhớ dùng writeByte luôn cho chuẩn
                dos.writeUTF(file.getName());
                dos.writeLong(file.length());
                dos.flush();

                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[4096];
                int bytesRead;
        
                while ((bytesRead = fis.read(buffer)) != -1) {
                    dos.writeByte(Protocol.CMD_FILE_DATA);
                    dos.writeInt(bytesRead);
                    dos.write(buffer, 0, bytesRead);
                    dos.flush();
                }
                fis.close();
            
                dos.writeByte(Protocol.CMD_FILE_END);
                dos.flush();
            }

        } catch (IOException e) {
            System.err.println("Lỗi khi gửi file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}