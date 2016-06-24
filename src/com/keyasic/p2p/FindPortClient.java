package com.keyasic.p2p;

import java.io.*;
import java.net.*;

public class FindPortClient {
	
	public static final int FINDPORTSERVER_PORT = 5000 ;
	private Socket clientSocket = null ; 
	BufferedOutputStream outToServer = null ;

	/**
	 * FindPortClient constructor:
	 *  1. instantiate clientSocket to turnserver
	 *  2. attach bufferoutputstream to clientSocket for I/O
	 * 
	 * @param server_ip
	 * @param server_port
	 */
	public FindPortClient(String server_ip, int server_port) {

		try {
			clientSocket = new Socket();
			InetSocketAddress isa = new InetSocketAddress(server_ip, server_port);
			clientSocket.connect(isa, 10000);
			outToServer = new BufferedOutputStream(clientSocket.getOutputStream());
		} catch(Exception ctor_ex) {
			System.out.printf("instantiate findPortClient failed: %s\n", ctor_ex.getMessage());
			//System.exit(-1);
		}
		
	}
	/**
	 * connect to turnserver's CLI to get the mapped port via UID.
	 *  
	 * @return mapped port # .
	 *            -1 : failed
	 *         
	 */
	public int getPort(String UID) {
		int client_port = -1 ;

		try {
			//BufferedReader inFromUser = new BufferedReader( new InputStreamReader(System.in));
			outToServer.write(UID.getBytes(), 0, 32);
			outToServer.flush();
			System.out.println("Sent UID to FindPortServer: " + UID);
			
			//BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			BufferedInputStream inFromServer = new BufferedInputStream(clientSocket.getInputStream());
			System.out.println("Start to get reflexive port through findportserver .....");

			// read from findportserver now
			byte[] recv_bytes = new byte[5] ; // port range is 30000 to 65355, so given byte[5]
			//int n = 0;
			//StringBuilder mapped_port = new StringBuilder() ;

			int bytesRead = inFromServer.read(recv_bytes) ; //read up to mapped_port.length i.e. 5
			// only 5 bytes, so no while loop
			// if the read doesn't return 5 bytes, return failure
			if (bytesRead != 5) {
				System.out.printf("Failed to get relay port:%d\n", bytesRead) ;
				//client_port stays as -1
			}
			else {
				String recv_buf = new String(recv_bytes, 0, bytesRead);
				//mapped_port.append(recv_buf) ;	        
				client_port = Integer.parseInt(recv_buf);
				System.out.printf("\nGot mapped port: %d\n ", client_port);
			}

		} catch (Exception getPort_ex) {
			System.out.printf("getPort call failed: %s\n", getPort_ex.getMessage());
			//System.exit(-1);
		}
		
		return client_port ;
	}
	
	public void shutdown_socket() {
		if (clientSocket != null)
			try {
				clientSocket.close();
			} catch (IOException sh_ex) {
				System.out.printf("Shutdown socket failed: %s\n", sh_ex.getMessage());
				System.exit(-1);
			}
	}

	public static void main(String argv[]) throws Exception
	{
		if (argv.length != 1) {
			System.out.printf("\n Usage: %s <ip of server> \n", argv[0]);
			System.exit(1);
		}
		
		String UID = "abcd1234568881111111111111111111" ;//testing UID
		
		FindPortClient myclient = new FindPortClient(argv[0], FINDPORTSERVER_PORT);
		if (myclient != null) {
			int parsed_port = myclient.getPort(UID); 

			if (parsed_port < 0) {
				System.out.println("Find Client Port failed");
			} else {
				System.out.printf("Find p2p reflexive port: %d", parsed_port);
			}
		}

		myclient.shutdown_socket();
	}
}
