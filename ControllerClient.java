

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class ControllerClient implements Runnable{
	Scanner sc;
	Socket server;
	DataOutputStream out;
	OutputStream outToServer;
	InputStream inFromServer;
	DataInputStream in;
	boolean userFlag,passFlag;
	String user,serverIP;
	int port;
	Thread t;

	@Override
	public void run(){
		try{
		sc = new Scanner(System.in); 
		System.out.println("Enter Server's IP address: ");
		serverIP = sc.nextLine();				//reading Server's IP Address
		System.out.println("Enter the port: ");
		port = (Integer.parseInt(sc.nextLine()));		//reading port number
		//establishing command connection 
		server = new Socket(serverIP, port);
		outToServer = server.getOutputStream();
	    	out = new DataOutputStream(outToServer);
		inFromServer = server.getInputStream();
	    	in = new DataInputStream(inFromServer);
	    
	    while(true){
	    if(server.isConnected()){
			System.out.println("Connected to server");
			break;
	    }
	    }
	    
	    //Receive welcome message from server.
	    System.out.println("Message from server: \n"+ in.readUTF());
	    
		
		System.out.println("Use USER and PASS commands to login");
		do{
			String username = sc.nextLine();
			String bits[]=username.split(" ");
			if(!bits[0].equalsIgnoreCase("user")||bits.length!=2){  	//check if entered command is USER
				System.out.println("Use USER command to login");	//if not, ask user to enter USER command
				continue;
			}
			out.writeUTF(username);						//write to server
			String responseCode = in.readUTF().split("= ")[1].split(" ")[0];	//receive response from server and see the response code
			
			if(responseCode.equals("331")){
				userFlag=true;
				System.out.println("It is a valid username, now use PASS command to enter password");
				user=username.split(" ")[1];
			}else{
				System.out.println("Invalid username, try again");
			}
		}while(!userFlag);

		if(userFlag){		//if username exists, read password
			do{
				String password = sc.nextLine();
				String bits[]=password.split(" "); 
				if(!bits[0].equalsIgnoreCase("pass")||bits.length!=2){		//see if user entered PASS command or not
					System.out.println("Use PASS command to login");	//if not, ask user to enter PASS command
					continue;
				}
				out.writeUTF(password);
				String responseCode = in.readUTF().split("= ")[1].split(" ")[0];
				
				if(responseCode.equals("230")){
					passFlag=true;
					System.out.println("Logged in successfully");
				}else{
					System.out.println("Invalid password, try again");
				}
			}while(!passFlag);
		}

		//if username and password exists and they match, call command handler module
		if(userFlag&&passFlag){
			commandHandler();
		}
		
		
		
		}catch(Exception e){
			e.printStackTrace();
			System.exit(0);
		}
		
		
		
	}

	//default constructor for this class
	ControllerClient(){
		t= new Thread(this,"control");		//creates a control thread on startup
		t.start();				// starts the thread
		System.out.println("Control Thread started");
	}
	
	//command handler module
	@SuppressWarnings("unchecked")
	public void commandHandler() throws IOException, ClassNotFoundException{
		while(true){
			System.out.print("Command>");
			String input=sc.nextLine();		//read input from user
			String commands[]=input.split(" ");	//split in on <space>
			String command=commands[0];		//first token will always be the command. commands[1] will be arguement
			switch(command){
				case "STOR":
					if(commands.length>2||commands.length<=1){		//check for right number of arguements
						System.out.println("Try STOR [filepath]");	//if not, print the correct syntax
						break;
					}
					if(new File(commands[1]).isFile()){
						commands[1]=commands[1].replaceAll("\\\\", "\\\\\\\\");  //replace single backslash with double backslash
						String path[]=commands[1].split("\\\\");
						//read the list of files on server and check if a file with same name already exists
						out.writeUTF("LIST");
						ArrayList<String> list1=new ArrayList<String>();
						ObjectInputStream ois1 = new ObjectInputStream(inFromServer);
						list1=(ArrayList<String>)ois1.readObject();
						String fileName;
						boolean noFlag=false;
						for(String s:list1){
							String tokens[]=s.split("\\\\");
							fileName=tokens[tokens.length-1];
							if(fileName.equals(path[path.length-1])){
							System.out.println("File with same name exists.It will be deleted and replaced(yes/no):");
							String op1=sc.nextLine();
							if(!op1.equalsIgnoreCase("yes")){
								noFlag=true;
								break;
							}
							}
						}
						//If user has not cancelled the operation in middle
						if(!noFlag){
						out.writeUTF("STOR "+path[path.length-1]);	//send STOR command and filename
						out.writeLong(new File(commands[1]).length());	//send filesize
						String responseCode = in.readUTF().split("= ")[1].split(" ")[0];
						if(responseCode.equals("200")){
							//create a sending thread
							new ClientSupport("STOR", new File(commands[1]), serverIP, port, "stor_"+path[path.length-1]);
						}else{
							System.out.println("Error sending file, Try again.");
						}}
					}else{
						System.out.println("Invalid file name");
					}
					break;
				case "RETR":
					if(commands.length>2||commands.length<=1){			//check for right number of arguements
						System.out.println("Try RETR [filename]");		//if not, print correct syntax
						break;
					}
					out.writeUTF("RETR "+commands[1]);		//send RETR command and filename to sever
					String serverResponse=in.readUTF();
					String responseCode=serverResponse.split("= ")[1].split(" ")[0];	//check for response code from server
					if(responseCode.equals("200")){
						long fileSize=Long.parseLong(serverResponse.split("size   =")[1]);
						boolean flag=false;
						//read a location on local machine to store the received file
						while(!flag){
						System.out.println("Enter a location to store file(or NO to exit): ");
						String path=sc.nextLine();
						if(path.equals("NO")||path.equals("no")){
							out.writeBoolean(false);
							break;
						}
						path=path.replaceAll("\\\\", "\\\\\\\\");

						//check if a file with same name exists in given destination.
						File f =new File(path);
						if(f.exists()&&f.isDirectory()){
							File f1=new File(path+commands[1]);
							if(f1.exists()&&!f1.isDirectory()){
								System.out.println("File with same name exists.It will be deleted and replaced(yes/no):");
								String op=sc.nextLine();
								if(!op.equals("yes")&&!op.equals("YES")){
									continue;
								}
							}
							out.writeBoolean(true);
							flag=true;
							//start a RETR thread
							new ClientSupport("RETR", serverIP, port, "retr_"+commands[1], path+commands[1], fileSize);
						}else{
							System.out.println("Folder doesn't exist");
						}
						}
						
					}else{
						System.out.println("Retrieve cannot be done");
					}
					//operation
					break;
				case "PORT":
					//check for right number of arguements
					if(commands.length>2||commands.length<=1){
						System.out.println("Try PORT [port_number]");
						break;
					}
					out.writeUTF("PORT " +commands[1]);
					String serverResponse2=in.readUTF();
					String responseCode2=serverResponse2.split("= ")[1].split(" ")[0];
					//check the server response
					if(responseCode2.equals("200")){
					port=Integer.parseInt(commands[1]);
					System.out.println("port changed to :"+port);
					}else {
						System.out.println("Invalid Port number");
					}
					
					break;
				case "QUIT":
					//send QUIT to server
					out.writeUTF("QUIT");
					String res=in.readUTF().split("= ")[1].split(" ")[0];
					//check response from server
					if(res.equals("226")){
					System.out.println("Logged out. All running transactions will be completed");
					//close all connections
					inFromServer.close();
					in.close();
					outToServer.close();
					out.close();
					if(server.isConnected())
						server.close();
					sc.close();
					System.exit(0);
					break;
					}
				case "NOOP":
					//check for right number of arguements
					if(commands.length!=1){
						System.out.println("Try NOOP");
						break;
					}
					//send NOOP to server
					out.writeUTF("NOOP");
					String response=in.readUTF().split("= ")[1].split(" ")[0];
					//check response from server
					if(response.equals("200")){
						System.out.println("Command OK");
					}else{
						System.out.println("Error in doing NOOP, try again");
					}
					break;
				case "LIST":
					//check for right number of arguements
					if(commands.length!=1){
						System.out.println("Try LIST");
						break;
					}
					out.writeUTF("LIST");
					ArrayList<String> list=new ArrayList<String>();
					ObjectInputStream ois = new ObjectInputStream(inFromServer);
					//read and display list of files and folders from server
					list=(ArrayList<String>)ois.readObject();
					System.out.println("=======================YOUR FILES=====================");
					for(String s:list){
						String tokens[]=s.split("\\\\");
						System.out.println(tokens[tokens.length-1]);
					}
					System.out.println("======================================================");
					break;
				case "MKDIR":
					//check for right number of arguements
					if(commands.length>2||commands.length<=1){
						System.out.println("Try MKDIR [folder name]");
						break;
					}
					out.writeUTF("MKDIR "+commands[1]);
					String response1=in.readUTF().split("= ")[1].split(" ")[0];
					if(response1.equals("257")){//check response from server
						System.out.println("Folder Created");
					}else{
						System.out.println("Error in doing MKDIR, try again");
					}
					break;
				case "STAT":
					//check for right number of arguements
					if(commands.length!=1){
						System.out.println("Try STAT");
						break;
					}
					//swnd STAT to server
					out.writeUTF("STAT");
					String resp=in.readUTF().split("= ")[1].split(" ")[0];//check response from server
					//read and display the status of all transactions from server
					if(resp.equals("200")){
					ArrayList<String> operations=new ArrayList<String>();
					ObjectInputStream os = new ObjectInputStream(inFromServer);
					operations=(ArrayList<String>)os.readObject();
					System.out.println("======================STATUS=====================");
					for(String s:operations){
						System.out.println(s);
					}
					System.out.println("======================================================");
					}else{
						System.out.println("Operation cannot be completed.Please try again.");
					}
					break;
				case "CANCEL":
					//check for right number of arguements
					if(commands.length!=1){
						System.out.println("Try CANCEL");
						break;
					}
					//send CANCEL to server
					out.writeUTF("CANCEL");
					//check response from server
					String response2=in.readUTF().split("= ")[1].split(" ")[0];
					if(response2.equals("426")){
						System.out.println("Cancelled all running operations");
					}else{
						System.out.println("Error in cancelling, try again");
					}
					break;
				case "PWD":
					//check for right number of arguements
					if(commands.length!=1){
						System.out.println("Try PWD");
						break;
					}
					//send PWD command to server
					out.writeUTF("PWD");
					//check response from server
					String pwdResp=in.readUTF().split("= ")[1].split(" ")[0];
					if(pwdResp.equals("200")){
						System.out.println(in.readUTF());
					}
					break;
				case "CWD":
					//check for right number of arguements
					if(commands.length>2||commands.length<=1){
						System.out.println("Try CWD [Directory]");
						break;
					}
					//send CWD command and foldername to server
					out.writeUTF("CWD "+commands[1]);
					//check response from server
					String cwdResp=in.readUTF().split("= ")[1].split(" ")[0];
					if(cwdResp.equals("200")){
						System.out.println("Working directory changed.");
					}else if(cwdResp.equals("550")){
						System.out.println("Directory doesn't exit.");
					}
					break;
				case "CDUP":
					//check for right number of arguements
					if(commands.length!=1){
						System.out.println("Try CDUP");
						break;
					}
					//send CDUP command ro server
					out.writeUTF("CDUP");
					//check response from server
					String cdupResp=in.readUTF().split("= ")[1].split(" ")[0];
					if(cdupResp.equals("200")){
						System.out.println("Changed to parent directory");
					}
					break;
				case "DEL":
					//check for right number of arguements
					if(commands.length>2||commands.length<=1){
						System.out.println("Try DEL [filename]");
						break;
					}
					//send DEL command and filename to server
					out.writeUTF("DEL "+commands[1]);
					//check response from server
					String resp1=in.readUTF().split("= ")[1].split(" ")[0];
					if(resp1.equals("200")){
						System.out.println("File deleted successfully.");
					}else if(resp1.equals("450")){
						System.out.println("File busy, operation aborted.");
					}else if(resp1.equals("550")){
						System.out.println("File not found.");
					}
					break;
				default:
					System.out.println("Invalid Command");
					break;
				}
			}
		}
		
	
	//-----------
	
}
