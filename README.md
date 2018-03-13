# multithreadedFileRepositoryJava

This repository consists the code for a stand alone multi threaded file repository server and the respective client. This server can be used to connect with a number of clients simultaneously. The server and client support a set of operations which are described below. The communication follows the standard File Transfer Protocol(FTP).

Instructions:
Run the server code “Server.java”. 

Run the client code “Client.java” and enter the IP Address of the system on which server is running. (Can find out IP Address using IPCONFIG command in Command Prompt if it is a Windows machine)

Enter the port number as 20.

Next, use USER <space> <username> followed by PASS <space><password> to log into the server.

Once logged in, you can enter the following commands:

STOR <local file path> to store file on server
RETR <file name on server> to retrieve file from server machine.
LIST to see the list of files and folders in current directory.
STAT to see status of transactions.
PORT <port number> to change the port number.
PWD to see present working directory’s path.
MKDIR <folder name> to create a directory.
CWD <folder> to change working directory.
CDUP to change to parent directory.
NOOP to ping the server.
CANCEL to cancel all ongoing transactions.
DEL <file name> to delete a file on server.
QUIT to log out.
