# File Transfer Client-Server Application
## Overview
This project is a Java-based client-server application that allows users to upload, download, and manage files between a client GUI and a server. It features a complex Graphical User Interface (GUI) for the client, network communication over sockets, file input/output operations, and concurrency handling on both the client and server sides.

Features
Client GUI:

File Table: Displays the list of available files with their names, actions, progress, and status.
Upload File: Allows users to upload files to the server via a file chooser or drag-and-drop.
Download File: Enables users to download selected files from the server.
Refresh File List: Updates the file list to reflect the current files on the server.
Clear Files: Deletes all files from the server (with confirmation).
Progress Bar: Shows the progress of file uploads and downloads.
Server:

Concurrent Connections: Handles multiple client connections simultaneously using a thread pool.
File Management: Stores uploaded files and serves files requested by clients.
Command Handling: Processes commands like UPLOAD, DOWNLOAD, LIST, and CLEAR.

## Getting Started
### 1. Download the Source Code - 
Download the FileClient.java and FileServer.java files and save them in a directory of your choice.

### 2. Compile the Code - 
Open a terminal or command prompt, navigate to the directory containing the source code, and compile both Java files:

#### How to compile
javac FileServer.java <br />
javac FileClient.java

### 3. Run the Server
Start the server by running:

java FileServer

You should see the following output indicating that the server is running: 

Server is running on port 42069

### 4. Run the Client
In a new terminal or command prompt window (keeping the server running), start the client:

java FileClient

The client GUI should appear, displaying the list of available files on the server.

## Usage Instructions
### Uploading Files
**Using the Upload Button:**

Click the Upload File button.
Select a file from the file chooser dialog.
The file will begin uploading, and the progress bar will indicate the progress.
Once uploaded, the file will appear in the table with the status Completed.

**Using Drag-and-Drop:**

Drag a file or multiple files into the Drag and Drop Files Here panel.
The files will begin uploading automatically.
Note: If a file with the same name already exists on the server, an error message will be displayed, and the upload will be aborted.

**Downloading Files:** <br />
Select a file from the table by clicking on it.
Click the Download File button.
Choose a directory where you want to save the file.
The file will begin downloading, and the progress bar will indicate the progress.
Once downloaded, a confirmation message will appear.
Note: If the file already exists in the selected directory, an error message will be displayed, and the download will be aborted.

**Refreshing the File List: <br />**
Click the Refresh File List button to update the table with the latest files available on the server.

**Clearing Files: <br />**
Click the Clear Files button to delete all files from the server.
A confirmation dialog will appear. Click Yes to proceed or No to cancel.
Upon confirmation, all files will be deleted from the server, and the table will be cleared.
Warning: This action cannot be undone and will affect all clients connected to the server.

### Project Structure
FileServer.java: The server-side application that handles client connections, file storage, and command processing. <br />
FileClient.java: The client-side GUI application that allows users to interact with the server.
## How It Works
### Client-Server Communication
Commands:

UPLOAD: Client requests to upload a file.
DOWNLOAD: Client requests to download a file.
LIST: Client requests the list of available files on the server.
CLEAR: Client requests to delete all files from the server.
Data Transfer:

Uses DataInputStream and DataOutputStream for communication.
Files are transferred in byte streams, with progress tracked and displayed on the client GUI.
Concurrency Handling 
<br />
Server:
<br />
* Utilizes an ExecutorService with a fixed thread pool to handle multiple client connections concurrently.
<br />
* Each client is handled in a separate thread (ClientHandler class).
<br />
Client:
<br />
* Uses separate threads for uploading and downloading files to prevent the GUI from freezing.
S* wingWorker and SwingUtilities.invokeLater are used to safely update the GUI from background threads.
<br />
## Error Handling
<br />
File Already Exists:
<br />
* If a file being uploaded already exists on the server, the upload is aborted, and an error message is displayed.
<br />
File Not Found:
<br />
* If a file requested for download does not exist on the server, an error message is displayed.
  <br />
Network Errors:
<br />
* Any network communication errors are caught and handled gracefully, with appropriate messages displayed to the user.
