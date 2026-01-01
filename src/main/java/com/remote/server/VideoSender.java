package com.remote.server;

import com.remote.common.Protocol;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.Frame;
import org.bytedeco.ffmpeg.global.avcodec;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.DataOutputStream;
import java.net.Socket;

public class VideoSender extends Thread {
    private Socket socket;
    private DataOutputStream dos;
    private Rectangle rect;
    private NativeWrapper nativeCapturer;

    public VideoSender(Socket socket, DataOutputStream dos, Rectangle rect) {
        this.socket = socket;
        this.dos = dos;
        this.rect = rect;
        this.nativeCapturer = new NativeWrapper();
    }

    @Override
    public void run() {
        FFmpegFrameRecorder recorder = null;
        try {
            // Gửi kích thước màn hình để Client biết mà setup
            // dos.writeInt(Protocol.CMD_SCREEN_SIZE);
            // dos.writeInt(rect.width);
            // dos.writeInt(rect.height);
            // dos.flush();
            recorder = new FFmpegFrameRecorder(socket.getOutputStream(), rect.width, rect.height);

            // Cấu hình FFmpeg Recorder: Ghi trực tiếp vào Output Stream của Socket
            // Format "h264", bitrate 2Mbps (tùy mạng), 30-60fps
            recorder = new FFmpegFrameRecorder(socket.getOutputStream(), rect.width, rect.height);
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            recorder.setFormat("h264");
            recorder.setFrameRate(30); 
            recorder.setVideoBitrate(2000 * 1000); // 2 Mbps (Tăng lên nếu muốn nét hơn)
            
            // Cấu hình giảm độ trễ tối đa (Zero Latency)
            recorder.setVideoOption("preset", "ultrafast");
            recorder.setVideoOption("tune", "zerolatency");
            recorder.setVideoOption("crf", "25"); // Chất lượng ảnh (thấp hơn là nét hơn, 20-28 là ổn)

            recorder.start();

            Java2DFrameConverter converter = new Java2DFrameConverter();
            BufferedImage buffer = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_RGB);

            while (!socket.isClosed()) {
                long start = System.currentTimeMillis();

                // 1. Chụp màn hình (Dùng Native C++ cho nhanh)
                int[] pixels = nativeCapturer.captureScreen(rect.x, rect.y, rect.width, rect.height);

                if (pixels != null) {
                    // 2. Đổ dữ liệu pixel vào BufferedImage
                    buffer.setRGB(0, 0, rect.width, rect.height, pixels, 0, rect.width);
                    
                    // 3. Convert sang Frame của JavaCV
                    Frame frame = converter.convert(buffer);
                    
                    // 4. Encode và Gửi đi (FFmpeg tự xử lý việc chia packet gửi qua mạng)
                    recorder.record(frame);
                }

                // Giới hạn FPS (để không ngốn 100% CPU)
                long duration = System.currentTimeMillis() - start;
                if (duration < 33) Thread.sleep(33 - duration); // ~30 FPS
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (recorder != null) recorder.stop(); } catch (Exception e) {}
        }
    }
}
