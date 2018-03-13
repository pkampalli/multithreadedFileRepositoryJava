package com.java.test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Set;

/**
 * @author Md Arshad
 *
 */

/*
 * This is the server of the application. This class needs to be
 *  executed before any client can make a connection request
*/
public class Server extends Thread{
	static Socket global_socket = null;//used to support simultaneous connection of multiple clients
	static ArrayList<ClientsTracker> CT_AL = new ArrayList<ClientsTracker>();//Keeps track of all clients connected to the application
	static ArrayList<StatusTracker> ST_AL =  new ArrayList<StatusTracker>();//Keeps track of all tarnsfers started by all connected clients
	
	static int port = 10599;
	String users [] = {"arshad","pramod","mohan","michael"};//Valid users
	String password [] = {"arshad","pramod","mohan","michael"};//Respective valid passwords
	public void run(){
		String root = "C:\\FTP_Application\\";//root of the application
		String path = "C:\\FTP_Application\\";//Current working directory path
		String login_username=null ;
		Socket local_socket=global_socket;//capture the global-socket locally to facilitate multiple simultaneos connection requests
		
		try {
			
			InputStream isr = local_socket.getInputStream();
	
			//Send the welcome message
			DataInputStream dis = new DataInputStream(isr);
			OutputStream os = local_socket.getOutputStream();
			DataOutputStream dos = new DataOutputStream(os);
			dos.writeUTF("*******************************************************\n"
	    		   + "***********        WELCOME TO FTP      ****************\n"
	    		   + "*******************************************************\n");
			login_username = user_Verification(dis,dos);//Call the user verification to validate the user loggin in to the system
			
			//Create the client tracker for the current user who has logged in successfully 
			//and call command_Handler() to accept commands from this user
			if(login_username!=null){
				System.out.println("User with socket "+local_socket+"logged in");
				ClientsTracker ct = new ClientsTracker(local_socket, login_username, path, root);
				CT_AL.add(ct);
				ct.setPath(path+ct.User_name+"\\");
				System.out.println(ct.path);
				File f = new File("C:\\FTP_Application\\"+login_username);
				 if (!(f.exists() && f.isDirectory())) {
					   boolean success=f.mkdir();
					   if(success){
						   System.out.println("Created directory for user:"+login_username);
					   }
					   }
				command_Handler(local_socket,CT_AL,dis,dos,isr,os);
				
				///////////////////////////////////////////////////////////////////////////////////////
				//login_username="pramod";
				//File file = new File(path+"pramod");
				//boolean success = file.mkdir();
				//if(success)
				//	System.out.println("Created directory");
				//FileWriter writer = new FileWriter(file);
				/*Path f = Paths.get(path+login_username+"\\\\");
				Set<PosixFilePermission> perms =
					    PosixFilePermissions.fromString("rwxrwxrwx");
					FileAttribute<Set<PosixFilePermission>> attr =
					    PosixFilePermissions.asFileAttribute(perms);
					Files.createDirectories(f, attr);
				ct.path=path+login_username+"\\\\";*/
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	/*
	 * This methods receives commands from client and services it.
	 * For STOR and RETR commands it creates the ServerSupport object
	 * sets all properties of it and sets propert "cmd" to "STOR" or 
	 * "RETR" respectively
	 */
	private void command_Handler(Socket local_socket, ArrayList<ClientsTracker> cT_AL2, DataInputStream dis,
			DataOutputStream dos, InputStream isr, OutputStream os) {
		// TODO Auto-generated method stub
		boolean continue_service = true;
		String cmd = null,username = null;
		ClientsTracker currentCT=null;
		for(ClientsTracker ct: CT_AL)
			if(ct.getSocket().equals(local_socket)){
				username = ct.getUser_name();
				currentCT=ct;
			}
		while(continue_service){
			try {
				cmd = dis.readUTF();
				System.out.println("Received command is: "+ cmd +" from user "+username);
				switch (cmd.split(" ")[0]) {
				case "STOR":
					String filename = cmd.split(" ")[1];
					//Receive the File size to maintain status
					long filesize = dis.readLong();
					dos.writeUTF("FTP Response: status = 200 (OK)\n"
							 + "\t\tMSG=Command Ok");
					System.out.println("File name is :"+filename+" File size is :"+filesize);
					ServerSupport serve = new ServerSupport(currentCT,filename,filesize,port+2,isr,os,cmd.split(" ")[0],true);
					StatusTracker st = new StatusTracker(serve, local_socket, cmd.split(" ")[0], 
							filesize, currentCT.path, filename,
							0, "In Progress", 0);
					ST_AL.add(st);
					//System.out.println(st.toString());
					serve.start();
					break;
				case "PORT":
					int newPort = Integer.parseInt(cmd.split(" ")[1]);
					if(newPort>5000&&newPort<60000){
						dos.writeUTF("FTP Response: status = 200 (OK)\n"
								 + 				"\t\tMSG   = Command Ok");
						port = newPort;
					
					}else
						dos.writeUTF("FTP Response: status = 501 (OK)\n"
				                 + "\t\tMSG    = Invalid Port Number");
					break;
				case "STAT":
					dos.writeUTF("FTP Response: status = 200 (OK)\n"
							 + 				"\t\tMSG   = Command Ok");
					ArrayList<String> aLstOfStatus = new ArrayList<String>();
					aLstOfStatus.add("File Name\tOperation\tStatus\n");
					ListIterator<StatusTracker> liIterForStatus = ST_AL.listIterator();
					int i=0;
					while(liIterForStatus.hasNext()==true){
						if(liIterForStatus.next().s.equals(currentCT.socket))
						{
							aLstOfStatus.add(ST_AL.get(i).getFile_Name()+"\t"+ST_AL.get(i).getOperation()+"\t"+ST_AL.get(i).getStatus());
						}
						i++;
					}
					ObjectOutputStream oos1 =  new ObjectOutputStream(os);
					oos1.writeObject(aLstOfStatus);
					break;
				case "QUIT":
					dos.writeUTF("FTP Response: status = 226 (OK)\n"
				             + "\t\tMSG    = Data Connection Closed. All running Transfers will be Completed. Ok\n");
					continue_service=false;
					break;

				case "CANCEL":
					
					for(StatusTracker s : ST_AL)
					{
						if(s.s==currentCT.socket)
							s.servSupp.setRunning(false);
					}
					dos.writeUTF("FTP Response: status = 426 (OK)\n"
							             + "\t\tMSG    = Data Connection Closed. All running Transfers Aborted. Ok\n");
					break;
				case "RETR":
					String filename2 = cmd.split(" ")[1];
					File f = new File(currentCT.path+filename2);
					long filesize2=f.length();
					if(f.exists())
					{
						dos.writeUTF("FTP Response: status = 200 (OK)\n"
							                 + "\t\tMSG    = Command Ok\n"
							                 + "\t\tsize   ="+filesize2);
						//dos.writeLong(filesize2);
						if(!dis.readBoolean()){
							System.out.println("User Has Refused to receive");
							break;
						}
						System.out.println("Start to Send file: File name is :"+filename2+" \nFile size is :"+filesize2+" \nUser_Name is :"+currentCT.getUser_name());
						ServerSupport serve2 = new ServerSupport(currentCT,filename2,filesize2,port+2,isr,os,cmd.split(" ")[0],true);
						StatusTracker st2 = new StatusTracker(serve2, currentCT.socket, cmd.split(" ")[0], 
								filesize2, currentCT.path, filename2, 0, "In Progress", 0);
						//System.out.println(st2.toString());
						ST_AL.add(st2);
						serve2.start();
						
					}else{
						dos.writeUTF("FTP Response: status = 550 (OK)\n"
							                 + "\t\tMSG    = File not Found");
					}
					
					break;
				case "LIST":
						
					String path=null;
					for(ClientsTracker ct : CT_AL)
						if(ct.socket.equals(local_socket))
							path=ct.path;
					DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(path));
					ArrayList <String> list = new ArrayList<String>();
					for(Path p : stream)
					{
						
						list.add(p.toString());
					}
					System.out.println(list);
					ObjectOutputStream oos = new ObjectOutputStream(os);
					oos.writeObject(list);
						
					break;
				
				case "NOOP":
					dos.writeUTF("FTP Response: status = 200 (OK)\n"
							                 + "\t\tMSG    = Command Ok\n");
					break;
				case "DEL":
					String fileToDelete = cmd.split(" ")[1];
					File f3 = new File(currentCT.getPath()+fileToDelete);
					if(f3.exists() && (f3.isDirectory()==false)){
						boolean delResult = f3.delete();
						if(delResult)
							dos.writeUTF("FTP Response: status = 200 (OK)\n"
			                 		     + "\t\tMSG    = Command Ok\n");
						else
							dos.writeUTF("FTP Response: status = 450 (OK)\n"
		                 		     + "\t\tMSG    = File Busy\n");
					}else
						dos.writeUTF("FTP Response: status = 550 (NOT OK)\n"
	                 		     + "\t\tMSG    = File does not exists.\n");
					break;
				case "CWD":
						String newDirName = cmd.split(" ")[1];
						//Path p = Paths.get(currentCT.getPath()+newDirName+"\\");
						File f2 = new File(currentCT.getPath()+newDirName+"\\");
						if(f2.exists() && f2.isDirectory()){
						currentCT.setPath(currentCT.getPath()+newDirName+"\\");	
						dos.writeUTF("FTP Response: status = 200 (OK)\n"
				                 		     + "\t\tMSG    = Command Ok\n");
						}else
							dos.writeUTF("FTP Response: status = 550 (NOT OK)\n"
		                 		     + "\t\tMSG    = Directory does not exists.\n");
					break;
				case "CDUP":
						currentCT.setPath(currentCT.getRoot()+currentCT.getUser_name()+"\\");
						dos.writeUTF("FTP Response: status = 200 (OK)\n"
											+ "\t\tMSG    = Command Ok\n");
					break;
				case "PWD":
					
					dos.writeUTF("FTP Response: status = 200 (OK)\n"
							                 + "\t\tMSG    = Command Ok\n");
					dos.writeUTF("\\"+currentCT.getUser_name()+currentCT.getPath().split(currentCT.getUser_name())[1]);
					break;
				case "MKDIR":
					String folders = cmd.split(" ")[1];
					File f1 = new File(currentCT.path+"\\\\"+folders);
					 if (!(f1.exists() && f1.isDirectory())) {
						   boolean success=f1.mkdir();
						   if(success){
							   System.out.println("Created directory for user:"+currentCT.User_name);
							   dos.writeUTF("FTP Response: status = 257 (OK)\n"
						                 + "\t\tMSG    = PATHNAME created Ok\n");
						   }
						   }
					
					break;
					
				default:
					break;
				}
			}catch(SocketException e)
			{
				System.out.println("Socket connection closed");
				break;
			}
			catch (EOFException e) {
				// TODO Auto-generated catch block
				//System.err.print("EOF Exception");
				System.out.println("User Disconnectd. Aborting.");
				break;
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				System.err.print("IOException");
				System.out.println("User Disconnectd. Aborting.");
				break;
			}
			
		}
	}

/*
 * This method does the user verification for letting a user enter into the application
 * and use its services.  
 */
	private String user_Verification(DataInputStream dis, DataOutputStream dos) {
		// TODO Auto-generated method stub
		boolean user_name=false,password=false;
		String username = null;
		while(!user_name){
			
			try {
				String cmd = dis.readUTF();
				if(cmd.split(" ").length==2)
				if(cmd.split(" ")[0].equalsIgnoreCase("USER"))
				{
					username = cmd.split(" ")[1];
					System.out.println(username);
					user_name = check_user_exits(username);
					if(user_name)
						dos.writeUTF("FTP Response: status = 331 (OK)\n"
											 + "\t\tMSG=User exits. Please enter Password");
					else
						dos.writeUTF("FTP Response: status = 332 (NOT OK)\n"
											 + "\t\tMSG=User does not exits. Need account for login.");
				}else
					dos.writeUTF("FTP Response: status = 332 (NOT OK)\n"
							 + "\t\tMSG=Incorrect Format. User not logged in.");

			}catch (SocketException e)
			{
				System.out.println("User Disconnected. Connection Closed");
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		while(!password){
			
			try {
				String cmd = dis.readUTF();
				if(cmd.split(" ").length==2)
				if(cmd.split(" ")[0].equalsIgnoreCase("PASS"))
				{
					String pass = cmd.split(" ")[1];
					password = check_password_exits(username,pass);
					if(password){
						dos.writeUTF("FTP Response: status = 230 (OK)\n"
											 + "\t\tMSG=User logged in, proceed");
						return username;
						}
					else
						dos.writeUTF("FTP Response: status = 530 (NOT OK)\n"
											 + "\t\tMSG=Incorrect password. User not logged in.");
				}
				else
					dos.writeUTF("FTP Response: status = 530 (NOT OK)\n"
										 + "\t\tMSG=Incorrect Format. User not logged in.");
			
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		return null;
	}

/*
 * This method matches a password with the user input in PASS command. 
 * If it exists for the user true is returned else false
 */
	private boolean check_password_exits(String username, String pass) {
		// TODO Auto-generated method stub
		for(int i=0;i<users.length;i++)
			if(username.equals(users[i]))
				if(pass.equals(password[i]))
					return true;
				else 
					return false;
		
		return false;
	}
	
	/*
	 * This method matches a username with the user input in USER command. 
	 * If it exists true is returned else false
	 */



	private boolean check_user_exits(String username) {
		// TODO Auto-generated method stub
		
		for(String s:users)
			if(s.equalsIgnoreCase(username))
				return true;
		
		return false;
	}




	public static void main (String args[])
	{
	
		 try {	
			 	/*
			 	 * The code below creates an application level folder
			 	 * Each user is assigned an account(folder) inside this folder 
			 	 */
				File file=new File("C:\\FTP_Application");
				if(!(file.exists() && file.isDirectory())){
					boolean suc=file.mkdir();
					if(suc){
						System.out.println("Created root directory");
					}
				}
				
				
				
			 
			 /*String user [] = {"arshad","pramod","mohan"};
			 for(int i=0;i<user.length;i++){
				 File f = new File("C:\\FTP_Application\\"+user[i]);
				 if (!(f.exists() && f.isDirectory())) {
					   boolean success=f.mkdir();
					   if(success){
						   System.out.println("Created directory for user:"+user[i]);
					   }
					 }
			 }*/
			/* File f1 = new File("C:\\FTP_Application\\pramod");
			 if (!(f1.exists() && f1.isDirectory())) {
			   boolean success1=f1.mkdir();
			   if(success1){
				   System.out.println("yes pramod");
			   }
			 }*/
			ServerSocket ss = new ServerSocket(port);//Server Socket is started
		    System.out.println("Server Started....");
		    InetAddress ip;
			  try {

				ip = InetAddress.getLocalHost();
				System.out.println("Current IP address : " + ip.getHostAddress());

			  } catch (UnknownHostException e) {

				e.printStackTrace();

			  }
		    while(true){
		    Server s = new Server();
			global_socket = ss.accept();//Server waits to listen for a connection request from the client
			
		    System.out.println("Connected to client with IP : "+ global_socket.getRemoteSocketAddress());
		    s.start();//As soon as the request is received a new Server thread is started and local_socket
		    //objectcapture the global socket object and main thread is ready to listen to a new
		    //connection request
		    
		    
		    
		    /*InputStream isr = client.getInputStream();
		    
		    DataInputStream dis = new DataInputStream(isr);
		    System.out.println(dis.readUTF());
		    OutputStream os = client.getOutputStream();
		    DataOutputStream dos = new DataOutputStream(os);
		    dos.writeUTF("Received");*/
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}//Listen to the port 20 for an incoming request
		
	        
	}
}
