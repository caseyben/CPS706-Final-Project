import java.net.*;
import java.io.*;
import java.util.*;

public class P2P_Server{
    final private int OK = 200;
	final private int BAD_REQUEST = 400;
	final private int NOT_FOUND = 404;
	final private int HTTP_NOT_SUPPORTED = 505;

	private int port = 20440;
	private ServerSocket socket;
	private Thread mainThread;

	public P2P_Server(){
		try{
			socket = new ServerSocket(port);
			mainThread = new Thread(mainThreadProcess);
			mainThread.start();
		}catch(IOException e){
			System.err.println("Port Not Available.");
		}
	}//constructor
	
	
	Runnable mainThreadProcess = new Runnable (){
		public void run(){
			while(true){
				Socket connectionSocket = socket.accept();
				DataInputStream in = new DataInputStream(connectionSocket.getInputStream());
				DataOutputStream out = new DataOutputStream(connectionSocket.getOutputStream());

				String response = in.readUTF();

				response = OK + " " + port;
				connectionSocket.close();		
			}//while
		}//run
	};


}//P2P_Server

//implement multithreads
//

//TODO class FileTransferThread{}
