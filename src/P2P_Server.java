import java.net.*;
import java.text.SimpleDateFormat;
import java.io.*;
import java.util.*;
import java.time.*;
import java.text.*;

public class P2P_Server{
    final private static int OK = 200;
	final private static int BAD_REQUEST = 400;
	final private static int NOT_FOUND = 404;
	final private static int HTTP_NOT_SUPPORTED = 505;

	private static int port = 20440;

	private ServerSocket mainSocket;
	private Thread mainThread;

	public P2P_Server(){
		try{
			mainSocket = new ServerSocket(port);
			mainThread = new Thread(mainThreadProcess);
			mainThread.start();
		}catch(IOException e){
			System.err.println("Port Not Available.");
		}//try
	}//constructor
	
	
	Runnable mainThreadProcess = new Runnable (){
		public void run(){
			while(true){
				Socket connectionSocket = mainSocket.accept();
				DataInputStream in = new DataInputStream(connectionSocket.getInputStream());
				DataOutputStream out = new DataOutputStream(connectionSocket.getOutputStream());

				String response = in.readUTF();

				response = OK + " " + port;
				connectionSocket.close();		
			}//while
		}//run
	};//mainThreadProcess

	public class activeClient{
		private Thread activeThread;
		private ServerSocket activeSocket;

		public activeClient(){
			try{
				activeSocket = new ServerSocket(port);
				activeThread = new Thread(activeThreadProcess);
				activeThread.start();
			}catch(IOException e){
				System.err.println("Port Not Available.");
			}//try

		}//constructor

		Runnable activeThreadProcess = new Runnable(){
			public void run(){
				try{
					String requestCode, filename, connection;
					Socket socket = activeSocket.accept();
					DataInputStream in = new DataInputStream(socket.getInputStream());
					String current = in.readUTF();
					Scanner scanner = new Scanner(current);

					requestCode = scanner.next();

					if(requestCode == "GET"){
						filename = scanner.next();
						connection = "Close";

						filename = filename.substring(1);
						File file = new File(filename);

						try{
							
						}catch(IOException e){

						}

					}//if
				}catch(IOException e){
					System.err.println("Process Errored");
				}//try
			}//run
		};//activeThreadProcess

		public String generateHTTPResponse(int statusCode, String connectionStatus, String date, String lastModifiedDate, int lengthOfFile){
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

		public String generateDate(){
			Date date = new Date();
  			SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
 			dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			return dateFormat.format(date) + " GMT";
		}//generateDate

		public String generateLastModified(File file){

			return "";
		}//generateLastModified
	}//activeClient

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