package com.remote.client.components;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class ScreenPanel extends JPanel {
    private BufferedImage backBuffer;
    private BufferedImage frontBuffer;
    public float serverWidth = 0;
    public float serverHeight = 0;
    

    public void setServerSize(int w, int h) {
        this.serverWidth = w;
        this.serverHeight = h;
    }

    public void updateBufferSize(int w, int h) {
        if (backBuffer == null || backBuffer.getWidth() != w || backBuffer.getHeight() != h) {
            backBuffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            frontBuffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        }
    }

    public void drawTileToBackBuffer(int[] pixels, int x, int y, int w, int h) {
        if (backBuffer == null) return;
        backBuffer.setRGB(x, y, w, h, pixels, 0, w);
    }

    public void swapBuffers() {
        if (backBuffer == null || frontBuffer == null) return;
        Graphics2D g2d = frontBuffer.createGraphics();
        g2d.drawImage(backBuffer, 0, 0, null);
        g2d.dispose();
        this.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (frontBuffer != null) {
            g.drawImage(frontBuffer, 0, 0, this.getWidth(), this.getHeight(), null);
        }
    }
    public void drawFullImage(BufferedImage img) {
        if (backBuffer == null) return;
        
        Graphics2D g = backBuffer.createGraphics();
        g.drawImage(img, 0, 0, backBuffer.getWidth(), backBuffer.getHeight(), null);
        g.dispose();
        
        swapBuffers();
    }
}
