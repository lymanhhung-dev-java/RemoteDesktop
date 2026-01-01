package com.remote.client.handlers;

import com.remote.client.components.ScreenPanel;
import com.remote.common.Protocol;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.net.Socket;

public class VideoReceiver extends Thread {
    private Socket socket;
    private DataInputStream dis;
    private ScreenPanel screenPanel;

    public VideoReceiver(Socket socket, DataInputStream dis, ScreenPanel screenPanel) {
        this.socket = socket;
        this.dis = dis;
        this.screenPanel = screenPanel;
    }

    @Override
    public void run() {
        FFmpegFrameGrabber grabber = null;
        try {
            // Đọc header kích thước trước (để setup panel)
            int cmd = dis.readInt();
            if (cmd == Protocol.CMD_SCREEN_SIZE) {
                int w = dis.readInt();
                int h = dis.readInt();
                screenPanel.setServerSize(w, h);
                screenPanel.updateBufferSize(w, h);
            }

            // Cấu hình Grabber: Đọc từ Input Stream của Socket
            grabber = new FFmpegFrameGrabber(socket.getInputStream());
            grabber.setFormat("h264");
            // Quan trọng: Tăng buffer size để tránh lỗi vỡ hình khi mạng lag
            grabber.setOption("probesize", "1024000"); 
            grabber.setVideoOption("preset", "ultrafast");
            
            grabber.start();

            Java2DFrameConverter converter = new Java2DFrameConverter();

            while (!socket.isClosed()) {
                // 1. Lấy khung hình mới (Decode H.264 -> Raw Image)
                Frame frame = grabber.grabImage();
                
                if (frame != null) {
                    // 2. Convert thành BufferedImage
                    BufferedImage img = converter.convert(frame);
                    
                    if (img != null) {
                        // 3. Vẽ lên màn hình (Cần thêm hàm drawFullImage vào ScreenPanel)
                        SwingUtilities.invokeLater(() -> {
                            screenPanel.drawFullImage(img);
                            screenPanel.repaint();
                        });
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Kết nối Video bị ngắt!"));
        } finally {
            try { if (grabber != null) grabber.stop(); } catch (Exception e) {}
        }
    }
}