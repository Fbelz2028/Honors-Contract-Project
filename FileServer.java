package filesharing;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class FileServer {
    private static final int PORT = 12345;
    private static final String STORAGE_DIR = "server_files";
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running on port " + PORT);
            File storageDir = new File(STORAGE_DIR);
            if (!storageDir.exists()) storageDir.mkdir();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());
                threadPool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (DataInputStream dis = new DataInputStream(socket.getInputStream());
                 DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

                String command = dis.readUTF();
                if ("UPLOAD".equalsIgnoreCase(command)) {
                    receiveFile(dis);
                } else if ("DOWNLOAD".equalsIgnoreCase(command)) {
                    sendFile(dis, dos);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void receiveFile(DataInputStream dis) throws IOException {
            String fileName = dis.readUTF();
            long fileSize = dis.readLong();
            File outputFile = new File(STORAGE_DIR + "/" + fileName);

            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalRead = 0;
                while (totalRead < fileSize && (bytesRead = dis.read(buffer)) > 0) {
                    fos.write(buffer, 0, bytesRead);
                    totalRead += bytesRead;
                }
                System.out.println("File received: " + fileName);
            }
        }

        private void sendFile(DataInputStream dis, DataOutputStream dos) throws IOException {
            String fileName = dis.readUTF();
            File file = new File(STORAGE_DIR + "/" + fileName);
            if (!file.exists()) {
                dos.writeUTF("ERROR: File not found");
                return;
            }
            dos.writeUTF("OK");
            dos.writeLong(file.length());

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) > 0) {
                    dos.write(buffer, 0, bytesRead);
                }
            }
            System.out.println("File sent: " + fileName);
        }
    }
}
