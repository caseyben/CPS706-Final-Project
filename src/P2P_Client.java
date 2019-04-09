import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Scanner;

public class P2P_Client {

    // HTTP Code 200
    private static final int OK = 200;

    // HTTP Code 400
    private static final int BAD_REQUEST = 400;

    // HTTP Code 404
    private static final int NOT_FOUND = 404;

    // HTTP Code 505
    private static final int HTTP_NOT_SUPPORTED = 505;

    // The number of DHT servers
    private static final int NUM_DHT_SERVERS = 4;

    // The assigned port number
    private static final int DEFAULT_PORT = 20440;

    // The IP of the initial DHT connection
    private static String initDHTIP;

    // The port of the initial DHT connection
    private static int initDHTPort;

    public static void main(String[] args) {
        initDHTIP = args[0];
        initDHTPort = Integer.valueOf(args[1]);
        Thread thread = new Thread(runnable);
        thread.start();
    }

    /**
     * Runnable thread that creates a client and server object, listens for user input
     */
    private static Runnable runnable = new Runnable() {

        /**
         * Override of run method for Runnable class
         */
        public void run(){
            Client client = new Client(initDHTIP, initDHTPort);
            P2P_Server server = new P2P_Server();
            Scanner scanner = new Scanner(System.in);
            String[] input;
            while(true){
                System.out.print("Enter input: ");
                input = scanner.nextLine().split(" ");
                if(input[0].equalsIgnoreCase("query") && input.length>1){
                    String file = input[1];
                    try{
                        client.query(file);
                    }
                    catch(Exception e){
                        System.out.println(e);
                    }
                }
                else if(input[0].equalsIgnoreCase("insert") && input.length>1){
                    String fileName = input[1];
                    File file = new File(fileName);
                    if(file.exists()){
                        try{
                            client.insert(fileName);
                        }
                        catch(Exception e){
                            System.out.println("CLIENT: " + e);
                        }
                    }
                    else{
                        System.out.println("File does not exist");
                    }
                }
                else if(input[0].equalsIgnoreCase("help")){
                    System.out.print("insert fileName - insert a file\nquery fileName - query for content\nexit - exit program\n");
                }
                else if(input[0].equalsIgnoreCase("exit")){
                    try{
                        client.exit();
                    }
                    catch(Exception e){
                        System.out.println(e);
                    }
                }
                else{
                    System.out.println("Invalid input. Type \"help\" for options.");
                }
                System.out.println("-----------");
            }
        }
    };

    /**
     * Client class
     */
    private static class Client{

        // Initializes UDP socket for communicating with DHTs
        private static DatagramSocket UDPSocket;

        // Initializes DHTPool which stores the IPs and ports of current DHT
        private static LinkedHashMap<String,Integer> DHTPool= new LinkedHashMap<>();

        // Initializes localRecords which stores which files the client has stored
        private static ArrayList<String> localRecords = new ArrayList<>();

        /**
         * Client constructor, initiates client with DHT
         * @param IP IP of the DHT with ID=1
         * @param port Port of the DHT with ID=1
         */
        private Client(String IP, int port){
            DHTPool.put(IP, port);
            try{
               init();
            }
            catch (Exception e){
                System.out.println(e);
            }
        }

        /**
         * Sends a packet to a DHT
         * @param message message to send
         * @param IP the IP address to send packet to
         * @param port the port to send packet to
         */
        private static void sendToDHT(String message, String IP, int port) throws IOException {
            byte[] sendData;
            sendData = message.getBytes();
            InetAddress addr = InetAddress.getByName(IP);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, addr, port);
            UDPSocket.send(sendPacket);
        }

        /**
         * Takes in a file name and determines which DHT to send content to
         * @param content the file name
         * @return the id of the DHT
         */
        private static int hashKey(String content){
            int ASCIISum = 0;
            for(int i = 0; i < content.length(); i++){
                ASCIISum += (int) content.charAt(i);
            }
            return (ASCIISum % NUM_DHT_SERVERS) + 1;
        }

        /**
         * Initializes the client by gathering DHT IPs
         */
        private void init(){
            try{
                UDPSocket = new DatagramSocket();
                sendToDHT("GET_IP", DHTPool.keySet().toArray()[0].toString(), (int) DHTPool.values().toArray()[0]);
                String resp = receiveFromDHT();
                fillDHTPool(resp);
                System.out.println("Client successfully initiated. Type \"help\" for commands.");
            }
            catch(Exception e){
                System.out.println("Client could not be initiated.");
            }
        }

        /**
         * Receives a packet from the DHT
         * @return The message received
         */
        private static String receiveFromDHT() throws Exception{
            byte[] receiveData = new byte[4096];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            UDPSocket.receive(receivePacket);
            return new String(receivePacket.getData()).trim();
        }

        /**
         * Takes the message sent by the DHT and fills the DHTPool
         * @param message The message sent by the DHT containing the IPs for the remaining DHTs
         */
        private static void fillDHTPool(String message) {
            String[] arr = message.split("\\?");
            for (String anArr : arr) {
                String[] data = anArr.split(":");
                DHTPool.put(data[0], Integer.valueOf(data[1].trim()));
            }
        }

        /**
         * Queries for an image from DHT and sends GET request for that image
         */
        private void query(String file) throws Exception{
            int id = hashKey(file);
            sendToDHT("FIND~"+file,  DHTPool.keySet().toArray()[id-1].toString(), (int) DHTPool.values().toArray()[id-1]);
            String resp = receiveFromDHT().trim();
            int port = receiveP2PPort(resp);
            if(port != 0){
                exchangeHTTP(resp, port, file);
            }
            else{
                System.out.println("Error in retrieving file.");
            }
        }

        /**
         * Inserts local file into DHT records and local records
         * @param file The file name
         */
        private void insert(String file) throws Exception{
            int id = hashKey(file);
            sendToDHT("INSERT~"+file,  DHTPool.keySet().toArray()[id-1].toString(), (int) DHTPool.values().toArray()[id-1]);

            String resp = receiveFromDHT().trim();
            if(resp.contains("Successfully")){
                localRecords.add(file + ":" + id + ":" + DHTPool.keySet().toArray()[id-1].toString());
                System.out.println("File " + file + " successfully inserted into DHT with ID " + id);
            }
            else{
                System.out.println(resp);
            }
        }

        /**
         * Notifies DHT that client is exiting
         */
        private void exit() throws Exception{
            String records = "";
            for (String localRecord : localRecords) {
                records += localRecord + "?";
            }
            sendToDHT("EXIT~"+records, DHTPool.keySet().toArray()[0].toString(), (int) DHTPool.values().toArray()[0]);
            System.out.println("Successfully exited.");
            System.exit(0);
        }
    
        /**
         * Establishes a TCP connection and receives an available port
         * @param IP The IP address to start the TCP connection with
         * @return The available port
         */
        private static int receiveP2PPort(String IP){
            try{
                Socket socket = new Socket(IP, DEFAULT_PORT);
                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                
                output.writeUTF("OPEN_CONNECTION");

                String resp[] = input.readUTF().split(" ");

                socket.close();

                if(Integer.valueOf(resp[0])==OK){
                    return Integer.valueOf(resp[1]);
                }
            }catch(Exception e){
                System.out.println("CLIENT: " + e);
            }
            return 0;
        }

        /**
         * Sends an HTTP request and receives a response
         * @param IP The IP to open the TCP connection with
         * @param port The port to open the TCP connection with
         * @param file The file name to retrieve
         */
        private static void exchangeHTTP(String IP, int port, String file){
            try{

                Socket socket = new Socket(IP, port);

                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output = new DataOutputStream(socket.getOutputStream());

                output.writeUTF(generateHTTP(file, IP, "close"));

                int len = input.readInt();
                byte resp[] = new byte[len];
                input.readFully(resp);

                String responseString = new String(resp);
                Scanner scanner = new Scanner(responseString);
                int status = Integer.valueOf(scanner.nextLine().split(" ")[1]);
                if(status==OK){
                    System.out.println(OK + " OK: HTTP response received.");
                    while(scanner.hasNextLine()){
                        String line = scanner.nextLine();
                        if(line.contains("Content-Length")) {
                            int contentLength = Integer.valueOf(line.split(" ")[1]);
                            generateFile(resp, contentLength, file);
                            scanner.close();
                            break;
                        }
                    }
                }
                else if(status==BAD_REQUEST){
                    System.out.println(BAD_REQUEST + " Error: Bad request");
                }
                else if(status==NOT_FOUND){
                    System.out.println(NOT_FOUND + " Error: File not found.");
                }
                else if(status==HTTP_NOT_SUPPORTED){
                    System.out.println(HTTP_NOT_SUPPORTED + " Error: HTTP version not supported.");
                }
                socket.close();
            }
            catch(Exception e){
                System.out.println(NOT_FOUND + " Error: File not found.");
            }
        }

        /**
         * Generates an HTTP GET request
         * @param filename The name of the file to retrieve
         * @param hostname The name of the host
         * @param connectionStatus The connection status of the HTTP request
         * @return The HTTP request
         */
        private static String generateHTTP(String filename, String hostname, String connectionStatus){
            String http = "";
            http += "GET /" + filename + " HTTP/1.1\r\nHost: " + hostname + "\r\nConnection: " + connectionStatus + "\r\nAccept-language: en-us\r\n";
            return http;
        }
        
        /**
         * Generates a file from a byte array
         * @param resp The byte array to be made into a file
         * @param contentLength The length of the content contained in resp
         * @param file The name of the file
         */
        private static void generateFile(byte resp[], int contentLength, String file){
            int headerLength = resp.length-contentLength;
            byte data[] = new byte[resp.length-headerLength];
            for(int i = 0;i<contentLength;i++){
                data[i] = resp[i+headerLength];
            }
            try(FileOutputStream fos = new FileOutputStream(file)){
                fos.write(data);
                System.out.println("File successfully created.");
            }
            catch(Exception e){
                System.out.println("File could not be created.");
            }
        }
    }
}