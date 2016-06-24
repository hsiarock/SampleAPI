package com.keyasic.p2p;

import com.keyasic.p2p.FindPortClient;
import com.keyasic.p2p.CommandProcess;

public class AppClient {

	public static void main(String argv[]) throws Exception
	{
		if (argv.length != 4) {
			System.out.println("\n Usage: AppClient <server IP> <UID> <User> <Passwd>\n");
			System.exit(1);
		}

		String UID =// "f9fc39705b8611e5a8370800200c9a66";
				   "b9e1e6a0553711e5a8370800200c9a66";
                //"abcd1234568881111111111111111111" ;

		String server_addr = argv[0];
		//String UID = argv[1] ;  
		String username = argv[2] ;
		String passwd = argv[3] ; 
		System.out.println("Cmd argv: " + server_addr + " " + UID + " " + username + " " + passwd );
		// username, passwd will be used later!
		
		FindPortClient myclient = new FindPortClient(server_addr, FindPortClient.FINDPORTSERVER_PORT);
		
		int ret_code = -1 ;
		
		if (myclient != null) {
			
			int relayed_port = myclient.getPort(UID); 
			
			if (relayed_port > 0) {
				CommandProcess cmdproc = new CommandProcess() ;
				
				ret_code = cmdproc.Connect(server_addr, relayed_port); // assume Turnserver run at same host as FindPortServer
				if (ret_code < 0)
					System.out.println("Connect to device failed");
				else 
					System.out.println("Connect to device successfully") ;
				
				
				String ls_out = cmdproc.command_list_folder("/mnt/sd") ; 
				//System.out.println("List output" + ls_out) ;

				// kkk.pcapng, ppp.gz, Kconfig, ar6klog.txt
				
				String file_name="/mnt/sd/Kconfig" ;//ar6klog.txt"; //power-2m.tar.bz2";
				//String local_name="/tmp/Kconfig" ;// Kconfig";
				String local_name="D:/Download/Kconfig" ;// Kconfig";
				
				if (cmdproc.command_getfile(file_name, local_name) < 0) 
					System.out.println("getfile command failed\n") ;	
				
				cmdproc.shutdown_socket(); // cmdproc exit scope, so auto destroyed anyway
				
				System.out.println("get file completed successfully");
				System.out.println("please check local file: " + local_name);
			}
		}

		myclient.shutdown_socket();
	}
}
