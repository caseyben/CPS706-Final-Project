import java.net.*;
import java.io.*;
import java.util.*;

public class P2P_Client {
    private static final int OK = 200;
	private static final int BAD_REQUEST = 400;
	private static final int NOT_FOUND = 404;
	private static final int HTTP_NOT_SUPPORTED = 505;
    private static final int NUM_DHT_SERVERS = 4;

    public static void main(String[] args) throws IOException {
        String key = "fig3";
        int hashedKey = hashKey(key);
        System.out.println(hashedKey);

        sendToDHT();
    }

    public static void sendToDHT() throws IOException { //UDP Client --> Server
        DatagramSocket clientSocket = new DatagramSocket();

        byte[] sendData = new byte[4096];
        //byte[] ipAddr = new byte[]{135, 0, 211, 153};
        InetAddress addr = InetAddress.getByName("135.0.211.153");

        String string = "the newest way to slide in the DMs ;)";

        sendData = string.getBytes();

        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, addr, 25565);

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
