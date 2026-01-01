package com.remote.server;

import com.remote.common.Config;
import com.remote.common.Protocol;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpDiscoveryServer extends Thread {
    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(Config.UDP_PORT)) {
            socket.setBroadcast(true);
            byte[] buf = new byte[1024];
            System.out.println("UDP Discovery Service started on port " + Config.UDP_PORT);
            
            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                
                String msg = new String(packet.getData(), 0, packet.getLength()).trim();
                if (msg.equals(Protocol.DISCOVERY_REQ)) {
                    String hostName = InetAddress.getLocalHost().getHostName();
                    String response = Protocol.DISCOVERY_RES + ";" + hostName;
                    byte[] data = response.getBytes();
                    
                    DatagramPacket sendPacket = new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort());
                    socket.send(sendPacket);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
