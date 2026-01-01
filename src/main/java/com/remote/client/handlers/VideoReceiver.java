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
                    // --- BƯỚC 3: CẬP NHẬT KÍCH THƯỚC TỰ ĐỘNG ---
                    // Nếu kích thước video thay đổi hoặc mới kết nối -> Cập nhật lại Panel
                    if (frame.imageWidth > 0 && frame.imageHeight > 0) {
                        if (screenPanel.serverWidth != frame.imageWidth || screenPanel.serverHeight != frame.imageHeight) {
                            screenPanel.setServerSize(frame.imageWidth, frame.imageHeight);
                            screenPanel.updateBufferSize(frame.imageWidth, frame.imageHeight);
                        }
                    }

                    BufferedImage img = converter.convert(frame);
                    if (img != null) {
                        SwingUtilities.invokeLater(() -> {
                            screenPanel.drawFullImage(img);
                            screenPanel.repaint();
                        });
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (grabber != null) grabber.stop(); } catch (Exception e) {}
        }
    }
}