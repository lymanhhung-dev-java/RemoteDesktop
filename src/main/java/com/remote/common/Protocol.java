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
    public static final int CMD_AUTH_REQUEST = 20; 
    public static final int CMD_AUTH_OK = 21;      
    public static final int CMD_AUTH_FAIL = 22;   
    public static final int CMD_FRAME_END = 11;
}