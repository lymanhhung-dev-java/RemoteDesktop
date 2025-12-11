package com.remote.server;

import java.awt.Rectangle;

public class NativeScreenCapture {
    // Load thư viện DLL khi chạy
    static {
        try {
            // Tê(ví dụ: screen_captn file .dll bạn sẽ tạo ure.dll)
            System.loadLibrary("screen_capture"); 
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Chưa tìm thấy file DLL C++! Hãy chắc chắn bạn đã copy nó vào đúng chỗ.");
            e.printStackTrace();
        }
    }
    // Hàm native gọi sang C++
    // Trả về mảng int[] chứa dữ liệu pixel (ARGB)
    public native int[] captureScreen(int x, int y, int width, int height);
}