import java.net.*;
import java.io.*;
import java.util.*;

public class P2P_Client {
    private static final int NUM_DHT_SERVERS = 4;

    public static void main(String[] args) throws IOException {
        String key = "kimi no na wa";
        int hashedKey = hashKey(key);
        System.out.println(hashedKey);

        sendToDHT();
    }

    public static void sendToDHT() throws IOException { //UDP Client --> Server
        DatagramSocket clientSocket = new DatagramSocket();

        byte[] sendData = new byte[4096];

        String string = "test";

        sendData = string.getBytes();

        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getLoopbackAddress(), 25565);

        clientSocket.send(sendPacket);
    }

    //hash function to determine DHT to send to
    public static int hashKey(String content){
        int ASCIISum = 0;
        for(int i = 0; i < content.length(); i++){
            ASCIISum += (int) content.charAt(i);
        }
        return (ASCIISum % NUM_DHT_SERVERS) + 1;
    }

    //TODO init(IP address) -- find DHTs

    //TODO int DHTToContact <-- hashContent() for use to insert and retrieve

    //TODO inform_and_update(hashedContentToStore, ownIPAddress) -- send to DHT

    //TODO query_for_content(hashedContentWanted) -- send to DHT

    //TODO exit(DHT)
}
