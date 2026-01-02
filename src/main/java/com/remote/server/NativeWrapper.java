package com.remote.server;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class NativeWrapper {
    private boolean isLoaded = false;
    private Robot robot;

    public NativeWrapper() {
        // Cố gắng load thư viện C++
        try {
            System.loadLibrary("screen_capture");
            isLoaded = true;
            System.out.println(">>> Đã load thành công: screen_capture.dll");
        } catch (Throwable e) { 
            // Bắt cả Error và Exception để không bị crash chương trình
            System.err.println("!!! CẢNH BÁO: Không load được DLL. Lỗi: " + e.getMessage());
            System.err.println("!!! Đang chuyển sang chế độ Java Robot (An toàn).");
            isLoaded = false;
        }
        
        // Luôn khởi tạo Robot để dự phòng
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    // Khai báo hàm Native
    private native int[] captureScreen(int x, int y, int width, int height);

    // Hàm gọi an toàn: Nếu C++ lỗi thì dùng Java Robot
    public int[] captureScreenSafe(int x, int y, int width, int height) {
        if (isLoaded) {
            try {
                return captureScreen(x, y, width, height);
            } catch (Throwable e) {
                System.err.println("Lỗi khi gọi hàm C++: " + e.getMessage());
                isLoaded = false; // Tắt C++, lần sau dùng Robot
            }
        }
        
        // Dự phòng bằng Java Robot
        if (robot != null) {
            Rectangle rect = new Rectangle(x, y, width, height);
            BufferedImage img = robot.createScreenCapture(rect);
            return ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
        }
        return new int[0];
    }
}