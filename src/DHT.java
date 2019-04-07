import java.net.*;
import java.io.*;
import java.util.*;

public class DHT {
    private static final int NUM_DHT_SERVERS = 4;
    private static Hashtable<String, String> theTable = new Hashtable<>();

    private static int DHT_ID = 0;
    private static String DHT_IP_Address = "";
    private static int DHT_PORT = 0;
    private static String SUCCESSOR_IP = "";
    private static int SUCCESSOR_PORT = 0;
    private static DatagramSocket UDP_SOCKET;
    private static int UDP_PORT = 25565; //TODO Change to another port
    static {
        try {
            UDP_SOCKET = new DatagramSocket(UDP_PORT); //Static?
        } catch (SocketException e) {
            System.out.println("Cannot create UDP port: " + UDP_PORT);
        }
    }

    public static void main(String[] args) throws IOException {

        init(); //set ID:IP:Port of DHT & Successor IP:Port

        //Start TCP_Server
        Thread TCP_Server_Thread = new Thread(startTCPServer);
        TCP_Server_Thread.start();

        //Start UDP_Server on //TODO change UDP Port
        UDPServer();

        //TCPClient("", "", 25565);
    }

    private static void UDPClient(String message, String destIP, int destPort) throws IOException {
        //Prepare Message
        byte[] UDP_sendData;
        UDP_sendData = message.getBytes();

        InetAddress UDP_returnIPAddress = InetAddress.getByName(destIP);

        DatagramPacket sendPacket = new DatagramPacket(UDP_sendData, UDP_sendData.length, UDP_returnIPAddress, destPort);

        UDP_SOCKET.send(sendPacket);
    }

    private static void UDPServer() throws IOException { //UDP from Client --> Server

        System.out.println("UDP_Server on port: " + UDP_PORT);

        //Data Buffers
        byte[] UDP_receiveDataBuffer = new byte[4096];
        //byte[] UDP_returnDataBuffer = new byte[4096];

        while(true){
            //In
            DatagramPacket UDP_receivePacket = new DatagramPacket(UDP_receiveDataBuffer, UDP_receiveDataBuffer.length);
            UDP_SOCKET.receive(UDP_receivePacket);
            String UDP_receiveString = new String(UDP_receivePacket.getData(), 0, UDP_receivePacket.getLength());
            System.out.println(UDP_receiveString);

            //Out
            InetAddress UDP_returnAddress = UDP_receivePacket.getAddress();
            int UDP_returnPort = UDP_receivePacket.getPort();

            String[] UDP_receiveArray = UDP_receiveString.split("~");

            switch (UDP_receiveArray[0]) {
                case "FIND":
                    retrieve(UDP_receiveArray[1], UDP_returnAddress.getHostAddress(), UDP_returnPort);
                    break;
                case "INSERT":
                    insert(UDP_receiveArray[1], UDP_returnAddress.getHostAddress());
                    break;
                case "GET_IP":
                    returnIP(UDP_returnAddress.getHostAddress(), UDP_returnPort);
                    break;
                case "EXIT":
                    if (UDP_receiveArray.length < 2) {
                        System.out.println("No records received");
                    } else {
                        remove(UDP_receiveArray[1], UDP_returnAddress.getHostAddress(), UDP_returnPort);
                    }
                    break;
            }

            //String UDP_returnString = //"192.168.2.1:1111_192.168.2.2:2222_192.168.2.3:3333"; //string + " acknowledged from: " + returnAddress;
            //UDP_returnDataBuffer = UDP_returnString.getBytes();

            //DatagramPacket UDP_returnPacket = new DatagramPacket(UDP_returnDataBuffer, UDP_returnDataBuffer.length, UDP_returnAddress, UDP_returnPort);
            //UDP_ServerSocket.send(UDP_returnPacket);
        }
    }

    private static void TCPClient(String content, String IPAddress, int port) throws IOException {
        //Establish Socket
        Socket TCP_clientSocket = new Socket("192.168.0.151", SUCCESSOR_PORT); //TODO change to DHT_SUCCESSOR_IP
        System.out.println("TCP_Client on port: " + SUCCESSOR_PORT);

        String[] TCP_sendArray = content.split("~");

        //Out
        DataOutputStream TCP_toServer = new DataOutputStream(TCP_clientSocket.getOutputStream());
        //TCP_toServer.writeBytes("This is TCP_client on port: " + DHT_PORT + " " + '\n');

        String toServerString = content + "$" + IPAddress + "$" + port + " " + '\n'; //Will break without the \n, do not ask why.

        if(TCP_sendArray[0].equals("GET_IP")){
            TCP_toServer.writeBytes(toServerString);
        } else if(TCP_sendArray[0].equals("EXIT")){
            TCP_toServer.writeBytes(toServerString);
        }

        //In
        //BufferedReader TCP_fromTCPServer = new BufferedReader(new InputStreamReader(TCP_clientSocket.getInputStream()));
        //String TCP_fromServerText = TCP_fromTCPServer.readLine();
        //System.out.println("Received from TCP_Server: " + TCP_fromServerText);
    }

    private static void TCPServer() throws IOException {
        //Establish Socket
        ServerSocket TCP_serverSocket = new ServerSocket(DHT_PORT);
        System.out.println("TCP_Server on port: " + DHT_PORT);

         while(true){
             //Establish Socket
             Socket TCP_serverConnectionSocket = TCP_serverSocket.accept();

             //In
             BufferedReader TCP_fromClient = new BufferedReader(new InputStreamReader(TCP_serverConnectionSocket.getInputStream()));
             String TCP_fromClientText = TCP_fromClient.readLine();
             System.out.println(TCP_fromClientText);

             String[] TCP_receiveArray = TCP_fromClientText.split("\\$");
             String TCP_receiveMessage = TCP_receiveArray[0];
             String[] TCP_receiveMessageArray = TCP_receiveMessage.split("~");

             //Out
             //DataOutputStream TCP_toClient = new DataOutputStream(TCP_serverConnectionSocket.getOutputStream());
             //TCP_toClient.writeBytes("\"" + TCP_fromClientText + "\" ACK!" + '\n');

             if(TCP_receiveMessageArray[0].equals("GET_IP")){
                 if(TCP_receiveMessageArray.length == 1){
                     TCPClient(TCP_receiveArray[0] + DHT_IP_Address + ":" + DHT_PORT + "?", TCP_receiveArray[1], Integer.valueOf(TCP_receiveArray[2].trim()));
                 } else {
                     String[] IPArray = TCP_receiveMessageArray[1].split("\\?");
                     if(IPArray.length == 1){ //TODO change to 3
                         UDPClient(TCP_receiveMessageArray[1], TCP_receiveArray[1], Integer.valueOf(TCP_receiveArray[2].trim()));
                     } else {
                         TCP_receiveArray[0] += DHT_IP_Address + ":" + DHT_PORT + "?";
                         TCPClient(TCP_receiveArray[0] + DHT_IP_Address + ":" + DHT_PORT + "?", TCP_receiveArray[1], Integer.valueOf(TCP_receiveArray[2].trim()));
                     }
                 }
             } else if(TCP_receiveMessageArray[0].equals("EXIT")){
                 if(TCP_receiveMessageArray.length == 1){
                    System.out.println("Completed removing records for client at: " + TCP_receiveArray[1] + ":" + Integer.valueOf(TCP_receiveArray[2].trim()));
                 } else {
                     remove(TCP_receiveMessageArray[1], TCP_receiveArray[1], Integer.valueOf(TCP_receiveArray[2].trim()));
                 }
             }

         }
    }

    /*Between DHT && P2P_Client*/
    private static void insert(String content, String IPAddress){ //P2P_Client inform_and_update()
        theTable.put(content, IPAddress);
    }

    private static void retrieve(String content, String destIPAddress, int destPort) throws IOException{ //P2P_Client query_for_content()
        if(theTable.containsKey(content)){
            UDPClient(theTable.get(content), destIPAddress, destPort);
        } else {
            UDPClient("404 content not found", destIPAddress , destPort);
        }
    }

    private static void remove(String content, String destIPAddress, int destPort) throws IOException { //P2P_Client exit()
        String out = "";

        //split into records, if exists, remove it
        ArrayList<String> records = new ArrayList<>(Arrays.asList(content.split("\\?"))); //split between records
        for(int recordNum = 0; recordNum < records.size(); recordNum++){ //split within records
            String[] record = records.get(recordNum).split(":");
            String fileName = record[0];
            String yourIP = record[2];

            if(theTable.containsKey(fileName)){
                theTable.remove(fileName);
                records.remove(recordNum);
                recordNum--;
            }
        }

        for(int i = 0; i < records.size(); i++){
            out += records.get(i) + "?";
        }

        forwardEntry(out, destIPAddress, destPort);
    }

    /*Between DHT && DHT*/
    private static void returnIP(String clientIPAddress, int clientPort) throws IOException { //return_DHT_IPs for P2P_Client init()

        TCPClient("GET_IP~", clientIPAddress, clientPort);
        //UDPClient("192.168.2.1:1111?192.168.2.2:2222?192.168.2.3:3333", clientIPAddress, clientPort);
    }

    private static void forwardEntry(String content, String clientIPAddress, int clientPort) throws IOException { //forwardRemovalRecords for P2P_Client remove()
        //call TCP Client to send data
        TCPClient("EXIT~" + content, clientIPAddress, clientPort);
        //if back to original DHT
        //remove()?
    }

    //TODO Error checking
    private static void init(){
        Scanner initScanner = new Scanner(System.in);
        System.out.println("Please enter the following, separated by spaces or new lines:\n" +
                "\"DHT_ID between 1-4\" \n\"DHT_IP_ADDRESS\" \n\"DHT_PORT\" \n\"SUCCESSOR_IP\" \n\"SUCCESSOR_PORT\"");

        DHT_ID = initScanner.nextInt();
        DHT_IP_Address = initScanner.next().trim();
        DHT_PORT = initScanner.nextInt();
        SUCCESSOR_IP = initScanner.next().trim();
        SUCCESSOR_PORT = initScanner.nextInt();

        initScanner.close();

        System.out.println("DHT_ID: " + DHT_ID);
        System.out.println("DHT_IP_Address: " + DHT_IP_Address);
        System.out.println("DHT_PORT: " + DHT_PORT);
        System.out.println("SUCCESSOR_IP: " + SUCCESSOR_IP);
        System.out.println("SUCCESSOR_PORT: " + SUCCESSOR_PORT);
    }

    /*public static void startTCPServer(String content, String destIP, int destPort) {
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
                    TCPServer();
                } catch (IOException e) {
                    System.out.println("Incorrect String content, String destIP, or int destPort.");
                    e.printStackTrace();
                }
            }
        }
        Thread TCPServerThread = new Thread(new InitTCPServer(content, destIP, destPort));
        TCPServerThread.start();
    }*/

    private static Runnable startTCPServer = new Runnable() {
        @Override
        public void run() {
            try {
                TCPServer();
            } catch (IOException e) {
                System.out.println("Incorrect String content, String destIP, or int destPort.");
                e.printStackTrace();
            }
        }
    };
}
