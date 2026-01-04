package com.remote.server;

import com.remote.common.Protocol;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;

public class InputReceiver extends Thread {
    private Socket socket;
    private DataInputStream dis;
    private Robot robot;

    // Biến để ghi file
    private FileOutputStream fileOut = null;
    private String saveDirectory;

    public InputReceiver(Socket socket, DataInputStream dis, Robot robot) {
        this.socket = socket;
        this.dis = dis;
        this.robot = robot;
        
        // Mặc định lưu file ra Desktop của máy Server
        saveDirectory = System.getProperty("user.home") + File.separator + "Desktop" + File.separator;
    }

    @Override
    public void run() {
        try {
            while (!socket.isClosed()) {
                // QUAN TRỌNG: Đổi readInt() thành readByte() để khớp với Client
                byte type = dis.readByte(); 
                
                switch (type) {
                    // --- NHÓM LỆNH CHUỘT / PHÍM 
                    case Protocol.CMD_MOUSE_MOVE:
                        int x = dis.readInt();
                        int y = dis.readInt();
                        robot.mouseMove(x, y);
                        break;
                        
                    case Protocol.CMD_MOUSE_PRESS:
                        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                        break;
                        
                    case Protocol.CMD_MOUSE_RELEASE:
                        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                        break;
                        
                    case Protocol.CMD_KEY_PRESS:
                        robot.keyPress(dis.readInt());
                        break;
                        
                    case Protocol.CMD_KEY_RELEASE:
                        robot.keyRelease(dis.readInt());
                        break;
                        
                    case Protocol.CMD_MOUSE_WHEEL:
                        robot.mouseWheel(dis.readInt());
                        break;

                    // --- NHÓM LỆNH NHẬN FILE 
                    
                    case Protocol.CMD_FILE_START: // 50
                        String fileName = dis.readUTF();
                        long fileSize = dis.readLong();
                        
                        System.out.println("Đang nhận file: " + fileName + " (" + fileSize + " bytes)");
                        
                        // Tạo file trên Desktop
                        File f = new File(saveDirectory + fileName);
                        fileOut = new FileOutputStream(f);
                        break;

                    case Protocol.CMD_FILE_DATA: // 51
                        int length = dis.readInt(); // Đọc độ dài gói tin
                        byte[] buffer = new byte[length];
                        dis.readFully(buffer, 0, length); // Đọc dữ liệu vào buffer
                        
                        if (fileOut != null) {
                            fileOut.write(buffer); // Ghi xuống ổ cứng
                        }
                        break;

                    case Protocol.CMD_FILE_END: // 52
                        if (fileOut != null) {
                            fileOut.close();
                            fileOut = null;
                            System.out.println("-> Đã lưu file thành công!");
                        }
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("Kết nối bị ngắt hoặc lỗi đọc dữ liệu.");
            // e.printStackTrace(); 
        } finally {
           
            try {
                if (fileOut != null) fileOut.close();
            } catch (IOException e) {}
        }
    }
}