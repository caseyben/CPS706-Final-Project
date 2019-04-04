import java.net.*;
import java.io.*;
import java.util.*;

public class P2P_Server {
    final int OK = 200;
	final int BAD_REQUEST = 400;
	final int NOT_FOUND = 404;
	final int HTTP_NOT_SUPPORTED = 505;

	private int port = 20440;
	private ServerSocket socket = new ServerSocket(port);
	private Thread mainthread;

	public P2P_Server(){
		
	}
	

}//P2P_Server

//implement multithreads
//

//TODO class FileTransferThread{}
