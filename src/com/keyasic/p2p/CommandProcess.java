
package com.keyasic.p2p;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * CommandProcess connects to Kdrive through STURN/TURN server.
 * 
 *  @author David Hsia
 *  @author Kevin Huang
 *  @version %I,%G%
 */
public class CommandProcess {

	Socket sockfd = null ;
	//DataOutputStream outToServer = null ;
	BufferedOutputStream outToServer = null;
	BufferedInputStream inFromServer = null;
	public static final int CMD_LEN = 64;
	public static final int MAX_RECV_BUFF_SIZE = 64*1024 ; //65536

	public CommandProcess() {

	}

	/**
	 * Connect to Kdrive via the turn_server and relayed_port.
	 * The relayed_port is returned by FindPortClient.
	 * After connected successfully, use the object's method to send/receive commands to Kdrive
	 * 
	 * @param turn_server
	 * @param relayed_port
	 * @return session Id - hashcode of socket. 
	 * 			-1 : failed     
	 */
	public int Connect(String turn_server, int relayed_port) {
		
		int ret_sid = 0 ;
		
		try {
			sockfd = new Socket();
			InetSocketAddress isa = new InetSocketAddress(turn_server, relayed_port);
			sockfd.connect(isa, 10000);

			//outToServer = new DataOutputStream(sockfd.getOutputStream());
			outToServer = new BufferedOutputStream(sockfd.getOutputStream());
			inFromServer = new BufferedInputStream(sockfd.getInputStream());
			
			ret_sid = sockfd.hashCode() ;  // This actually is "port + addr.hashCode()"

		} catch (Exception pr_ex) {
			ret_sid = -1 ;
			System.out.printf("instantiate commandProcess failed: %s",  pr_ex.getMessage());
		}
		
		return ret_sid;
	}

	/**
	 * send cmdline to kdrive.  
	 * @param cmdline.
	 * @return 0: succeeded
	 *        -1: failed
	 */
	private int send_to_turnserver(String cmdline) {

		byte[] pad_cmd = null;
		try {
			pad_cmd = Arrays.copyOf(cmdline.getBytes("UTF-8"), CMD_LEN); // create new byte[] and copy into it
		} catch (UnsupportedEncodingException e) {
			System.out.println("Convert cmd to bytes failed\n");
			return -1 ;
		} 

		int ret_code = 0 ;
		try {
			outToServer.write(pad_cmd, 0, CMD_LEN); // right now server only take 64 bytes msg
			outToServer.flush();
		} catch (IOException io_ex) {
			System.out.printf("Send cmd to turnserver faield: %s\n", io_ex.getMessage()) ;
			ret_code = -1 ;
		}
		
		return ret_code ;
	}

	/**
	 * Read 'recv_size' bytes char from KDrive's and return the result
	 * 
	 * @param recv_size - expect receive length of char. 0 means read until '\n'
	 * @return received string from Kdrive, Null when exception
	 */
	private String recv_string_from_turnserver(int recv_size) {

		byte[] recv_buff = new byte[MAX_RECV_BUFF_SIZE] ; //doen't like C, java init object
		int bytesRead = 0, total_sz =0, nw_eof = 0;
		//StringBuilder recv_contents = new StringBuilder() ;
		
		if (recv_size > MAX_RECV_BUFF_SIZE)
			recv_size = MAX_RECV_BUFF_SIZE ;

		if (recv_size == 0) {
			nw_eof= 1 ;  // stop reading when last char is \n
			recv_size = MAX_RECV_BUFF_SIZE ;
		}

		Arrays.fill(recv_buff, (byte)0);
		int offset = 0 ;
		try {
			while (offset < recv_size) {
				bytesRead = inFromServer.read(recv_buff, offset, recv_size-offset); // up to MAX_RECV_BUFF_SIZE bytes
				if (bytesRead < 0) // no more data to receive
					break;

				offset += bytesRead;	
				//String recv_buf = new String(recv_buff, 0, bytesRead);
				//System.out.printf("Read from client (through turnserver) buffer: %s, bytes read: %d\n ", recv_buf, bytesRead);
				
				if (nw_eof == 1 && recv_buff[offset-1] == '\n' ) // only check the last char, not inside loop
					break; // end of line

			}
		} catch (IOException io_ex) {
			Arrays.fill(recv_buff, (byte)0);
			System.out.printf("Receive from turnserver failed: %s\n", io_ex.getMessage()) ;
		}
		//System.out.printf("Read from client fin\n");
		String ret_str = null;
		try {
			ret_str = new String(recv_buff, 0, offset, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			System.out.println("LS command Convert to String failed");
		}
		 
		 return ret_str ;
	}
	/**
	 * Read 'recv_size' bytes binary data from KDrive's and return the result
	 *  
	 * @param recv_size - expect receive length of data
	 * @return: null if recv_size is 0, or receive nothing <br>
	 *          byte[] - the receive buffer <br>
	 */
	private byte[] recv_binary_from_turnserver(int recv_size) {

		byte[] recv_buff = new byte[MAX_RECV_BUFF_SIZE] ; //doen't like C, java init object
		
		if (recv_size > MAX_RECV_BUFF_SIZE)
			recv_size = MAX_RECV_BUFF_SIZE ;
		
		if (recv_size <= 0)
			return (byte[])null ;
		 
		Arrays.fill(recv_buff, (byte)0);
		int bytesRead = 0, offset=0;
		try {
			while (offset < recv_size) {
				bytesRead = inFromServer.read(recv_buff, offset, recv_size-offset); // up to MAX_RECV_BUFF_SIZE bytes

				if (bytesRead <= 0) // no more data to receive
					break;
				
				offset += bytesRead;
			}

		} catch (IOException io_ex) {
			Arrays.fill(recv_buff, (byte)0);
			System.out.printf("Receive binary from turnserver failed: %s\n", io_ex.getMessage()) ;
		}

		if (offset != recv_size)
			System.out.printf("Incomplete Receive data, expected: %d, received: %d\n", recv_size, offset);
		
		//String str = new String(recv_bytes);
		//System.out.printf("Read from client suze: %d %d\n", ()recv_bytes[0] , (recv_bytes[1] << 8));

		return recv_buff;
	}
	/**
	 * gracely shutdownt socket
	 */
	public void shutdown_socket() {

		try {
			inFromServer.close();
			outToServer.flush();
			outToServer.close();

			if (sockfd != null)
				sockfd.close();
			
		} catch (IOException sh_ex) {
			System.out.printf("Shutdown commandProcess socket failed: %s\n", sh_ex.getMessage());
			System.exit(-1);
		}
	}

	public String basename(String fullpath) {
		
		String[] file_split = fullpath.split("\\/(?=[^\\/]+$)") ;
		
		return file_split[1] ;
	}
	/**
	 * List the folder of Kdrive. This is a wrapper for APP to call.
	 *  
	 * @param folder_name return the content of the folder
	 * @return folder list in XML format
	 */
	public String command_list_folder(String folder_name) {

		//this.send_to_turnserver("cmdcd " + folder_name) ;
		//String respline = this.recv_string_from_turnserver(0);
		//System.out.printf("Sent \"cmdcd %s\" --> response: %s\n", folder_name, respline);
		
		this.send_to_turnserver("cmdls " + folder_name) ;

		String respline = this.recv_string_from_turnserver(0);

		//System.out.printf("Sent \"cmdls\" ---> response len: %d, %s\n", respline.length(), respline);

		return respline ;
	}
	/**
	 * Get the info of Kdrive. This is a wrapper for APP to call.
	 * 
	 * @return info of current directory
	 */
	public String command_info() {

		this.send_to_turnserver("cmdinfo") ;
		String respline = this.recv_string_from_turnserver(0);
		//System.out.printf("Sent \"cmdinfo\" ---> response: %s\n", respline);

		return respline ;
	}
	/**
	 * List the current Kdrive's folder name. This is a wrapper for APP to call.
	 *  
	 * @return: current directory 
	 */
	public String command_pwd() {

		this.send_to_turnserver("cmdpwd") ;
		String respline = this.recv_string_from_turnserver(0);
		//System.out.printf("Sent \"cmdpwd\" ---> response: %s\n", respline);
		return respline ;
	}

	/**
	 * get remote file from KDrive and copy it to current directory
	 * @param remote_filename
	 * @param local_filename
	 * @return: <br>
	 *     0: successfully get file and save to local directory
	 *    -1: failed to get file
	 */
	public int command_getfile(String remote_filename, String local_filename) {

		//System.out.printf("Get remote file: %s\n", remote_filename);

		// Java String has MAX_VALUE size (2^31 - 1) Or 2GB bytes
		// can we have bigger than 2G file?

		byte[] recv_buff = null; // new byte[MAX_RECV_BUFF_SIZE] ;  // MAX_RECV_BUFF_SIZE might be too big
		byte[] read_block_size_buff = new byte[8] ;

		this.send_to_turnserver("cmdsize " + remote_filename) ;
		// read response for cmdsize
		read_block_size_buff = this.recv_binary_from_turnserver(8);//cmdsize always 8 bytes
		
		ByteBuffer buffer = ByteBuffer.wrap(read_block_size_buff);
		buffer.order(ByteOrder.LITTLE_ENDIAN);  // if you want little-endian
		int file_size = buffer.getInt();
         
		//System.out.printf("Sent \"cmdsize %s\" ---> response size: %d\n", remote_file, file_size);

		if (file_size <= 0) {
			System.out.println("remote file size = 0. return now");
			return 0 ;
		}

		// get basename of remote file
		//String[] remote_file_split = remote_filename.split("\\/(?=[^\\/]+$)") ;

		FileOutputStream fop = null;
		try {
			File local_file = new File(local_filename) ;
			fop = new FileOutputStream(local_file);

			// if file doesn't exists, then create it
			if (!local_file.exists()) {
				local_file.createNewFile();
			}
			
		} catch (FileNotFoundException e) {
			System.out.println("Open output file failed " + e.getMessage());
			return -1 ;
		} catch (IOException ioe) {
			System.out.println("Create new file error: %s" + ioe.getMessage());
		}
		
		// start to receive remote file

		int recv_size = 0, offset=0, read_cnt = 0 ;
		int write_pos = 0, write_len = 0, total_sz = 0;
		int read_block_size = 16384 ;

		long last_time = System.currentTimeMillis() ;
		long curr_time = last_time ;
		
		while (total_sz < file_size) {

			if (offset + read_block_size  > file_size) {
				read_block_size = file_size - offset; // outstanding length
			}

			//System.out.println("Send \"cmdget  " + offset + " " + read_block_size + " " + remote_file + "\"") ;
			this.send_to_turnserver("cmdget " + offset + " " + read_block_size + " " + remote_filename) ;

			recv_buff = this.recv_binary_from_turnserver(read_block_size+8); // up to 16384+8 bytes  		
			if (recv_buff == null) // no data or exception
				break;

			//System.arraycopy(tmp_buff, 8, recv_buff, 0, read_block_size);
			write_pos = 8 ;
			write_len = read_block_size ;

			//write read_block_size into file
			try {
				fop.write(recv_buff, write_pos, write_len);
				fop.flush() ;
				total_sz += write_len ;
			} catch (IOException e) {
				System.out.println("Write to file error: %s" + e.getMessage());
			} 

			offset += read_block_size;
			read_cnt += 1 ;
		}

		curr_time = System.currentTimeMillis();
		//System.out.printf("\n ***Done!!!\nTotal count: %d, read %d bytes. write %d bytes, transfer rate: %f bytes/ms\n", read_cnt, offset, total_sz, offset*1.0/(curr_time - last_time));

		return 0 ;
	}

}
