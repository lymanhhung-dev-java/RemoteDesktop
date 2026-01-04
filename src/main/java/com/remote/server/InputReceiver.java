package com.remote.server;

import com.remote.common.Protocol;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.io.DataInputStream;
import java.net.Socket;
import com.remote.common.ClipboardUtils;

public class InputReceiver extends Thread {
    private Socket socket;
    private DataInputStream dis;
    private Robot robot;

    public InputReceiver(Socket socket, DataInputStream dis, Robot robot) {
        this.socket = socket;
        this.dis = dis;
        this.robot = robot;
    }

    @Override
    public void run() {
        try {
            while (!socket.isClosed()) {
                int type = dis.readInt();

                switch (type) {
                    case Protocol.CMD_MOUSE_MOVE:
                        int x = dis.readInt();
                        int y = dis.readInt();
                        robot.mouseMove(x, y);
                        break;
                    case Protocol.CMD_MOUSE_PRESS:
                        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                        break;
                    case Protocol.CMD_MOUSE_RELEASE:
                        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                        break;
                    case Protocol.CMD_KEY_PRESS:
                        robot.keyPress(dis.readInt());
                        break;
                    case Protocol.CMD_KEY_RELEASE:
                        robot.keyRelease(dis.readInt());
                        break;
                    case Protocol.CMD_MOUSE_WHEEL:
                        robot.mouseWheel(dis.readInt());
                        break;
                    case Protocol.CMD_CLIPBOARD_TEXT:
                        String text = dis.readUTF();
                        ClipboardUtils.setClipboardText(text);
                        break;
                }

            }
        } catch (Exception e) {
            System.out.println("InputReceiver ended.");
        }
    }
}