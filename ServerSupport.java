package com.java.test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ListIterator;

/*
 * This class is used for supporting separate data connection for each transfer (STOR/RETR)
 * Each STOR or RETR opertaion starts a new thread of this class denoted by "cmd" property of the object
 * Initially running is set to true, when terminated this running is set to false and before the thread ends 
 * it deletes the file that it was working on to complete the termination operation
 * 
*/
public class ServerSupport extends Thread{
	ClientsTracker ct;//Client associated with this thread
	String filename;//File name associated with the transaction
	int port;//Port associated with the data connection
	InputStream isr;
	OutputStream os;
	long filesize;//File size of the current file operated on
	String cmd;//current command STOR/RETR
	static ServerSocket data_socket = null;
	volatile boolean running = true;
	
	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}
	/*
	 * Constructor setting all properties of the file
	*/
	public ServerSupport(ClientsTracker ct, String filename, 
			long filesize, int port, InputStream isr, OutputStream os, 
			String string,boolean running) {
		super();
		this.cmd=string;
		this.ct = ct;
		this.filename=filename;
		this.port=port;
		this.filesize = filesize;
		this.os=os;
		this.isr=isr;
		this.running=running;
		
		if(data_socket!=null&&data_socket.getLocalPort()==port){
			System.out.println("Old reference returned");
			this.data_socket = data_socket;
			}
		else
			try {
				data_socket =  new ServerSocket(port);
				System.out.println(data_socket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	public void run(){
		
		
		if(cmd.equals("STOR")){//Runs only if the current cmd is STOR
		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		System.out.println("Starting transmission");
		Socket sending_client = null;
	    try {
	    	
	    	sending_client = data_socket.accept();//Accept the connection from client to upload
	    	
	    	System.out.println("Sending Client:"+sending_client);
	        
	    	
	    	fos = new FileOutputStream(ct.path+"\\\\"+filename);//create a file or overwrite existing file
	        bos = new BufferedOutputStream(fos);
	    	 InputStream in = sending_client.getInputStream();
	    	byte[] bytes = new byte[2048];//(int)(filesize)+1];
	    	int count=0;
	    	int received=0;
	    	//Below code reads input from the client and writes it into the file
	    	while((count = in.read(bytes))>0 && running==true)
	    	{
	    		received=count+received;
	    		
	    		bos.write(bytes,0,count);
	    		
	    		if(received>=filesize)//When file is received completely then terminate
	    			break;
	    		
	    		System.out.println("Receiving");
	    	}
	    		bos.close();
	    		in.close();
	    		//This part of the code handles Status Tracking of a transaction
	    		int i=0;
	    		ListIterator<StatusTracker> listOfStatus = Server.ST_AL.listIterator();
	    		StatusTracker currStat = null;
	    		//The code below gets the status object of this transaction as complete
	    		while(listOfStatus.hasNext()==true){
	    			
	    			if(listOfStatus.next().servSupp==this)
	    			{
	    				currStat = Server.ST_AL.get(i);
	    				currStat.setStatus("Complete");
	    				System.out.println(currStat.toString());
	    			}
	    			i++;
	    		}
	    		//This part of the code handles Status Tracking of a transaction
	    		
	    		//this part handles cancel operation
	    		if(!this.isRunning()){//if running=false (if transaction is terminated)
	    			//This has no effect on completed transactions. Only the running transactions are terminated.
	    			//this.join();
	    			Path p = Paths.get(ct.path+"\\\\"+filename);
	    			Files.delete(p);
	    			currStat.setStatus("Terminated");//Status is set to terminated for the transaction that were running when CANCEL operation was executed
	    			System.out.println(currStat.toString());
	    			System.out.println("Transaction Terminated");
	    		}
	    		//this part handles cancel operation
	    		System.out.println("Done");
	    		
	    	//System.out.println("File " + filename + " downloaded (" + current + " bytes read)");
	      } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	      finally {
	       
				try {
					 if (fos != null) fos.close();
				     if ( sending_client!= null) sending_client.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	       
	      }
		}else if(cmd.equals("RETR"))//This code is similar to STOR operation other than the fact that it writes a file existing on the server side to the client side
		{
		
	    	
	    	
			try {
				Socket receiving_client = null;
				receiving_client = data_socket.accept();
				File f=new File(ct.path+"//"+filename);
				FileInputStream fis = new FileInputStream(f);
				BufferedInputStream bis = new BufferedInputStream(fis);
				OutputStream bos =  receiving_client.getOutputStream();
				byte[] bytes = new byte[1024];
			    int len = 0,totalbytes=0;
				
				System.out.println("Sending Client:"+receiving_client);
				receiving_client.setKeepAlive(true);
			    while((len = bis.read(bytes)) != -1 && running==true)
			    {
			        // send the first len bytes of arr over socket.
			    	if(len>=1){
			    		totalbytes=len+totalbytes;
			    		System.out.println("Sending");
			    		bos.write(bytes, 0, len);
			    		bos.flush();
			    		if(totalbytes>=filesize){
			    		System.out.println("Done");
			    		bos.close();
			    		bis.close();
			    		fis.close();
			    		receiving_client.close();
			    		
		    			break;	
			    		}
			    	}
			    }
			  //This part of the code handles Status Tracking of a transaction
	    		int i=0;
	    		ListIterator<StatusTracker> listOfStatus = Server.ST_AL.listIterator();
	    		StatusTracker currStat = null;
	    		while(listOfStatus.hasNext()==true){
	    			
	    			if(listOfStatus.next().servSupp==this)
	    			{
	    				currStat = Server.ST_AL.get(i);
	    				currStat.setStatus("Complete");
	    				System.out.println(currStat.toString());
	    			}
	    			i++;
	    		}
	    		//This part of the code handles Status Tracking of a transaction
			  //this part handles cancel operation
	    		if(!this.isRunning()){//if running=false (if transaction is terminated). 
	    			//This has no effect on completed transactions. Only the running transactions are completed.
	    			//this.join();
	    			//Path p = Paths.get(ct.path+"\\\\"+filename);
	    			//Files.delete(p);
	    			currStat.setStatus("Terminated");
	    			System.out.println(currStat.toString());
	    			bos.close();
	    			System.out.println("Transaction Terminated");
	    		}
	    		//this part handles cancel operation
			}/*catch(SocketException e)
			{
				System.out.println("Socket connection closed");
				
			} */catch(IOException ex) {
			    ex.printStackTrace();
			}

		}
		}
}
