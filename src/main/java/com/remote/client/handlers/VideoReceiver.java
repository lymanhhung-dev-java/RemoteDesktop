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
            // Để đọc tiếp luồng dữ liệu mà không bị ngắt quãng
            grabber = new FFmpegFrameGrabber(dis);
            
            grabber.setFormat("h264");
            grabber.setOption("probesize", "10000000"); // 10MB (Tăng lên để dò kỹ hơn)
            grabber.setOption("analyzeduration", "10000000"); 
            grabber.setVideoOption("preset", "ultrafast");
            
            grabber.start();

            Java2DFrameConverter converter = new Java2DFrameConverter();

            while (!socket.isClosed()) {
                Frame frame = grabber.grabImage();
                if (frame != null) {
                    if (frame.imageWidth > 0 && frame.imageHeight > 0) {
                        if (screenPanel.serverWidth != frame.imageWidth || screenPanel.serverHeight != frame.imageHeight) {
                            screenPanel.setServerSize(frame.imageWidth, frame.imageHeight);
                            screenPanel.updateBufferSize(frame.imageWidth, frame.imageHeight);
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
            }
        } catch (Exception e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Kết nối video bị ngắt!"));
        } finally {
            try { if (grabber != null) grabber.stop(); } catch (Exception e) {}
            try { socket.close(); } catch (Exception e) {}
        }
    }
}