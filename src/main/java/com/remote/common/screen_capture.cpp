#include <jni.h>
#include <windows.h>
#include <vector>

// Hàm hỗ trợ chụp màn hình GDI
void CaptureScreenData(int x, int y, int w, int h, int* buffer) {
    HDC hScreen = GetDC(NULL);
    HDC hDC = CreateCompatibleDC(hScreen);
    HBITMAP hBitmap = CreateCompatibleBitmap(hScreen, w, h);
    SelectObject(hDC, hBitmap);

    // Copy màn hình vào Bitmap (Cực nhanh)
    BitBlt(hDC, 0, 0, w, h, hScreen, x, y, SRCCOPY);

    // Lấy dữ liệu pixel raw
    BITMAPINFOHEADER bi = {0};
    bi.biSize = sizeof(BITMAPINFOHEADER);
    bi.biWidth = w;
    bi.biHeight = -h; // Dấu âm để ảnh không bị lộn ngược
    bi.biPlanes = 1;
    bi.biBitCount = 32;
    bi.biCompression = BI_RGB;

    GetDIBits(hDC, hBitmap, 0, h, buffer, (BITMAPINFO*)&bi, DIB_RGB_COLORS);

    // Dọn dẹp bộ nhớ
    DeleteObject(hBitmap);
    DeleteDC(hDC);
    ReleaseDC(NULL, hScreen);
}

extern "C" {
    JNIEXPORT jintArray JNICALL Java_com_remote_server_NativeWrapper_captureScreen
      (JNIEnv *env, jobject obj, jint x, jint y, jint w, jint h) {
        
        int size = w * h;
        // Cấp phát bộ nhớ đệm
        std::vector<int> pixels(size);
        
        // Gọi hàm chụp màn hình
        CaptureScreenData(x, y, w, h, pixels.data());

        // Chuyển dữ liệu từ C++ sang mảng Java
        jintArray result = env->NewIntArray(size);
        env->SetIntArrayRegion(result, 0, size, (jint*)pixels.data());
        
        return result;
    }
}