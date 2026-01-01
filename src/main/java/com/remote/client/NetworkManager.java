package com.remote.client;

import com.remote.common.Config;
import com.remote.common.Protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class NetworkManager {
    
    // Kết nối và xác thực. Trả về Socket nếu thành công, ném Exception nếu thất bại.
    public static Socket connect(String ip, String password) throws Exception {
        Socket socket = new Socket(ip, Config.TCP_PORT);
        DataInputStream dis = new DataInputStream(socket.getInputStream());
        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

        // Gửi yêu cầu xác thực
        dos.writeInt(Protocol.CMD_AUTH_REQUEST);
        dos.writeUTF(password);
        dos.flush();

        int response = dis.readInt();
        if (response == Protocol.CMD_AUTH_OK) {
            return socket;
        } else {
            socket.close();
            throw new Exception("Sai mật khẩu hoặc bị từ chối!");
        }
    }
}