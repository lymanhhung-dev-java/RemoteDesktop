#include <jni.h>
#include <windows.h>
#include <d3d11.h>
#include <dxgi1_2.h>
#include <vector>

#pragma comment(lib, "d3d11.lib")
#pragma comment(lib, "dxgi.lib")

ID3D11Device* g_Device = nullptr;
ID3D11DeviceContext* g_Context = nullptr;
IDXGIOutputDuplication* g_DeskDupl = nullptr;
ID3D11Texture2D* g_StagingTex = nullptr;
D3D11_TEXTURE2D_DESC g_TexDesc;

void Cleanup() {
    if (g_StagingTex) g_StagingTex->Release();
    if (g_DeskDupl) g_DeskDupl->Release();
    if (g_Context) g_Context->Release();
    if (g_Device) g_Device->Release();
    g_StagingTex = nullptr;
    g_DeskDupl = nullptr;
    g_Context = nullptr;
    g_Device = nullptr;
}

HRESULT InitDXGI() {
    HRESULT hr = S_OK;
    D3D_FEATURE_LEVEL featureLevel;
    hr = D3D11CreateDevice(nullptr, D3D_DRIVER_TYPE_HARDWARE, nullptr, 0, nullptr, 0, D3D11_SDK_VERSION, &g_Device, &featureLevel, &g_Context);
    if (FAILED(hr)) return hr;

    IDXGIDevice* dxgiDevice = nullptr;
    hr = g_Device->QueryInterface(__uuidof(IDXGIDevice), (void**)&dxgiDevice);
    if (FAILED(hr)) return hr;

    IDXGIAdapter* dxgiAdapter = nullptr;
    hr = dxgiDevice->GetParent(__uuidof(IDXGIAdapter), (void**)&dxgiAdapter);
    dxgiDevice->Release();
    if (FAILED(hr)) return hr;

    IDXGIOutput* dxgiOutput = nullptr;
    hr = dxgiAdapter->EnumOutputs(0, &dxgiOutput);
    dxgiAdapter->Release();
    if (FAILED(hr)) return hr;

    IDXGIOutput1* dxgiOutput1 = nullptr;
    hr = dxgiOutput->QueryInterface(__uuidof(IDXGIOutput1), (void**)&dxgiOutput1);
    dxgiOutput->Release();
    if (FAILED(hr)) return hr;

    hr = dxgiOutput1->DuplicateOutput(g_Device, &g_DeskDupl);
    dxgiOutput1->Release();
    
    return hr;
}

extern "C" {
    JNIEXPORT jintArray JNICALL Java_com_remote_server_NativeWrapper_captureScreen
      (JNIEnv *env, jobject obj, jint x, jint y, jint w, jint h) {
        
        if (!g_DeskDupl) {
            if (FAILED(InitDXGI())) {
                return nullptr; // Lỗi khởi tạo
            }
        }

        IDXGIResource* desktopResource = nullptr;
        DXGI_OUTDUPL_FRAME_INFO frameInfo;
        
        HRESULT hr = g_DeskDupl->AcquireNextFrame(100, &frameInfo, &desktopResource);
        
        if (hr == DXGI_ERROR_WAIT_TIMEOUT) {
            return nullptr; 
        }
        if (FAILED(hr)) {
            Cleanup();
            return nullptr;
        }

        ID3D11Texture2D* gpuTex = nullptr;
        hr = desktopResource->QueryInterface(__uuidof(ID3D11Texture2D), (void**)&gpuTex);
        desktopResource->Release();

        D3D11_TEXTURE2D_DESC desc;
        gpuTex->GetDesc(&desc);
        
        if (!g_StagingTex || g_TexDesc.Width != desc.Width || g_TexDesc.Height != desc.Height) {
            if (g_StagingTex) g_StagingTex->Release();
            
            desc.BindFlags = 0;
            desc.CPUAccessFlags = D3D11_CPU_ACCESS_READ;
            desc.Usage = D3D11_USAGE_STAGING;
            desc.MiscFlags = 0;
            g_TexDesc = desc;
            
            g_Device->CreateTexture2D(&desc, nullptr, &g_StagingTex);
        }

        g_Context->CopyResource(g_StagingTex, gpuTex);
        gpuTex->Release();
        g_DeskDupl->ReleaseFrame();

        D3D11_MAPPED_SUBRESOURCE mapped;
        hr = g_Context->Map(g_StagingTex, 0, D3D11_MAP_READ, 0, &mapped);
        if (FAILED(hr)) return nullptr;

        int size = w * h;
        jintArray result = env->NewIntArray(size);
        jint* buf = new jint[size];
        
        unsigned char* source = (unsigned char*)mapped.pData;
        int rowPitch = mapped.RowPitch;

        for (int row = 0; row < h; ++row) {
            const int* rowSrc = (const int*)(source + (row + y) * rowPitch + x * 4);
            jint* rowDst = buf + row * w; 
            memcpy(rowDst, rowSrc, w * 4);
        }

        g_Context->Unmap(g_StagingTex, 0);
        
        env->SetIntArrayRegion(result, 0, size, buf);
        delete[] buf;

        return result;
    }
}