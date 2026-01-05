package com.remote.common;

public class Protocol {
    public static final int PORT = 9999;
    public static final int UDP_PORT = 8888;
    public static final String DISCOVERY_REQ = "RDP_DISCOVER_REQ";
    public static final String DISCOVERY_RES = "RDP_DISCOVER_RES";

    public static final int CMD_MOUSE_MOVE = 1;
    public static final int CMD_MOUSE_PRESS = 2;
    public static final int CMD_MOUSE_RELEASE = 3;
    public static final int CMD_KEY_PRESS = 4;
    public static final int CMD_KEY_RELEASE = 5;
    public static final int CMD_MOUSE_WHEEL = 6;

    public static final int CMD_SCREEN_SIZE = 7;
    public static final int CMD_SEND_TILE = 10;
    public static final int CMD_FRAME_END = 11;

    public static final int CMD_AUTH_REQUEST = 20;
    public static final int CMD_AUTH_OK = 21;
    public static final int CMD_AUTH_FAIL = 22;

    public static final int CMD_CLIPBOARD_TEXT = 23;

    public static final byte CMD_FILE_START = 50; 
    public static final byte CMD_FILE_DATA  = 51; 
    public static final byte CMD_FILE_END   = 52; 

    public static final int CMD_CHAT_MSG = 30;

    // --- CÁC LỆNH CHO CHẾ ĐỘ ONLINE (RELAY) ---
    public static final int CMD_REGISTER_HOST = 100;    // Máy Server báo danh: "Tôi là 123"
    public static final int CMD_CONNECT_TO_ID = 101;    // Client yêu cầu: "Cho tôi nối máy 123"
    public static final int CMD_ID_NOT_FOUND = 102;     // Relay báo: "Không tìm thấy ID đó"
    public static final int CMD_CONNECT_SUCCESS = 103;  // Relay báo: "Kết nối thành công"

}