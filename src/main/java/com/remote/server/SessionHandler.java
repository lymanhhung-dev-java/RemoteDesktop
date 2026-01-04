package com.remote.server;
import com.remote.common.Protocol;

import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class SessionHandler extends Thread {
    private Socket socket;
    private Robot robot;
    private Rectangle rect;
    private String serverPassword;

    public SessionHandler(Socket socket, Robot robot, Rectangle rect, String serverPassword) {
        this.socket = socket;
        this.robot = robot;
        this.rect = rect;
        this.serverPassword = serverPassword;
    }

    @Override
    public void run() {
        try {
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            int cmd = dis.readInt();
            if (cmd == Protocol.CMD_AUTH_REQUEST) {
                String clientPass = dis.readUTF();
                if (serverPassword.equals(clientPass)) {
                    dos.writeInt(Protocol.CMD_AUTH_OK);
                    dos.flush();
                    System.out.println("Client authenticated!");

                    // SỬ DỤNG LẠI SCREEN SENDER CŨ
                    new ScreenSender(socket, dos, rect).start();

                    new InputReceiver(socket, dis, robot).start();
                    
                    // Khởi động ChatReceiver để nhận tin nhắn
                    new ChatReceiver(socket, dis).start();
                } else {
                    dos.writeInt(Protocol.CMD_AUTH_FAIL);
                    dos.flush();
                    socket.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}