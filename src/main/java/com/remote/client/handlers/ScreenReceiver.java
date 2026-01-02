package com.remote.client.handlers;
import com.remote.client.components.ScreenPanel;
import com.remote.common.ImageUtils;
import com.remote.common.Protocol;

import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;
import java.io.DataInputStream;
import java.net.Socket;

public class ScreenReceiver extends Thread {
    private Socket socket;
    private DataInputStream dis;
    private ScreenPanel screenPanel;

    public ScreenReceiver(Socket socket, DataInputStream dis, ScreenPanel screenPanel) {
        this.socket = socket;
        this.dis = dis;
        this.screenPanel = screenPanel;
    }

    @Override
    public void run() {
        try {
            while (!socket.isClosed()) {
                int cmd = dis.readInt();
                switch (cmd) {
                    case Protocol.CMD_SCREEN_SIZE:
                        int w = dis.readInt();
                        int h = dis.readInt();
                        screenPanel.setServerSize(w, h);
                        screenPanel.updateBufferSize(w, h);
                        break;

                    case Protocol.CMD_SEND_TILE:
                        int x = dis.readInt();
                        int y = dis.readInt();
                        int tw = dis.readInt();
                        int th = dis.readInt();
                        int len = dis.readInt();
                        
                        byte[] data = new byte[len];
                        dis.readFully(data);
                        int[] pixels = ImageUtils.decompressRaw(data, tw, th);
                        
                        SwingUtilities.invokeLater(() -> screenPanel.drawTileToBackBuffer(pixels, x, y, tw, th));
                        break;

                    case Protocol.CMD_FRAME_END:
                        SwingUtilities.invokeLater(() -> screenPanel.swapBuffers());
                        break;
                }
            }
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Mất kết nối với Server!"));
        }
    }
}