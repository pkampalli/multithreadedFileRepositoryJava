

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;


public class ClientSupport implements Runnable {
	File file;
	Long fileSize;
	String operation,ipAddress,receivePath;
	static volatile int dataPort;
	
	//constructor for STOR thread
	public ClientSupport(String op, File file, String ip, int port, String threadName){
		this.operation=op;
		this.file=file;
		this.ipAddress=ip;
		ClientSupport.dataPort=port+2;
		Thread t= new Thread(this,threadName);
		t.start();	//start the STOR thread
	}
	
	//constructor for RETR thread
	public ClientSupport(String op, String ip, int port, String threadName, String path, Long size){
		this.operation=op;
		this.fileSize=size;
		this.ipAddress=ip;
		this.receivePath=path;
		ClientSupport.dataPort=port+2;
		//System.out.println(receivePath);
		Thread t= new Thread(this,threadName);
		t.start();	//start the RETR thread
	}

	@Override
	public void run() {
		try {
			Thread.sleep(4000);//To allow the server to create connection
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		switch(operation){
		case "STOR":
			try {
				//establish a new data connection
				Socket serverData=new Socket(ipAddress, dataPort);
				if(serverData.isConnected()){
					byte[] bytes = new byte[2048];
			        FileInputStream in = new FileInputStream(file);
			        OutputStream out = serverData.getOutputStream();
			        int count=0;
			        int totalread=0;
				//writing file contents
			        while (((count = in.read(bytes)) > 0)&&serverData.isConnected()) {
			        	
			            out.write(bytes, 0, count);
			            totalread=totalread+count;
			            if(totalread>=file.length())
			            	break;
			            
			        }
			        
			        out.close();
			        in.close();
			        serverData.close();
			        
				}
				
			}catch(SocketException e){
				System.out.print("File Transfer Aborted/ Connection Closed\nCommand>");
			}catch(IOException e){
				System.out.print("I/O Error. Transfer Aborted\nCommand>");
			}catch (Exception e) {
				e.printStackTrace();
			}
			
			break;
		case "RETR":
			//establishing a new data connection
			try{
			Socket fromServer = new Socket(ipAddress, dataPort);
			while(true){
			    if(fromServer.isConnected()){
					break;
			    }
			    }
			FileOutputStream fos = new FileOutputStream(receivePath);
	        BufferedOutputStream bos = new BufferedOutputStream(fos);
	    	InputStream in = fromServer.getInputStream();
	    	byte[] bytes = new byte[1024];
	    	int count=0;
	    	int received=0;
	    	//System.out.println("started");
		//reading from server
	    	fromServer.setKeepAlive(true);
	    	while((count = in.read(bytes))!= -1){
	    		if(count>=1){
	    		received=count+received;
	    		
	    		bos.write(bytes,0,count);
	    		bos.flush();
	    		//System.out.println("Receiving");
	    		if(received>=fileSize){
	    			//System.out.println("Done");
	    			bos.close();
	    			break;
	    		}
	    			
	    		}
	    		
	    	}
	    		//System.out.println("exited");
	    		
	    		fos.close();
	    		fromServer.close();
	    		in.close();
	    		
			}catch(SocketException e){
				System.out.print("File Transfer Aborted/ Connection Closed\nCommand>");
			}catch(IOException e){
				System.out.print("I/O Error. Transfer Aborted\nCommand>");
			}catch(Exception e){
				e.printStackTrace();
			}
			break;
		}
		
	}
}