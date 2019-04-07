import java.net.*;
import java.text.SimpleDateFormat;
import java.io.*;
import java.util.*;
import java.time.*;
import java.text.*;
import java.nio.charset.Charset;

public class P2P_Server{
    final private static int OK = 200;
	final private static int BAD_REQUEST = 400;
	final private static int NOT_FOUND = 404;
	final private static int HTTP_NOT_SUPPORTED = 505;

	private ArrayList<activeClient> clientList = new ArrayList<activeClient>();

	private static int port = 25565;

	private ServerSocket mainSocket;
	private Thread mainThread;

	public P2P_Server(){
		try{
			mainSocket = new ServerSocket(port);
			mainThread = new Thread(mainThreadProcess);
			mainThread.start();
		}catch(Exception e){
			System.out.println(e);
			//System.err.println("Port Not Available.");
		}//try
	}//constructor
	
	
	Runnable mainThreadProcess = new Runnable (){
		public void run(){
			while(true){
				try{
				Socket connectionSocket = mainSocket.accept();
				DataInputStream in = new DataInputStream(connectionSocket.getInputStream());
				DataOutputStream out = new DataOutputStream(connectionSocket.getOutputStream());

				String response = in.readUTF();
				response = OK + " " + port;
				out.writeUTF(response);

				clientList.add(new activeClient());
				connectionSocket.close();
				}catch(Exception e){
					System.out.println("SERVER: " + e);
				}	
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
			}catch(Exception e){
				System.err.println("SERVER: " + e);
			}//try

		}//constructor

		Runnable activeThreadProcess = new Runnable(){
			public void run(){
				try{
					String requestCode, filename, connection, responseString;
					double fileLength;
					Socket socket = activeSocket.accept();
					DataInputStream in = new DataInputStream(socket.getInputStream());
					String current = in.readUTF();
					Scanner scanner = new Scanner(current);
					byte[] byteArray;

					requestCode = scanner.next();

					if(requestCode == "GET"){
						filename = scanner.next();
						connection = "Close";
						filename = filename.substring(1);
						File file = new File(filename);
						byteArray = new byte[(int)file.length()];
						try{//Server Responds 200 OK
							if(file.exists()){
								responseString = generateHTTPResponse(OK, connection, generateDate(null), generateDate(file), (double)file.length());
								byteArray = responseString.getBytes(Charset.forName("UTF-8"));
								FileInputStream fileInput = new FileInputStream(file);
								byte[] fileByteArray = new byte[(int)file.length()];

								fileInput.read(fileByteArray);
								fileInput.close();
								
								byte[] combinedArray = new byte[byteArray.length + fileByteArray.length];
								System.arraycopy(byteArray, 0, combinedArray, 0, byteArray.length);
								System.arraycopy(fileByteArray, 0, combinedArray, byteArray.length, fileByteArray.length);

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
						socket.close();
						activeSocket.close();
					}//if
				}catch(Exception e){
					System.err.println("Process Errored");
				}//try
			}//run
		};//activeThreadProcess

		public String generateHTTPResponse(int statusCode, String connectionStatus, String date, String lastModifiedDate, double lengthOfFile) throws Exception{
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

		public String generateDate(File file) throws Exception{
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