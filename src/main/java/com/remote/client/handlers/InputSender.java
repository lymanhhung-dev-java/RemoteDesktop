package com.remote.client.handlers;

import com.remote.client.components.ScreenPanel;
import com.remote.common.Protocol;

import java.awt.event.*;
import java.io.DataOutputStream;
import java.io.IOException;

public class InputSender implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
    private DataOutputStream dos;
    private ScreenPanel screenPanel;
    private long lastSendTime = 0;

    public InputSender(DataOutputStream dos, ScreenPanel screenPanel) {
        this.dos = dos;
        this.screenPanel = screenPanel;
    }

    private void sendCmd(int type, int p1, int p2) {
        try {
            synchronized (dos) {
                dos.writeByte(type);
                if (type == Protocol.CMD_MOUSE_MOVE) {
                    dos.writeInt(p1);
                    dos.writeInt(p2);
                } 
                else {
                    dos.writeInt(p1);
                }
                dos.flush();
            }
        } catch (IOException e) {
            System.out.println("Lỗi gửi input: " + e.getMessage());
        }
    }

    private int getButtonMask(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) return InputEvent.BUTTON1_DOWN_MASK;
        if (e.getButton() == MouseEvent.BUTTON3) return InputEvent.BUTTON3_DOWN_MASK;
        if (e.getButton() == MouseEvent.BUTTON2) return InputEvent.BUTTON2_DOWN_MASK; 
        return InputEvent.BUTTON1_DOWN_MASK;
    }

    private void sendMouseLocation(int type, MouseEvent e) {
        long now = System.currentTimeMillis();
        if (type == Protocol.CMD_MOUSE_MOVE && (now - lastSendTime < 40)) {
            return; 
        }
        lastSendTime = now;
        
        if (screenPanel.serverWidth == 0) return;
        
        float scaleX = (float) screenPanel.serverWidth / screenPanel.getWidth();
        float scaleY = screenPanel.serverHeight / screenPanel.getHeight();
        int realX = (int) (e.getX() * scaleX);
        int realY = (int) (e.getY() * scaleY);
        
        sendCmd(type, realX, realY);
    }


    @Override public void mouseMoved(MouseEvent e) { sendMouseLocation(Protocol.CMD_MOUSE_MOVE, e); }
    @Override public void mouseDragged(MouseEvent e) { sendMouseLocation(Protocol.CMD_MOUSE_MOVE, e); }

    @Override public void mousePressed(MouseEvent e) { 
        int mask = getButtonMask(e);
        sendCmd(Protocol.CMD_MOUSE_PRESS, mask, 0); 
    }
    
    @Override public void mouseReleased(MouseEvent e) { 
        int mask = getButtonMask(e);
        sendCmd(Protocol.CMD_MOUSE_RELEASE, mask, 0); 
    }

    @Override public void mouseWheelMoved(MouseWheelEvent e) { sendCmd(Protocol.CMD_MOUSE_WHEEL, e.getWheelRotation(), 0); }
    @Override public void keyPressed(KeyEvent e) { sendCmd(Protocol.CMD_KEY_PRESS, e.getKeyCode(), 0); }
    @Override public void keyReleased(KeyEvent e) { sendCmd(Protocol.CMD_KEY_RELEASE, e.getKeyCode(), 0); }
    
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
}