package com.remote.client.handlers;

import com.remote.client.components.ScreenPanel;
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
            grabber = new FFmpegFrameGrabber(socket.getInputStream());
            grabber.setFormat("h264");
            grabber.setOption("probesize", "5000000"); // Tăng lên 5MB để chắc chắn dò được header
            grabber.setOption("analyzeduration", "5000000"); // Thời gian dò luồng
            grabber.setVideoOption("preset", "ultrafast");
            
            grabber.start(); // Bắt đầu nhận luồng ngay lập tức

            Java2DFrameConverter converter = new Java2DFrameConverter();

           while (!socket.isClosed()) {
                Frame frame = grabber.grabImage();
                if (frame != null) {
                    // --- ĐOẠN CODE SỬA ĐỔI ---
                    
                    // 1. Chỉ xử lý nếu frame có kích thước hợp lệ
                    if (frame.imageWidth > 0 && frame.imageHeight > 0) {
                        
                        // Cập nhật kích thước Panel nếu thay đổi
                        if (screenPanel.serverWidth != frame.imageWidth || screenPanel.serverHeight != frame.imageHeight) {
                            screenPanel.setServerSize(frame.imageWidth, frame.imageHeight);
                            screenPanel.updateBufferSize(frame.imageWidth, frame.imageHeight);
                        }

                        // 2. Convert và Vẽ
                        BufferedImage img = converter.convert(frame);
                        if (img != null) {
                            SwingUtilities.invokeLater(() -> {
                                screenPanel.drawFullImage(img);
                                screenPanel.repaint();
                            });
                        }
                    } 
                    // Nếu size = 0 thì bỏ qua, không làm gì cả (tránh lỗi Picture size 0x0 invalid)
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (grabber != null) grabber.stop(); } catch (Exception e) {}
        }
    }
}