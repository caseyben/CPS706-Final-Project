import java.net.*;
import java.io.*;

public class DHT {
    private static final int NUM_DHT_SERVERS = 4;
    public static void main(String[] args) throws IOException {
        //TODO Hashtable(K, V) = (Name of file wanted, IP address of client with it)

        String key = "kimi no na wa";
        //System.out.println(hashKey(key));

        UDPServer();

        //TCPServer(25565);
        //TCPServer(25566);
        //TCPClient(25565);
        //TCPClient(25566);
    }

    public static void UDPServer() throws IOException { //UDP from Client --> Server
        DatagramSocket DHTSocket = new DatagramSocket(25565);

        byte[] receiveDataBuffer = new byte[4096];
        byte[] returnDataBuffer = new byte[4096];

        while(true){
            DatagramPacket receivePacket = new DatagramPacket(receiveDataBuffer, receiveDataBuffer.length);

            DHTSocket.receive(receivePacket);

            String string = new String(receivePacket.getData());

            System.out.println(string);

            InetAddress returnAddress = receivePacket.getAddress();
            int returnPort = receivePacket.getPort();
            returnDataBuffer = string.getBytes(); //(string + "Acknowledged from" + returnAddress).getBytes();

            DatagramPacket returnPacket = new DatagramPacket(returnDataBuffer, returnDataBuffer.length, returnAddress, returnPort);
            DHTSocket.send(returnPacket);

        }
    }



    public static void TCPClient(int port) throws IOException {
        //Establish Socket
        Socket clientSocket = new Socket("127.0.0.1", port);
        System.out.println("Client on port: " + port);

        //In
        BufferedReader fromTCPServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        String fromServerText = fromTCPServer.readLine();
        System.out.println("Received from Server: " + fromServerText);


        //Out
        DataOutputStream toServer = new DataOutputStream(clientSocket.getOutputStream());

        toServer.writeBytes("Returning on port" + port + '\n');
    }

    public static void TCPServer(int port) throws IOException {
        //Establish Socket
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Server on port: " + port);

         while(true){
             //Establish Socket
             Socket serverConnectionSocket = serverSocket.accept();

             //Data streams
             BufferedReader fromClient = new BufferedReader(new InputStreamReader(serverConnectionSocket.getInputStream()));
             DataOutputStream toClient = new DataOutputStream(serverConnectionSocket.getOutputStream());

             //In
             String fromClientText = fromClient.readLine();
             System.out.println(fromClientText);

             //Out
             toClient.writeBytes(fromClientText + "ACK" + '\n');




         }
    }

    //hash function to determine which DHT to store in
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
