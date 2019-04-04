import java.net.*;
import java.io.*;
import java.util.*;

public class P2P_Client {
    private static final int OK = 200;
	private static final int BAD_REQUEST = 400;
	private static final int NOT_FOUND = 404;
	private static final int HTTP_NOT_SUPPORTED = 505;
    private static final int NUM_DHT_SERVERS = 4;
    private static String DHTOneIP;
    private static int DHTOnePort;
    private static DatagramSocket UDPSocket;

    public static void main(String[] args) throws Exception {
        //DHTOneIP = args[0];
        //DHTOnePort=args[1];
        init();
        //String key = "fig3";
        //int hashedKey = hashKey(key);
        //System.out.println(hashedKey);

        //sendToDHT();
    }

    public static void sendToDHT(String message, String IP, int port) throws IOException { //UDP Client --> Server
        byte[] sendData = new byte[4096];
        sendData = message.getBytes();

        InetAddress addr = InetAddress.getByName(IP);

        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, addr, port);

        UDPSocket.send(sendPacket);
    }

    //hash function to determine DHT to send to
    public static int hashKey(String content){
        int ASCIISum = 0;
        for(int i = 0; i < content.length(); i++){
            ASCIISum += (int) content.charAt(i);
        }
        return (ASCIISum % NUM_DHT_SERVERS) + 1;
    }

    public static void init() throws Exception{

        UDPSocket = new DatagramSocket();

        sendToDHT("This is a test", "135.0.211.153", 25565);
        String resp = receiveFromDHT();
        System.out.println(resp);
    }

    public static String receiveFromDHT() throws Exception{
        byte[] receiveData = new byte[4096];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        UDPSocket.receive(receivePacket);
        return new String(receivePacket.getData());
    }
    //TODO int DHTToContact <-- hashContent() for use to insert and retrieve

    //TODO inform_and_update(hashedContentToStore, ownIPAddress) -- send to DHT

    //TODO query_for_content(hashedContentWanted) -- send to DHT

    //TODO exit(DHT)
}
