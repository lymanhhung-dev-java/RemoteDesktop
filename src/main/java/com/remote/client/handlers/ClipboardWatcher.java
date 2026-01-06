package com.remote.client.handlers;

import com.remote.common.ClipboardUtils;
import com.remote.common.Protocol;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClipboardWatcher implements Runnable {
    private final DataOutputStream dos;
    private volatile boolean running = true;
    private String lastText = "";

    public ClipboardWatcher(DataOutputStream dos) {
        this.dos = dos;
    }

    public void stopRunning() {
        this.running = false;
    }

    @Override
    public void run() {
        while (running) {
            try {
                String current = ClipboardUtils.getClipboardText();

                if (current != null && !current.isEmpty() && !current.equals(lastText)) {
                    lastText = current;
                    
                    synchronized (dos) {
                        dos.writeByte(Protocol.CMD_CLIPBOARD_TEXT); 
                        dos.writeUTF(current);                      
                        dos.flush();
                    }
                   
                }

                Thread.sleep(1000); 
            } catch (InterruptedException e) {
                break;
            } catch (IOException e) {
                System.out.println("Lỗi gửi Clipboard: " + e.getMessage());
                running = false;
            }
        }
    }
}