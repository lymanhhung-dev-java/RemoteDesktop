package com.remote.server;


public class NativeWrapper {
    static {
        try {
            System.loadLibrary("screen_capture");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("ERROR: Khong tim thay file screen_capture.dll!");
            e.printStackTrace();
        }
    }
    // Khai báo hàm Native
    public native int[] captureScreen(int x, int y, int width, int height);
}