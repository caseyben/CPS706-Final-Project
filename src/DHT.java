import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Scanner;

public class DHT {
    private static Hashtable<String, String> theTable = new Hashtable<>();

    private static int DHT_ID = 0;
    private static String DHT_IP_ADDRESS = "";
    private static int DHT_TCP_PORT = 0;
    private static String SUCCESSOR_IP = "";
    private static int SUCCESSOR_TCP_PORT = 0;
    private static DatagramSocket UDP_SOCKET;
    private static int UDP_PORT = 20440;
    static {
        try {
            UDP_SOCKET = new DatagramSocket(UDP_PORT);
        } catch (SocketException e) {
            System.out.println("Cannot create UDP socket with port: " + UDP_PORT);
        }
    }

    public static void main(String[] args) throws IOException {
        init(); //set ID:IP:Port of DHT & Successor IP:Port

        //Start TCP_Server
        Thread TCP_Server_Thread = new Thread(startTCPServer);
        TCP_Server_Thread.start();

        //Start UDP_Server
        UDPServer();
    }

    /**
     * To return data from retrieve() and returnIPs() back to P2P_Client
     * @param message the content to return
     * @param UDP_returnAddress the return IP address of the P2P_Client
     * @param UDP_returnPort the return port of the P2P_Client
     * @throws IOException if an I/O error occurs or unknown host
     */
    private static void UDPClient(String message, String UDP_returnAddress, int UDP_returnPort) throws IOException {
        //Prepare Message
        byte[] UDP_sendData;
        UDP_sendData = message.getBytes();

        InetAddress UDP_returnIPAddress = InetAddress.getByName(UDP_returnAddress);

        DatagramPacket sendPacket = new DatagramPacket(UDP_sendData, UDP_sendData.length, UDP_returnIPAddress, UDP_returnPort);

        UDP_SOCKET.send(sendPacket);
        System.out.println("Sent UDP packet containing: \"" + message + "\" to P2P_Client @ " + UDP_returnAddress + ":" + UDP_returnPort);
    }

    /**
     * Awaits UDP commands from P2P_Clients & parses them for processing
     * @throws IOException if an I/O error occurs
     */
    private static void UDPServer() throws IOException {
        System.out.println("Started UDP_Server running on port: " + UDP_PORT);

        byte[] UDP_receiveDataBuffer = new byte[4096];

        while(true){
            //In
            DatagramPacket UDP_receivePacket = new DatagramPacket(UDP_receiveDataBuffer, UDP_receiveDataBuffer.length);
            UDP_SOCKET.receive(UDP_receivePacket);
            String UDP_receiveString = new String(UDP_receivePacket.getData(), 0, UDP_receivePacket.getLength());

            //Out
            InetAddress UDP_returnAddress = UDP_receivePacket.getAddress();
            int UDP_returnPort = UDP_receivePacket.getPort();
            System.out.println("Received UDP packet containing: \"" + UDP_receiveString + "\" from P2P_Client @ " + UDP_returnAddress + ":" + UDP_returnPort);

            //Parsing data
            String[] UDP_receiveArray = UDP_receiveString.split("~");
            switch (UDP_receiveArray[0]) {
                case "GET_IP":
                    returnIPs(UDP_returnAddress.getHostAddress(), UDP_returnPort);
                    break;
                case "INSERT":
                    insert(UDP_receiveArray[1], UDP_returnAddress.getHostAddress(), UDP_returnPort);
                    break;
                case "FIND":
                    retrieve(UDP_receiveArray[1], UDP_returnAddress.getHostAddress(), UDP_returnPort);
                    break;
                case "EXIT":
                    if (UDP_receiveArray.length < 2) { //if no records given to remove
                        System.out.println("No records to clear from: " + UDP_returnAddress.getHostAddress() + ":" + UDP_returnPort);
                    } else {
                        remove(UDP_receiveArray[1], UDP_returnAddress.getHostAddress(), UDP_returnPort);
                    }
                    break;
            }
        }
    }

    /**
     * To forward queries to next DHT for processing (collect IPs returnIPs() & remove entries for P2P_Client exit())
     * @param content content to process (IPs or Entries) with header
     * @param UDP_returnAddress the return IP address of the P2P_Client
     * @param UDP_returnPort the return port of the P2P_Client
     * @throws IOException if an I/O error occurs or unknown host
     */
    private static void TCPClient(String content, String UDP_returnAddress, int UDP_returnPort) throws IOException {
        //Establish Socket
        Socket TCP_clientSocket = new Socket(SUCCESSOR_IP, SUCCESSOR_TCP_PORT);

        String[] TCP_sendArray = content.split("~");

        //Out
        DataOutputStream TCP_toServer = new DataOutputStream(TCP_clientSocket.getOutputStream()); //Stream to successor DHT

        String toServerString = content + "$" + UDP_returnAddress + "$" + UDP_returnPort + " " + '\n'; //Will break without the \n, do not ask why.
        System.out.println("Sending TCP packet containing: \"" + content  + "\" to DHT @ " + SUCCESSOR_IP + ":" + SUCCESSOR_TCP_PORT);

        if(TCP_sendArray[0].equals("GET_IP")){
            TCP_toServer.writeBytes(toServerString);
        } else if(TCP_sendArray[0].equals("EXIT")){
            TCP_toServer.writeBytes(toServerString);
        }
    }

    /**
     * Awaits TCP commands from previous DHT & parses them for processing
     * @throws IOException if an I/O error occurs
     */
    private static void TCPServer() throws IOException {
        //Establish Socket
        ServerSocket TCP_serverSocket = new ServerSocket(DHT_TCP_PORT);
        System.out.println("Started TCP_Server running on port: " + DHT_TCP_PORT);

         while(true){
             //Establish Socket
             Socket TCP_serverConnectionSocket = TCP_serverSocket.accept();

             //In
             BufferedReader TCP_fromClient = new BufferedReader(new InputStreamReader(TCP_serverConnectionSocket.getInputStream()));
             String TCP_fromClientText = TCP_fromClient.readLine();
             System.out.println("Received TCP packet containing: \"" + TCP_fromClientText + "\"");

             //Parse content
             String[] TCP_receiveArray = TCP_fromClientText.split("\\$");
             String TCP_receiveMessage = TCP_receiveArray[0];
             String[] TCP_receiveMessageArray = TCP_receiveMessage.split("~");

             //Out
             if(TCP_receiveMessageArray[0].equals("GET_IP")){ //Collecting IPs
                 if(TCP_receiveMessageArray.length == 1){ //no records exist yet, add record and forward
                     TCPClient(TCP_receiveArray[0] + DHT_IP_ADDRESS + ":" + UDP_PORT + "?", TCP_receiveArray[1], Integer.valueOf(TCP_receiveArray[2].trim()));
                 } else {
                     String[] IPArray = TCP_receiveMessageArray[1].split("\\?"); //split records to count if enough DHT_IPs collected to return
                     if(IPArray.length == 3){ //done collecting IPs, return records to P2P_Client
                         UDPClient(TCP_receiveMessageArray[1], TCP_receiveArray[1], Integer.valueOf(TCP_receiveArray[2].trim()));
                     } else { //add record and forward
                         TCPClient(TCP_receiveArray[0] + DHT_IP_ADDRESS + ":" + UDP_PORT + "?", TCP_receiveArray[1], Integer.valueOf(TCP_receiveArray[2].trim()));
                     }
                 }
             } else if(TCP_receiveMessageArray[0].equals("EXIT")){ //removing entries
                 if(TCP_receiveMessageArray.length == 1){ //no more entries in tables
                    System.out.println("Completed removing records for P2P_Client @ " + TCP_receiveArray[1] + ":" + Integer.valueOf(TCP_receiveArray[2].trim()));
                 } else { //look for record in current DHT for removal
                     remove(TCP_receiveMessageArray[1], TCP_receiveArray[1], Integer.valueOf(TCP_receiveArray[2].trim()));
                 }
             }
         }
    }

    /*Between DHT && P2P_Client*/

    /**
     * Insert a (Content, IPAddress) key, value pair into DHT from a P2P_Client inform_and_update()
     * @param content key for theTable
     * @param UDP_returnAddress value for theTable/the return IP address of the P2P_Client
     * @param UDP_returnPort the return port of the P2P_Client
     * @throws IOException if an I/O error occurs
     */
    private static void insert(String content, String UDP_returnAddress, int UDP_returnPort) throws IOException {
		if(theTable.containsKey(content)){
			UDPClient("File already exists.", UDP_returnAddress, UDP_returnPort);
		} else {
			theTable.put(content, UDP_returnAddress);
			System.out.println("Added record (K, V): (" + content + ", " + UDP_returnAddress + ")");
            UDPClient("Successfully inserted file.", UDP_returnAddress, UDP_returnPort);
		}
    }

    /**
     * Looks in current DHT for (Content, IPAddress) key, value pair, return to P2P_Client if it exists. If it does not exist, return error message.
     * Matches with P2P_Client query_for_content().
     * @param content content P2P_Client is querying for
     * @param UDP_returnAddress the return IP address of the P2P_Client
     * @param UDP_returnPort the return port of the P2P_Client
     * @throws IOException if an I/O error occurs
     */
    private static void retrieve(String content, String UDP_returnAddress, int UDP_returnPort) throws IOException {
        if(theTable.containsKey(content)){
            UDPClient(theTable.get(content), UDP_returnAddress, UDP_returnPort);
        } else {
            UDPClient("404 content not found", UDP_returnAddress , UDP_returnPort);
        }
    }

    /**
     * Removes requested entries from a P2P_Client. If entry exists in current DHT, remove from theTable, then forward entries to succeeding DHT.
     * Matches with P2P_Client exit().
     * @param content content that was requested to be removed
     * @param UDP_returnAddress IP Address associated with record
     * @param UDP_returnPort Port associated with P2P_Client
     * @throws IOException if an I/O error occurs
     */
    private static void remove(String content, String UDP_returnAddress, int UDP_returnPort) throws IOException {
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
                System.out.println("Removed record (K, V): (" + fileName + ", " + yourIP + ")");
            }
        }

        for(String record : records) {
            out += record + "?";
        }

        TCPClient("EXIT~" + out, UDP_returnAddress, UDP_returnPort); //add EXIT~ header to content, then forward to successor DHT
    }

    /**
     * Returns IP address of all DHTs to requesting P2P_Client.
     * Matches with P2P_Client init()
     * @param UDP_returnAddress the return IP address of the P2P_Client
     * @param UDP_returnPort the return port of the P2P_Client
     * @throws IOException if an I/O error occurs
     */
    private static void returnIPs(String UDP_returnAddress, int UDP_returnPort) throws IOException {
        TCPClient("GET_IP~", UDP_returnAddress, UDP_returnPort); //add GET_IP~ header to content, then forward to successor DHT
    }

    /**
     * Initialises the DHT with the following parameters, entered from the console
     * DHT_ID
     * DHT_IP_ADDRESS
     * DHT_TCP_PORT
     * SUCCESSOR_IP
     * SUCCESSOR_TCP_PORT
     */
    private static void init(){
        Scanner initScanner = new Scanner(System.in);
        System.out.println("Please enter the following, separated by spaces or new lines:\n" +
                "\"DHT_ID between 1-4\" \n\"DHT_IP_ADDRESS\" \n\"DHT_TCP_PORT\" \n\"SUCCESSOR_IP\" \n\"SUCCESSOR_TCP_PORT\"");

        DHT_ID = initScanner.nextInt();
        DHT_IP_ADDRESS = initScanner.next().trim();
        DHT_TCP_PORT = initScanner.nextInt();
        SUCCESSOR_IP = initScanner.next().trim();
        SUCCESSOR_TCP_PORT = initScanner.nextInt();

        initScanner.close();

        System.out.println("DHT_ID: " + DHT_ID);
        System.out.println("DHT_IP_ADDRESS: " + DHT_IP_ADDRESS);
        System.out.println("DHT_TCP_PORT: " + DHT_TCP_PORT);
        System.out.println("SUCCESSOR_IP: " + SUCCESSOR_IP);
        System.out.println("SUCCESSOR_TCP_PORT: " + SUCCESSOR_TCP_PORT);
    }

    /**
     * TCPServer Thread
     */
    private static Runnable startTCPServer = new Runnable() {
        @Override
        public void run() {
            try {
                TCPServer();
            } catch (IOException e) {
                System.out.println("Unable to start TCP_Server: cannot bind on given IP address and/or port");
                e.printStackTrace();
            }
        }
    };
}