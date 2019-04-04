import java.net.*;
import java.io.*;
import java.util.*;

public class P2P_Client {
    private static final int OK = 200;
	private static final int BAD_REQUEST = 400;
	private static final int NOT_FOUND = 404;
	private static final int HTTP_NOT_SUPPORTED = 505;
    private static final int NUM_DHT_SERVERS = 4;

    public static void main(String[] args) throws Exception {
        //DHTPool.put("135.0.211.153", 25565); //arg0, arg1
        //String key = "fig3";
        //int hashedKey = hashKey(key);
        //System.out.println(hashedKey);
        Thread thread = new Thread(runnable);
        thread.start();

    }

	static Runnable runnable = new Runnable() {
        public void run(){
            Client client = new Client("135.0.211.153", 25565);
            Scanner scanner = new Scanner(System.in);
            String input;
            while(true){
                System.out.print("Enter input: ");
                input = scanner.next();
                if(input.equalsIgnoreCase("query")){
                    System.out.print("Enter file name:");
                    String file = scanner.next();
                    try{
                        client.query(file);
                    }
                    catch(Exception e){
                        System.out.println(e);
                    }
                }
                if(input.equalsIgnoreCase("upload")){
                    System.out.print("Enter file name:");
                    String fileName = scanner.next();
                    try{
                        File file = new File(fileName);
                    }
                    catch(Exception e){
                        System.out.println("Could not find file.");
                    }
                    
                }
                if(input.equalsIgnoreCase("help")){
                    System.out.print("upload - upload a file\nquery - query for content\nexit - exit program\n");
                }
                if(input.equalsIgnoreCase("exit")){
                    try{
                        client.exit();
                    }
                    catch(Exception e){
                        System.out.println(e);
                    }
                }
            }
        }
    };
    public static class Client{
        private static DatagramSocket UDPSocket;
        private static LinkedHashMap<String,Integer> DHTPool= new LinkedHashMap<String,Integer>();

        public Client(String IP, int port){
            DHTPool.put(IP, port);
            
            try{
               init();
            }
            catch (Exception e){
                System.out.println(e);
            }
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

        public void init() throws Exception{
            UDPSocket = new DatagramSocket();
            sendToDHT("FIND~balls", DHTPool.keySet().toArray()[0].toString(), (int) DHTPool.values().toArray()[0]);
            String resp = receiveFromDHT();
            System.out.println(resp);
            //fillDHTPool(resp);
            //System.out.println(DHTPool.toString());
        }

        public static String receiveFromDHT() throws Exception{
            byte[] receiveData = new byte[4096];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            UDPSocket.receive(receivePacket);
            return new String(receivePacket.getData());
        }

        public static void fillDHTPool(String message) throws Exception{
            String[] arr = message.split("_");
            for(int i = 0;i<arr.length;i++){
                String[] data = arr[i].split(":");
                DHTPool.put(data[0], Integer.valueOf(data[1].trim()));
            }
        }

        public void query(String file) throws Exception{
            int id = hashKey(file);
            sendToDHT("FIND~"+file,  DHTPool.keySet().toArray()[id].toString(), (int) DHTPool.values().toArray()[id]);
            String resp = receiveFromDHT();
            System.out.println(resp);
        }
        //TODO int DHTToContact <-- hashContent() for use to insert and retrieve

        //TODO inform_and_update(hashedContentToStore, ownIPAddress) -- send to DHT

        //TODO query_for_content(hashedContentWanted) -- send to DHT

        public void exit() throws Exception{
            sendToDHT("EXIT", DHTPool.keySet().toArray()[0].toString(), (int) DHTPool.values().toArray()[0]);//inform content
            System.exit(0);
        }
    }
}