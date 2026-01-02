package com.remote.client.handlers;

import com.remote.client.components.ScreenPanel;
import com.remote.common.Protocol;

import java.awt.event.*;
import java.io.DataOutputStream;
import java.io.IOException;

public class InputSender implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
    private DataOutputStream dos;
    private ScreenPanel screenPanel;

    public InputSender(DataOutputStream dos, ScreenPanel screenPanel) {
        this.dos = dos;
        this.screenPanel = screenPanel;
    }

    // --- Helper gửi lệnh ---
    private void sendCmd(int type, int p1, int p2) {
        try {
            synchronized (dos) {
                dos.writeInt(type);
                if (type == Protocol.CMD_MOUSE_MOVE) {
                    dos.writeInt(p1);
                    dos.writeInt(p2);
                } else if (type != Protocol.CMD_MOUSE_PRESS && type != Protocol.CMD_MOUSE_RELEASE) {
                    dos.writeInt(p1);
                }
                dos.flush();
            }
        } catch (IOException e) {
            System.out.println("Lỗi gửi input: " + e.getMessage());
        }
    }

    private void sendMouse(int type, MouseEvent e) {
        if (screenPanel.serverWidth == 0) return;
        // Tính toán tỉ lệ tọa độ
        float scaleX = (float) screenPanel.serverWidth / screenPanel.getWidth();
        float scaleY = screenPanel.serverHeight / screenPanel.getHeight();
        int realX = (int) (e.getX() * scaleX);
        int realY = (int) (e.getY() * scaleY);
        sendCmd(type, realX, realY);
    }

    // --- Các Override của Listener ---
    @Override public void mouseMoved(MouseEvent e) { sendMouse(Protocol.CMD_MOUSE_MOVE, e); }
    @Override public void mouseDragged(MouseEvent e) { sendMouse(Protocol.CMD_MOUSE_MOVE, e); }
    @Override public void mousePressed(MouseEvent e) { sendMouse(Protocol.CMD_MOUSE_PRESS, e); }
    @Override public void mouseReleased(MouseEvent e) { sendMouse(Protocol.CMD_MOUSE_RELEASE, e); }
    @Override public void mouseWheelMoved(MouseWheelEvent e) { sendCmd(Protocol.CMD_MOUSE_WHEEL, e.getWheelRotation(), 0); }
    @Override public void keyPressed(KeyEvent e) { sendCmd(Protocol.CMD_KEY_PRESS, e.getKeyCode(), 0); }
    @Override public void keyReleased(KeyEvent e) { sendCmd(Protocol.CMD_KEY_RELEASE, e.getKeyCode(), 0); }
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
}