package com.remote.client.handlers;

import com.remote.common.ClipboardUtils;
import com.remote.common.Protocol;

import java.io.DataOutputStream;
import java.net.Socket;

public class ClipboardWatcher implements Runnable {

    private final Socket socket;
    private String lastText = "";

    public ClipboardWatcher(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            while (true) {
                String current = ClipboardUtils.getClipboardText();

                if (current != null && !current.equals(lastText)) {
                    lastText = current;

                    dos.writeInt(Protocol.CMD_CLIPBOARD_TEXT);
                    dos.writeUTF(current);
                    dos.flush();
                }

                Thread.sleep(500); 
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}