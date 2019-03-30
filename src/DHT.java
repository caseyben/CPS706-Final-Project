import java.net.*;
import java.io.*;
import java.util.*;

public class DHT {
    private static final int NUM_DHT_SERVERS = 4;
    public static void main(String[] args) throws IOException {
        //TODO Hashtable(K, V) = (Name of file wanted, IP address of client with it)

        String key = "kimi no na wa";
        System.out.println(hashKey(key));

        receiveFromUDP();
    }

    public static void receiveFromUDP() throws IOException { //UDP from Client --> Server
        DatagramSocket DHTSocket = new DatagramSocket(25565);

        byte[] receiveDataBuffer = new byte[4096];

        while(true){
            DatagramPacket receivePacket = new DatagramPacket(receiveDataBuffer, receiveDataBuffer.length);

            DHTSocket.receive(receivePacket);

            String string = new String(receivePacket.getData());

            System.out.println(string);
        }
    }

    //hash function to determine which peer to store in
    public static int hashKey(String content){
        int ASCIISum = 0;
        for(int i = 0; i < content.length(); i++){
            ASCIISum += (int) content.charAt(i);
        }
        return (ASCIISum % NUM_DHT_SERVERS) + 1;
    }

    //TODO Initialise IP and port() (?)

    //TODO insert(K, V)

    //TODO retreive(hash(K))
        //TODO returnIP()

    //TODO remove(K, V)
}
