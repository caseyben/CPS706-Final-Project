import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class DHT {
    private static final int NUM_DHT_SERVERS = 4;
    private static Hashtable<String, String> theTable = new Hashtable<>();

    public static int DHT_ID = 0;
    public static String DHT_IP_Address = "";
    public static int DHT_PORT = 0;
    public static String SUCCESSOR_IP = "";
    public static int SUCCESSOR_PORT = 0;
    public static DatagramSocket UDP_SOCKET;
    public static int UDP_PORT = 25565;
    static {
        try {
            UDP_SOCKET = new DatagramSocket(UDP_PORT); //Static?
        } catch (SocketException e) {
            System.out.println("Cannot create UDP port: " + UDP_PORT);
        }
    }

    public static void main(String[] args) throws IOException {
        //TODO Hashtable(K, V) = (Name of file wanted, IP address of client with it)

        init();
        String key = "kimi no na wa";
        //System.out.println(hashKey(key));
        startTCPServer("", "", 0);
        //startTCPServer("", "", 25566);
        //startTCPServer("", "", 25567);
        //startTCPServer("", "", 25568);

        //UDPServer();

        //TCPServer(25565);
        //TCPServer(25566);
        //TCPServer(25567);
        //TCPServer(25568);

        TCPClient("", "", 25565);
        //TCPClient("", "", 25566);
        //TCPClient("", "", 25567);
        //TCPClient("", "", 25568);
    }

    public static void UDPClient(String message, String destIP, int destPort) throws IOException {
        //DatagramSocket UDP_ClientSocket = new DatagramSocket(25565);
        //Prepare Message
        byte[] UDP_sendData = new byte[4096];
        UDP_sendData = message.getBytes();

        InetAddress UDP_returnIPAddress = InetAddress.getByName(destIP);

        DatagramPacket sendPacket = new DatagramPacket(UDP_sendData, UDP_sendData.length, UDP_returnIPAddress, destPort);

        UDP_SOCKET.send(sendPacket);
        //TimeUnit.SECONDS.sleep(1);
        //UDP_SOCKET.close();
        //UDPServer(25565);
    }

    public static void UDPServer() throws IOException { //UDP from Client --> Server
        ;
        //DatagramSocket UDP_ServerSocket = new DatagramSocket(port);
        System.out.println("UDP_Server on port: " + UDP_PORT);

        //Data Buffers
        byte[] UDP_receiveDataBuffer = new byte[4096];
        byte[] UDP_returnDataBuffer = new byte[4096];

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

            if(UDP_receiveArray[0].equals("FIND")){
                System.out.println("find");
                //UDP_SOCKET.close();
                retreive(UDP_receiveArray[1], UDP_returnAddress.getHostAddress(), UDP_returnPort);
                //break;
            } else if (UDP_receiveArray[0].equals("INSERT")){
                System.out.println("insert");
                insert(UDP_receiveArray[1], UDP_returnAddress.getHostAddress());
            } else if (UDP_receiveArray[0].equals("GET_IP")) {
                System.out.println("init");
                //UDP_SOCKET.close();
                returnIP(UDP_returnAddress.getHostAddress(), UDP_returnPort);
                //break;
            } else if (UDP_receiveArray[0].equals("EXIT")){
                System.out.println("exit");
                //UDP_SOCKET.close();
                if(UDP_receiveArray.length < 2){
                    System.out.println("No records received");
                } else {
                    remove(UDP_receiveArray[1], UDP_returnAddress.getHostAddress(), UDP_returnPort);
                }
                //break;
            }

            //String UDP_returnString = "bitch fuck titty"; //"192.168.2.1:1111_192.168.2.2:2222_192.168.2.3:3333"; //string + " acknowledged from: " + returnAddress;
            //UDP_returnDataBuffer = UDP_returnString.getBytes();

            //DatagramPacket UDP_returnPacket = new DatagramPacket(UDP_returnDataBuffer, UDP_returnDataBuffer.length, UDP_returnAddress, UDP_returnPort);
            //UDP_ServerSocket.send(UDP_returnPacket);
        }
    }

    public static void TCPClient(String content, String IPAddress, int port) throws IOException {
        //Establish Socket
        Socket TCP_clientSocket = new Socket("192.168.0.151", SUCCESSOR_PORT); //TODO change to DHT_SUCCESSOR_IP
        System.out.println("TCP_Client on port: " + SUCCESSOR_PORT);

        String[] TCP_sendArray = content.split("~");


        //Out
        DataOutputStream TCP_toServer = new DataOutputStream(TCP_clientSocket.getOutputStream());
        TCP_toServer.writeBytes("This is TCP_client on port: " + DHT_PORT + " " + '\n');

        String toServerString = content + "$" + IPAddress + "$" + port;

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

    public static void TCPServer(String content, String IPAddress, int port) throws IOException {
        //Establish Socket
        ServerSocket TCP_serverSocket = new ServerSocket(DHT_PORT);
        System.out.println("TCP_Server on port: " + DHT_PORT);
        //System.out.println(TCP_serverSocket.getInetAddress().getHostAddress());

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
             DataOutputStream TCP_toClient = new DataOutputStream(TCP_serverConnectionSocket.getOutputStream());
             TCP_toClient.writeBytes("\"" + TCP_fromClientText + "\" ACK!" + '\n');
             if(TCP_receiveMessageArray[0].equals("GET_IP")){
                 if(TCP_receiveMessageArray.length == 1){
                     TCPClient(TCP_receiveArray[0] + DHT_IP_Address + ":" + DHT_PORT + "?", TCP_receiveArray[1], Integer.valueOf(TCP_receiveArray[2]));
                 } else {
                     String[] IPArray = TCP_receiveMessageArray[1].split("\\?");
                     if(IPArray.length == 3){
                         UDPClient(TCP_receiveArray[0], TCP_receiveArray[1], Integer.valueOf(TCP_receiveArray[2]));
                     } else {
                         TCP_receiveArray[0] += DHT_IP_Address + ":" + DHT_PORT + "?";
                         TCPClient(TCP_receiveArray[0] + DHT_IP_Address + ":" + DHT_PORT + "?", TCP_receiveArray[1], Integer.valueOf(TCP_receiveArray[2]));
                     }
                 }


             } else if(TCP_receiveMessageArray[0].equals("EXIT")){
                 if(TCP_receiveMessageArray.length == 1){
                    System.out.println("Completed removing records for client at: " + TCP_receiveArray[1] + ":" + Integer.valueOf(TCP_receiveArray[2]));
                 } else {
                     remove(TCP_receiveMessageArray[1], TCP_receiveArray[1], Integer.valueOf(TCP_receiveArray[2]));
                 }
             }






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

    /*Between DHT && P2P_Client*/
    public static void insert(String content, String IPAddress){ //P2P_Client inform_and_update()
        theTable.put(content, IPAddress);
    }

    public static void retreive(String content, String destIPAddress, int destPort) throws IOException{ //P2P_Client query_for_content()
        if(theTable.containsKey(content)){
            UDPClient(theTable.get(content), destIPAddress, destPort);
        } else {
            UDPClient("404 content not found", destIPAddress , destPort);
        }
    }

    public static void remove(String content, String destIPAddress, int destPort) throws IOException { //P2P_Client exit()
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

        //records.remove(0);

        for(int i = 0; i < records.size(); i++){
            out += records.get(i) + "?";
        }


        forwardEntry(out, destIPAddress, destPort);
    }

    /*Between DHT && DHT*/
    //TODO return_DHT_IPs for P2P_Client init()
    public static void returnIP(String clientIPAddress, int clientPort) throws IOException{
        //call TCP Client to send data

        TCPClient("GET_IP~", clientIPAddress, clientPort);
        //UDPClient("192.168.2.1:1111?192.168.2.2:2222?192.168.2.3:3333", clientIPAddress, clientPort);
    }

    //TODO forwardRemovalRecords for P2P_Client remove()
    public static void forwardEntry(String content, String clientIPAddress, int clientPort) throws IOException {
        //call TCP Client to send data
        TCPClient("EXIT~" + content, clientIPAddress, clientPort);
        //if back to original DHT
        //remove()?
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

        System.out.println("Please enter SUCCESSOR_PORT: ");
        SUCCESSOR_PORT = initScanner.nextInt();

        initScanner.close();

        System.out.println("DHT_ID: " + DHT_ID);
        System.out.println("DHT_IP_Address: " + DHT_IP_Address);
        System.out.println("DHT_PORT: " + DHT_PORT);
        System.out.println("SUCCESSOR_IP: " + SUCCESSOR_IP);
        System.out.println("SUCCESSOR_PORT: " + SUCCESSOR_PORT);
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
                    e.printStackTrace();
                }
            }
        }
        Thread TCPServerThread = new Thread(new InitTCPServer(content, destIP, destPort));
        TCPServerThread.start();
    }


}
