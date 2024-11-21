import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class FileServer {
    // Server port and storage directory
    private static final int PORT = 42069;
    private static final String STORAGE_DIR = "server_files";
    // Thread pool to handle multiple clients concurrently
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        // Start the server and listen for connections
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running on port " + PORT);
            // Ensure the storage directory exists
            File storageDir = new File(STORAGE_DIR);
            if (!storageDir.exists()) storageDir.mkdir();

            while (true) {
                // Accept client connections
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());
                // Handle each client in a separate thread
                threadPool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Server exception:");
            e.printStackTrace();
        }
    }

    // Inner class to handle client interactions
    static class ClientHandler implements Runnable {
        private final Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            // Handle client commands
            try (DataInputStream dis = new DataInputStream(socket.getInputStream());
                 DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

                String command = dis.readUTF();
                System.out.println("Received command: " + command);

                if ("UPLOAD".equalsIgnoreCase(command)) {
                    // Handle file upload
                    receiveFile(dis, dos);
                } else if ("DOWNLOAD".equalsIgnoreCase(command)) {
                    // Handle file download
                    sendFile(dis, dos);
                } else if ("LIST".equalsIgnoreCase(command)) {
                    // Send list of files to the client
                    sendFileList(dos);
                } else if ("CLEAR".equalsIgnoreCase(command)) {
                    // Clear all files from the server
                    clearFiles(dos);
                } else {
                    // Unknown command received
                    System.out.println("Unknown command: " + command);
                    dos.writeUTF("ERROR: Unknown command");
                }
            } catch (IOException e) {
                System.err.println("Exception in client handler:");
                e.printStackTrace();
            }
        }

        // Method to receive a file from the client
        private void receiveFile(DataInputStream dis, DataOutputStream dos) throws IOException {
            String fileName = dis.readUTF();
            long fileSize = dis.readLong();
            File outputFile = new File(STORAGE_DIR, fileName);

            if (outputFile.exists()) {
                // File already exists on the server
                dos.writeUTF("ERROR: File already exists on the server.");
                System.out.println("File already exists: " + fileName);
                return;
            } else {
                dos.writeUTF("OK");
            }

            // Receive file data from the client
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalRead = 0;
                while (totalRead < fileSize && (bytesRead = dis.read(buffer)) > 0) {
                    fos.write(buffer, 0, bytesRead);
                    totalRead += bytesRead;
                }
                System.out.println("File received: " + fileName);
            } catch (IOException e) {
                System.err.println("Error receiving file: " + fileName);
                e.printStackTrace();
            }
        }

        // Method to send a file to the client
        private void sendFile(DataInputStream dis, DataOutputStream dos) throws IOException {
            String fileName = dis.readUTF();
            File file = new File(STORAGE_DIR, fileName);
            if (!file.exists()) {
                // File not found on the server
                dos.writeUTF("ERROR: File not found");
                System.out.println("File not found: " + fileName);
                return;
            }
            dos.writeUTF("OK");
            dos.writeLong(file.length());

            // Send file data to the client
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) > 0) {
                    dos.write(buffer, 0, bytesRead);
                }
            }
            System.out.println("File sent: " + fileName);
        }

        // Method to send the list of files to the client
        private void sendFileList(DataOutputStream dos) throws IOException {
            File storageDir = new File(STORAGE_DIR);
            File[] files = storageDir.listFiles();
            if (files == null || files.length == 0) {
                dos.writeInt(0);
                System.out.println("No files found in storage directory.");
                return;
            }
            dos.writeInt(files.length);
            System.out.println("Sending file list to client. File count: " + files.length);
            for (File file : files) {
                dos.writeUTF(file.getName());
                System.out.println("Sent file name: " + file.getName());
            }
        }

        // Method to clear all files from the server's storage directory
        private void clearFiles(DataOutputStream dos) throws IOException {
            File storageDir = new File(STORAGE_DIR);
            File[] files = storageDir.listFiles();
            int deletedFiles = 0;

            if (files != null) {
                for (File file : files) {
                    if (file.delete()) {
                        deletedFiles++;
                        System.out.println("Deleted file: " + file.getName());
                    } else {
                        System.err.println("Failed to delete file: " + file.getName());
                    }
                }
            }

            dos.writeUTF("OK");
            dos.writeInt(deletedFiles);
            System.out.println("Cleared " + deletedFiles + " files from the server.");
        }
    }
}
