package com.remote.server;

import com.remote.common.Protocol;
import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class RelayServer {
    private static final int PORT = 7777; // Cổng cho Relay 
    private static ConcurrentHashMap<String, Socket> waitingHosts = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println(">>> RELAY SERVER STARTED ON PORT " + PORT + " <<<");
            
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> handleConnection(socket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleConnection(Socket socket) {
        try {
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            int type = dis.readInt(); // Đọc lệnh đầu tiên

            if (type == Protocol.CMD_REGISTER_HOST) {
                // Máy bị điều khiển (Host) kết nối lên để báo danh
                String id = dis.readUTF();
                waitingHosts.put(id, socket);
                System.out.println("Host registered: ID=" + id + " from " + socket.getInetAddress());
                // Socket này sẽ treo ở đây để chờ...
                
            } else if (type == Protocol.CMD_CONNECT_TO_ID) {
                // TRƯỜNG HỢP 2: Máy điều khiển (Client) muốn kết nối vào ID nào đó
                String targetId = dis.readUTF();
                System.out.println("Client request connect to: " + targetId);
                
                Socket hostSocket = waitingHosts.remove(targetId); // Lấy socket máy Host ra

                if (hostSocket != null && !hostSocket.isClosed()) {
                    dos.writeInt(Protocol.CMD_CONNECT_SUCCESS);
                    dos.flush();
                    
                    // Gửi tín hiệu báo cho Host biết là có người kết nối
                    DataOutputStream hostDos = new DataOutputStream(hostSocket.getOutputStream());
                    hostDos.writeInt(Protocol.CMD_CONNECT_SUCCESS);
                    hostDos.flush();

                    System.out.println("Bridging " + socket.getInetAddress() + " <--> " + hostSocket.getInetAddress());

                    // --- BẮT ĐẦU NỐI DÂY (PIPE) ---
                    //  Copy dữ liệu từ Client -> Host
                    new Thread(() -> pipeStreams(socket, hostSocket)).start();
                    
                    // Copy dữ liệu từ Host -> Client
                    pipeStreams(hostSocket, socket); 

                } else {
                    dos.writeInt(Protocol.CMD_ID_NOT_FOUND);
                    System.out.println("ID not found: " + targetId);
                    socket.close();
                }
            }
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    // Hàm copy dữ liệu từ Input của socket A sang Output của socket B
    private static void pipeStreams(Socket inputSocket, Socket outputSocket) {
        try {
            InputStream in = inputSocket.getInputStream();
            OutputStream out = outputSocket.getOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
                out.flush();
            }
        } catch (Exception e) {
            // Khi một bên ngắt kết nối
        } finally {
            try { inputSocket.close(); } catch (Exception e) {}
            try { outputSocket.close(); } catch (Exception e) {}
        }
    }
}