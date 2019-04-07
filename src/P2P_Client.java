import java.net.*;
import java.io.*;
import java.util.*;


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
    private static final int NUM_DHT_SERVERS = 2;

    // The assigned port number
    private static final int port = 25565;

    // The IP of the initial DHT connection
    private static String initDHTIP;

    // The port of the initial DHT connection
    private static int initDHTPort;

    public static void main(String[] args) throws Exception {
        //initDHTIP = args[0];
        //initDHTPort = Integer.valueOf(args[1]);
        Thread thread = new Thread(runnable);
        thread.start();
    }

	static Runnable runnable = new Runnable() {
        public void run(){
            Client client = new Client("135.0.211.153",25565);//(initDHTIP, initDHTPort); //135.0.211.153 25565
            P2P_Server server = new P2P_Server();
            Scanner scanner = new Scanner(System.in);
            String[] input;
            while(true){
                System.out.print("Enter input: ");
                input = scanner.nextLine().split(" ");
                if(input[0].equalsIgnoreCase("query") && input.length>1){
                    String file = input[1];
                   // System.out.print("Enter file name: ");
                    //String file = scanner.next();
                    try{
                        client.query(file);
                    }
                    catch(Exception e){
                        System.out.println(e);
                    }
                }
                else if(input[0].equalsIgnoreCase("insert") && input.length>1){
                    //System.out.print("Enter file name: ");
                    //String fileName = scanner.next();
                    String fileName = input[1];
                    File file = new File(fileName);
                    if(file.exists()){
                        try{
                            client.insert(fileName);
                        }
                        catch(Exception e){
                            System.out.println(e);
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
    public static class Client{

        // Initializes UDP socket for communicating with DHTs
        private static DatagramSocket UDPSocket;

        // Initializes DHTPool which stores the IPs and ports of current DHT
        private static LinkedHashMap<String,Integer> DHTPool= new LinkedHashMap<String,Integer>();

        // Initializes localRecords which stores which files the client has stored
        private static ArrayList<String> localRecords = new ArrayList<String>();

        public Client(String IP, int port){
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
        public static void sendToDHT(String message, String IP, int port) throws IOException {
            byte[] sendData = new byte[4096];
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
        public static int hashKey(String content){
            int ASCIISum = 0;
            for(int i = 0; i < content.length(); i++){
                ASCIISum += (int) content.charAt(i);
            }
            return (ASCIISum % NUM_DHT_SERVERS) + 1;
        }

        /**
         * Initializes the client by gathering DHT IPs
         */
        public void init() throws Exception{
            UDPSocket = new DatagramSocket();
            sendToDHT("GET_IP", DHTPool.keySet().toArray()[0].toString(), (int) DHTPool.values().toArray()[0]);
            String resp = receiveFromDHT();
            System.out.println(resp);
            fillDHTPool(resp);
            System.out.println(DHTPool.toString());
        }

        /**
         * Receives a packet from the DHT
         * @return The message received
         */
        public static String receiveFromDHT() throws Exception{
            byte[] receiveData = new byte[4096];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            UDPSocket.receive(receivePacket);
            return new String(receivePacket.getData()).trim();
        }

        /**
         * Takes the message sent by the DHT and fills the DHTPool
         * @param message The message sent by the DHT containing the IPs for the remaining DHTs
         */
        public static void fillDHTPool(String message) throws Exception{
            String[] arr = message.split("\\?");
            for(int i = 0;i<arr.length;i++){
                String[] data = arr[i].split(":");
                DHTPool.put(data[0], Integer.valueOf(data[1].trim()));
            }
        }

        /**
         * Queries for an image from DHT and sends GET request for that image
         */
        public void query(String file) throws Exception{
            int id = hashKey(file);
            sendToDHT("FIND~"+file,  DHTPool.keySet().toArray()[0].toString(), (int) DHTPool.values().toArray()[0]);//id-1
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
        public void insert(String file) throws Exception{
            int id = hashKey(file);
            sendToDHT("INSERT~"+file,  DHTPool.keySet().toArray()[0].toString(), (int) DHTPool.values().toArray()[0]);//id-1

            localRecords.add(file + ":" + id + ":" + DHTPool.keySet().toArray()[0].toString());//id-1
            System.out.println(localRecords.toString());
            //String resp = receiveFromDHT().trim();
            //System.out.println(resp);
        }

        /**
         * Notifies DHT that client is exiting
         */
        public void exit() throws Exception{
            String records = "";
            for(int i = 0;i<localRecords.size();i++){
                records+=localRecords.get(i) + "?";
            }
            sendToDHT("EXIT~"+records, DHTPool.keySet().toArray()[0].toString(), (int) DHTPool.values().toArray()[0]);
            System.exit(0);
        }
    
        public static int receiveP2PPort(String IP){
            try{
                Socket socket = new Socket(IP, port);
                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                

                output.writeUTF("Open");

                String resp[] = input.readUTF().split(" ");

                socket.close();

                if(Integer.valueOf(resp[0])==OK){
                    return Integer.valueOf(resp[1]);
                    //sendHTTP(IP, Integer.valueOf(resp[1]), file);
                }
            }catch(Exception e){
                System.out.println("CLIENT: " + e);
                //System.err.println("Cannot connect to server.");
            }
            return 0;
        }//P2PServerConnect

        public static void exchangeHTTP(String IP, int port, String file){
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
                    while(scanner.hasNextLine()){
                        String line = scanner.nextLine();
                        if(line.contains("Content-Length")){
                            int contentLength = Integer.valueOf(scanner.nextLine().split(" ")[1]);
                            generateFile(resp,contentLength,file);
                            break;
                        }
                        else{
                            continue;
                        }
                    }
                }
                socket.close();
            }
            catch(Exception e){
                System.out.println("CLIENT: " + e);
            }
        }

        public static String generateHTTP(String filename, String hostname, String connectionStatus){
            String http = "";
            http += "GET /" + filename + " HTTP/1.1\r\nHost: " + hostname + "\r\nConnection: " + connectionStatus + "\r\nAccept-language: en-us\r\n";
            return http;
        }//generateHTTP
        
        public static void generateFile(byte resp[], int contentLength, String file) throws Exception{
            int headerLength = resp.length-contentLength;
            byte data[] = new byte[resp.length-headerLength];
            for(int i = 0;i<contentLength;i++){
                data[i] = resp[i+headerLength];
            }
            
            try(FileOutputStream fos = new FileOutputStream(file)){
                fos.write(data);
            }
        }
    }
}

//HTTP Request

//GET /filename.txt HTTP/1.1\r\n
//Host: hostname\r\n
//Connection: status\r\n
//Accept-language: en-us\r\n

//HTTP Response

//HTTP/1.1 StatusCode\r\n
//Connection: status\r\n
//Date: currentDate\r\n
//Last-Modified: last file modified\r\n
//Content-Length: length\r\n
//Content-Type: image/jpeg\r\n
