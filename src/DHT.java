import java.net.*;
import java.io.*;
import java.util.*;

public class DHT {
    private static final int NUM_DHT_SERVERS = 4;
    private static Hashtable<String, String> theTable = new Hashtable<>();

    public static int DHT_ID = 0;
    public static String DHT_IP_Address = "";
    public static int DHT_PORT = 0;
    public static String SUCCESSOR_IP = "";

    public static void main(String[] args) throws IOException {
        //TODO Hashtable(K, V) = (Name of file wanted, IP address of client with it)

        //init();
        String key = "kimi no na wa";
        //System.out.println(hashKey(key));
        startTCPServer("", "", 20069);

        UDPServer(20069);

        //TCPServer(25565);
        //TCPServer(25566);
        //TCPServer(25567);
        //TCPServer(25568);

        //TCPClient(25565);
        //TCPClient(25566);
        //TCPClient(25567);
        //TCPClient(25568);
    }

    public static void UDPClient(String message, String destIP, int destPort) throws IOException{
        DatagramSocket UDP_ClientSocket = new DatagramSocket(destPort);
        //Prepare Message
        byte[] UDP_sendData = new byte[4096];
        UDP_sendData = message.getBytes();

        InetAddress UDP_returnIPAddress = InetAddress.getByName(destIP);

        DatagramPacket sendPacket = new DatagramPacket(UDP_sendData, UDP_sendData.length, UDP_returnIPAddress, destPort);

        UDP_ClientSocket.send(sendPacket);
        UDP_ClientSocket.close();
        UDPServer(20069);
    }

    public static void UDPServer(int port) throws IOException { //UDP from Client --> Server
        DatagramSocket UDP_ServerSocket = new DatagramSocket(port);
        System.out.println("UDP_Server on port: " + port);

        //Data Buffers
        byte[] UDP_receiveDataBuffer = new byte[4096];
        byte[] UDP_returnDataBuffer = new byte[4096];

        while(true){
            //In
            DatagramPacket UDP_receivePacket = new DatagramPacket(UDP_receiveDataBuffer, UDP_receiveDataBuffer.length);
            UDP_ServerSocket.receive(UDP_receivePacket);
            String UDP_receiveString = new String(UDP_receivePacket.getData(), 0, UDP_receivePacket.getLength());
            System.out.println(UDP_receiveString);

            //Out
            InetAddress UDP_returnAddress = UDP_receivePacket.getAddress();
            int UDP_returnPort = UDP_receivePacket.getPort();

            String UDP_receiveArray[] = UDP_receiveString.split("~");




            if(UDP_receiveArray[0].equals("FIND")){
                System.out.println("find");
                UDP_ServerSocket.close();
                retreive(UDP_receiveArray[1], UDP_returnAddress.getHostAddress(), UDP_returnPort);
                break;
            } else if (UDP_receiveArray[0].equals("INSERT")){
                System.out.println("insert");
                insert(UDP_receiveArray[1], UDP_returnAddress.getHostAddress());
            } else if (UDP_receiveArray[0].equals("GET_IP")){
                //TODO returnIP();
                System.out.println("init");
                UDP_ServerSocket.close();
                UDPClient("192.168.2.1:1111_192.168.2.2:2222_192.168.2.3:3333", UDP_returnAddress.getHostAddress(), UDP_returnPort);
                break;
            }

            //String UDP_returnString = "192.168.2.1:1111_192.168.2.2:2222_192.168.2.3:3333"; //string + " acknowledged from: " + returnAddress;
            //UDP_returnDataBuffer = UDP_returnString.getBytes();

            //DatagramPacket UDP_returnPacket = new DatagramPacket(UDP_returnDataBuffer, UDP_returnDataBuffer.length, UDP_returnAddress, UDP_returnPort);
            //UDP_ServerSocket.send(UDP_returnPacket);
        }
    }

    public static void TCPClient(String content, String IPAddress, int port) throws IOException {
        //Establish Socket
        Socket TCP_clientSocket = new Socket("127.0.0.1", port);
        System.out.println("TCP_Client on port: " + port);

        //Out
        DataOutputStream TCP_toServer = new DataOutputStream(TCP_clientSocket.getOutputStream());
        TCP_toServer.writeBytes("This is TCP_client on port: " + port + " " + '\n');

        //In
        BufferedReader TCP_fromTCPServer = new BufferedReader(new InputStreamReader(TCP_clientSocket.getInputStream()));
        String TCP_fromServerText = TCP_fromTCPServer.readLine();
        System.out.println("Received from TCP_Server: " + TCP_fromServerText);
    }

    public static void TCPServer(String content, String IPAddress, int port) throws IOException {
        //Establish Socket
        ServerSocket TCP_serverSocket = new ServerSocket(port);
        System.out.println("TCP_Server on port: " + port);

         //while(true){
             //Establish Socket
             Socket TCP_serverConnectionSocket = TCP_serverSocket.accept();

             //In
             BufferedReader TCP_fromClient = new BufferedReader(new InputStreamReader(TCP_serverConnectionSocket.getInputStream()));
             String TCP_fromClientText = TCP_fromClient.readLine();

             //Out
             DataOutputStream TCP_toClient = new DataOutputStream(TCP_serverConnectionSocket.getOutputStream());
             TCP_toClient.writeBytes(TCP_fromClientText + " ACK!" + '\n');
         //}
    }

    //hash function to determine which DHT to store in
    public static int hashKey(String content){
        int ASCIISum = 0;
        for(int i = 0; i < content.length(); i++){
            ASCIISum += (int) content.charAt(i);
        }
        return (ASCIISum % NUM_DHT_SERVERS) + 1;
    }

    /*Between DHT && P2P_Client*/
    public static void insert(String content, String IPAddress){ //P2P_Client inform_and_update()
        theTable.put(content, IPAddress);
    }

    public static void retreive(String content, String IPAddress, int port) throws IOException{ //P2P_Client query_for_content()
        if(theTable.containsKey(content)){
            UDPClient(theTable.get(content), IPAddress, port);
        } else {
            UDPClient("404 content not found", IPAddress , port);
        }
    }

    public static void remove(String content, String IPAddress){ //P2P_Client exit()
        if(theTable.containsKey(content)){
            theTable.remove(content);
        } else {
            forwardEntry(content, IPAddress);
        }
    }

    /*Between DHT && DHT*/
    //TODO return_DHT_IPs
    public static void returnIP(){
        //call TCP Client to send data

    }

    //TODO forward_table_entry
    public static void forwardEntry(String content, String IPAddress){
        //call TCP Client to send data
    }

    //TODO Error checking
    public static void init(){
        Scanner initScanner = new Scanner(System.in);

        System.out.println("Please enter DHT_ID between 1-4: ");
        DHT_ID = initScanner.nextInt();

        System.out.println("Please enter DHT_IP_Address: ");
        DHT_IP_Address = initScanner.next().trim();

        System.out.println("Please enter DHT_PORT: ");
        DHT_PORT = initScanner.nextInt();

        System.out.println("Please enter SUCCESSOR_IP: ");
        SUCCESSOR_IP = initScanner.next().trim();

        initScanner.close();

        System.out.println("DHT_ID: " + DHT_ID);
        System.out.println("DHT_IP_Address: " + DHT_IP_Address);
        System.out.println("DHT_PORT: " + DHT_PORT);
        System.out.println("SUCCESSOR_IP: " + SUCCESSOR_IP);
    }

    public static void startTCPServer(String content, String destIP, int destPort) {
        class InitTCPServer implements Runnable {
            String content;
            String destIP;
            int destPort;

            InitTCPServer(String content, String destIP, int destPort) {
                this.content = content;
                this.destIP = destIP;
                this.destPort = destPort;
            }

            public void run() {
                try {
                    TCPServer(content, destIP, destPort);
                } catch (IOException e) {
                    System.out.println("Incorrect String content, String destIP, or int destPort.");
                }
            }
        }
        Thread TCPServerThread = new Thread(new InitTCPServer(content, destIP, destPort));
        TCPServerThread.start();
    }
}
