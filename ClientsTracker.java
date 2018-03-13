package com.java.test;

import java.net.Socket;

/**
 * @author Md Arshad
 *
 */
/*
 * The Objects of this class are created for every user that logs into the system
 * These objects are then inserted into the ArrayList<ClientTrackers> data type 
 * in the server class. This ArrayList keeps tracks of all the users that connect 
 * to the system
 */
public class ClientsTracker {
	Socket socket ;//Stores the client socket
	String User_name;// stores the user name of the user in current session
	String path;//stores the current working directory path
	String root;//stores the root folder of the application
	public ClientsTracker(Socket socket, String user_name, String path, String root) {
		super();
		this.socket = socket;
		User_name = user_name;
		this.path = path;
		this.root = root;
	}
	public Socket getSocket() {
		return socket;
	}
	public void setSocket(Socket socket) {
		this.socket = socket;
	}
	public String getUser_name() {
		return User_name;
	}
	public void setUser_name(String user_name) {
		User_name = user_name;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public String getRoot() {
		return root;
	}
	public void setRoot(String root) {
		this.root = root;
	}
	
}
