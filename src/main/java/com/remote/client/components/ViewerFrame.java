package com.remote.client.components;

import com.remote.client.handlers.InputSender;
import com.remote.client.handlers.ScreenReceiver;
import com.remote.common.Protocol; // Import Protocol

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
    
    public ViewerFrame(Socket socket, DataInputStream dis, DataOutputStream dos, String ip) {
        this.dos = dos; // Lưu biến dos vào class ngay đầu tiên

        setTitle("Remote Desktop (Standard) - Connected to: " + ip);
        setSize(1024, 768);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        ScreenPanel screenPanel = new ScreenPanel();
        add(screenPanel, BorderLayout.CENTER);

        try {
            // Khởi động luồng nhận màn hình
            new ScreenReceiver(socket, dis, screenPanel).start();

            // Khởi động bộ gửi chuột/phím
            InputSender inputSender = new InputSender(dos, screenPanel);
            
            screenPanel.addMouseListener(inputSender);
            screenPanel.addMouseMotionListener(inputSender);
            screenPanel.addMouseWheelListener(inputSender);
            this.addKeyListener(inputSender);

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Sự kiện khi đóng cửa sổ thì ngắt kết nối
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
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
            // 1. Gửi lệnh BẮT ĐẦU: ID lệnh + Tên file + Kích thước
            dos.writeByte(Protocol.CMD_FILE_START);
            dos.writeUTF(file.getName());
            dos.writeLong(file.length());
            dos.flush();

        
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[4096]; // Mỗi lần gửi 4KB
            int bytesRead;
    
            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.writeByte(Protocol.CMD_FILE_DATA); // Báo đây là gói dữ liệu
                dos.writeInt(bytesRead);               // Báo độ dài gói này
                dos.write(buffer, 0, bytesRead);       // Gửi dữ liệu thực
                dos.flush();
            }
            fis.close();

         
            dos.writeByte(Protocol.CMD_FILE_END);
            dos.flush();
            
            System.out.println("-> Đã gửi xong file: " + file.getName());

        } catch (IOException e) {
            System.err.println("Lỗi khi gửi file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}