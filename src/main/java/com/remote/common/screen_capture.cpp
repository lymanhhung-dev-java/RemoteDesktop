#include <jni.h>
#include <windows.h>
#include <iostream>

HDC hScreenDC = NULL;
HDC hMemoryDC = NULL;
HBITMAP hBitmap = NULL;
HBITMAP hOldBitmap = NULL;
int lastWidth = 0;
int lastHeight = 0;

extern "C" {
    JNIEXPORT jintArray JNICALL Java_com_remote_server_NativeWrapper_captureScreen
      (JNIEnv *env, jobject obj, jint x, jint y, jint w, jint h) {

        // 1. Khởi tạo DC (Chỉ 1 lần)
        if (hScreenDC == NULL) {
            hScreenDC = GetDC(NULL);
            hMemoryDC = CreateCompatibleDC(hScreenDC);
        }

        // 2. Tạo Bitmap mới nếu kích thước thay đổi
        if (hBitmap == NULL || w != lastWidth || h != lastHeight) {
            if (hBitmap != NULL) {
                SelectObject(hMemoryDC, hOldBitmap);
                DeleteObject(hBitmap);
            }
            hBitmap = CreateCompatibleBitmap(hScreenDC, w, h);
            hOldBitmap = (HBITMAP)SelectObject(hMemoryDC, hBitmap);
            lastWidth = w;
            lastHeight = h;
        }

        // 3. Chụp màn hình (BitBlt)
        if (!BitBlt(hMemoryDC, 0, 0, w, h, hScreenDC, x, y, SRCCOPY)) {
             return NULL;
        }

        // 4. Lấy dữ liệu Pixel
        int size = w * h;
        jintArray result = env->NewIntArray(size);
        jint* buf = new jint[size];

        BITMAPINFOHEADER bi;
        bi.biSize = sizeof(BITMAPINFOHEADER);
        bi.biWidth = w;
        bi.biHeight = -h; 
        bi.biPlanes = 1;
        bi.biBitCount = 32;
        bi.biCompression = BI_RGB;
        bi.biSizeImage = 0;
        
        GetDIBits(hMemoryDC, hBitmap, 0, h, buf, (BITMAPINFO*)&bi, DIB_RGB_COLORS);

        env->SetIntArrayRegion(result, 0, size, buf);
        delete[] buf;
        
        return result;
    }
}