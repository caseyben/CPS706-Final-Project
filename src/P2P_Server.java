import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;
import java.util.TimeZone;

public class P2P_Server{
	//Status Code for 200 OK response
    final private static int OK = 200;
	//Status Code for 400 Bad Request response
	final private static int BAD_REQUEST = 400;
	//Status Code for 404 Not Found response
	final private static int NOT_FOUND = 404;
	//Status Code for 505 HTTP Version Not Supported Response
	final private static int HTTP_NOT_SUPPORTED = 505;

	private ArrayList<activeClient> clientList = new ArrayList<>();

	//Default port to open P2P_Server server sockets on
	final private static int DEFAULT_PORT = 25565;

	//Instance variables
	private ServerSocket mainSocket;
	private Thread mainThread;

	/**
	 * Constructor for P2P_Server class
	 */
	public P2P_Server(){
		try{
			mainSocket = new ServerSocket(DEFAULT_PORT);
			mainThread = new Thread(mainThreadProcess);
			mainThread.start();
		}catch(Exception e){
			System.out.println(e);
			//System.err.println("Port Not Available.");
		}//try
	}//constructor
	
	
	/**
	 * Static thread that runs whenever a client is instantiated
	 */
	private Runnable mainThreadProcess = new Runnable (){
		/**
		 * Override of run method for Runnable class
		 */
		public void run(){
			while(true){
				try{
				Socket connectionSocket = mainSocket.accept();
				DataInputStream in = new DataInputStream(connectionSocket.getInputStream());
				DataOutputStream out = new DataOutputStream(connectionSocket.getOutputStream());

				String response = in.readUTF();
				int portToPass = seekPort(DEFAULT_PORT+1);
				response = OK + " " + portToPass;
				out.writeUTF(response);

				clientList.add(new activeClient(portToPass));
				connectionSocket.close();
				}catch(Exception e){
					System.out.println("SERVER (main thread): " + e);
				}	
			}//while
		}//run
	};//mainThreadProcess

	/**
	 * Class to instantiate when connection with another P2P Client is established
	 */
	public class activeClient{
		private Thread activeThread;
		private ServerSocket activeSocket;

		/**
		 * Constructor for activeClient class
		 * @param activePort port to open socket on for communication between clients
		 */
		private activeClient(int activePort){
			try{
				activeSocket = new ServerSocket(activePort);
				activeThread = new Thread(activeThreadProcess);
				activeThread.start();
			}catch(Exception e){
				System.err.println("SERVER (active thread): " + e);
			}//try

		}//constructor

		/**
		 * Thread that runs whenever connection with another P2P Client is established
		 */
		Runnable activeThreadProcess = new Runnable(){
			public void run(){
				try{
					String requestCode, filename, connection, responseString;
					Socket socket = activeSocket.accept();
					DataInputStream in = new DataInputStream(socket.getInputStream());
					String current = in.readUTF();
					Scanner scanner = new Scanner(current);
					byte[] byteArray;

					requestCode = scanner.next();

					if(requestCode.contains("GET")){
						filename = scanner.next();
						connection = "Close";
						filename = filename.substring(1);
						File file = new File(filename);
						byteArray = new byte[(int)file.length()];
						try{//Server Responds 200 OK
							if(file.exists()){
								responseString = generateHTTPResponse(OK, connection, generateDate(null), generateDate(file), file.length());
								byteArray = responseString.getBytes(Charset.forName("UTF-8"));
								FileInputStream fileInput = new FileInputStream(file);
								byte[] fileByteArray = new byte[(int)file.length()];

								fileInput.read(fileByteArray);
								fileInput.close();
								
								byte[] combinedArray = new byte[byteArray.length + fileByteArray.length];
								System.arraycopy(byteArray, 0, combinedArray, 0, byteArray.length);
								System.arraycopy(fileByteArray, 0, combinedArray, byteArray.length, fileByteArray.length);
								byteArray = combinedArray;

							}else{//Server Responds 404 NOT_FOUND
								responseString = generateHTTPResponse(NOT_FOUND, connection, generateDate(null), "", 0);
								byteArray = responseString.getBytes(Charset.forName("UTF-8"));
							}//if
						}catch(Exception e){//Server Responds 400 BAD_REQUEST
							responseString = generateHTTPResponse(BAD_REQUEST, connection, generateDate(null), "", 0);
							byteArray = responseString.getBytes(Charset.forName("UTF-8"));
						}//try
						DataOutputStream dataOutput = new DataOutputStream(socket.getOutputStream());
						dataOutput.writeInt(byteArray.length);
						dataOutput.write(byteArray, 0, byteArray.length);
						//dataOutput.writeUTF("TEST");
						socket.close();
						activeSocket.close();
					}//if
				}catch(Exception e){
					System.err.println("Process Errored");
				}//try
			}//run
		};//activeThreadProcess

		/**
		 * Generates HTTP response to be sent to currently connected P2P Client
		 * @param statusCode HTTP Status code that dictates contents of HTTP Response message
		 * @param connectionStatus String to send that determines if connection should be maintained or closed
		 * @param date Current date on local machine
		 * @param lastModifiedDate Date that file to send was last modified
		 * @param lengthOfFile Length of file in long
		 */
		private String generateHTTPResponse(int statusCode, String connectionStatus, String date, String lastModifiedDate, long lengthOfFile){
			String message = "";
	
			if(statusCode == OK){
				message += "HTTP/1.1 " + statusCode + " OK\r\nConnection: " 
				+ connectionStatus + "\r\nDate: " + date + "\r\nLast-Modified: " 
				+ lastModifiedDate + "\r\nContent-Length: " + lengthOfFile + "\r\nContent-Type: image/jpeg\r\n";
			}else if(statusCode == BAD_REQUEST){
				message = "HTTP/1.1 " + statusCode + " Bad Request\r\nConnection " + connectionStatus + "\r\nDate: " + date + "\r\n";
			}else if(statusCode == NOT_FOUND){
				message = "HTTP/1.1 " + statusCode + " Not Found\r\nConnection " + connectionStatus + "\r\nDate: " + date + "\r\n";
			}else if(statusCode == HTTP_NOT_SUPPORTED){
				message = "HTTP/1.1 " + statusCode + "HTTP Version Not Supported\r\nConnection " + connectionStatus + "\r\nDate: " + date + "\r\n";
			}//if
			
			return message;
		}//generateHTTP

		/**
		 * If file object is not null, generate the last modified date of the file. Else, generate the current date on local machine.
		 * @param file file to generate last modified date
		 */
		private String generateDate(File file){
			Date date;
			if(file != null){
				date = new Date(file.lastModified());
			}else{
				date = new Date();
			}
  			SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
 			dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			return dateFormat.format(date) + " GMT";
		}//generateDate
	}//activeClient

	/**
	 * Finds an available port to open new Socket for client-client connection.
	 * @param basePort the port to increment when seeking an available port
	 */
	private int seekPort(int basePort){
		boolean portNotFound = true;

		while(portNotFound){
			try{
				ServerSocket attempt = new ServerSocket(basePort);
				portNotFound = false;
				attempt.close();
			}catch (Exception e){
				basePort++;
			}//try
		}//while

		return basePort;
	}//seekPort

}//P2P_Server
//HTTP Request

//GET /filename.jpeg HTTP/1.1\r\n
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