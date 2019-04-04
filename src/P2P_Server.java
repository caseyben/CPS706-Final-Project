import java.net.*;
import java.io.*;
import java.util.*;

//C:\Users\brian\Documents\ff.txt 
public class P2P_Server{
  final static private int OK = 200;
  final static private int BAD_REQUEST = 400;
  final static private int NOT_FOUND = 404;
  final static private int HTTP_NOT_SUPPORTED = 505;
  
  private int port = 20440; 
  private ServerSocket socket, activeSocket;
  private Thread mainThread, activeThread;
  
  private int activeConnections = 0;
  
  public P2P_Server(){
    System.out.println("Initialization");
    try{
      socket = new ServerSocket(port);
      mainThread = new Thread(staticThread);
      mainThread.start();
    }catch(IOException e){
      System.err.println("Port Not Available.");
    }
  }//constructor
  
  Runnable staticThread = new Runnable(){
    public void run(){
      System.out.println("Main Thread is running");
      Socket connectionSocket = null;
      while(true){
        System.out.println("while");
        try {
          connectionSocket = socket.accept();
          DataInputStream in = new DataInputStream(connectionSocket.getInputStream());
          DataOutputStream out = new DataOutputStream(connectionSocket.getOutputStream());
          activeThread = new Thread(activeTCPThread);
          activeThread.start();
          connectionSocket.close(); 
        } catch (Exception e) {
          //TODO: handle exception
        }
        
      }//while
    }//run
  };
  
  Runnable activeTCPThread = new Runnable(){
    public void run(){
      System.out.println("Connection Thread is running");
      try{
        Socket activeConnectionSocket = socket.accept();
        DataInputStream activeIn = new DataInputStream(activeConnectionSocket.getInputStream());
        DataOutputStream activeOut = new DataOutputStream(activeConnectionSocket.getOutputStream());
        
        
        //If 200 OK
        FileInputStream fileReader = null;
        String currentString = "";
        currentString = activeIn.readUTF();
        
        
      }catch(IOException e){
        System.err.println("Error caught");
      }//try
    }
  };
  
  
}//P2P_Server

//implement multithreads

//TODO class FileTransferThread{}
