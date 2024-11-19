import java.io.*;
import java.net.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;

public class FileClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(FileClient::createGUI);
    }

    private static void createGUI() {
        JFrame frame = new JFrame("File Transfer Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);

        JProgressBar progressBar = new JProgressBar(0, 100);
        DefaultTableModel model = new DefaultTableModel(new String[]{"File Name", "Action", "Progress", "Status"}, 0);
        JTable table = new JTable(model);

        JButton uploadButton = new JButton("Upload File");
        JButton downloadButton = new JButton("Download File");
        JPanel dragAndDropPanel = createDragAndDropPanel(progressBar, model);

        uploadButton.addActionListener(e -> uploadFile(progressBar, model));
        downloadButton.addActionListener(e -> downloadFile(progressBar, model));

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.add(uploadButton);
        buttonsPanel.add(downloadButton);

        frame.setLayout(new BorderLayout());
        frame.add(dragAndDropPanel, BorderLayout.NORTH);
        frame.add(new JScrollPane(table), BorderLayout.CENTER);
        frame.add(progressBar, BorderLayout.SOUTH);
        frame.add(buttonsPanel, BorderLayout.PAGE_END);

        frame.setVisible(true);
    }

    private static JPanel createDragAndDropPanel(JProgressBar progressBar, DefaultTableModel model) {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Drag and Drop Files Here"));
        panel.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                try {
                    List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    for (File file : files) {
                        uploadFile(file, progressBar, model);
                    }
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        });
        return panel;
    }

    private static void uploadFile(JProgressBar progressBar, DefaultTableModel model) {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            uploadFile(file, progressBar, model);
        }
    }

    private static void uploadFile(File file, JProgressBar progressBar, DefaultTableModel model) {
        new Thread(() -> {
            int rowIndex = model.getRowCount();
            model.addRow(new Object[]{file.getName(), "Upload", "0%", "In Progress"});
            try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                 DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                 FileInputStream fis = new FileInputStream(file)) {

                dos.writeUTF("UPLOAD");
                dos.writeUTF(file.getName());
                dos.writeLong(file.length());

                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalSent = 0;
                while ((bytesRead = fis.read(buffer)) > 0) {
                    dos.write(buffer, 0, bytesRead);
                    totalSent += bytesRead;
                    int progress = (int) ((totalSent * 100) / file.length());
                    progressBar.setValue(progress);
                    model.setValueAt(progress + "%", rowIndex, 2);
                }
                model.setValueAt("Completed", rowIndex, 3);
                JOptionPane.showMessageDialog(null, "File uploaded successfully!");
            } catch (IOException e) {
                e.printStackTrace();
                model.setValueAt("Failed", rowIndex, 3);
            }
        }).start();
    }

    private static void downloadFile(JProgressBar progressBar, DefaultTableModel model) {
        String fileName = JOptionPane.showInputDialog("Enter the file name to download:");
        if (fileName == null || fileName.trim().isEmpty()) return;

        JFileChooser folderChooser = new JFileChooser();
        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = folderChooser.showOpenDialog(null);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File selectedFolder = folderChooser.getSelectedFile();
        new Thread(() -> {
            int rowIndex = model.getRowCount();
            model.addRow(new Object[]{fileName, "Download", "0%", "In Progress"});
            try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                 DataInputStream dis = new DataInputStream(socket.getInputStream());
                 DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

                dos.writeUTF("DOWNLOAD");
                dos.writeUTF(fileName);

                String response = dis.readUTF();
                if (!"OK".equals(response)) {
                    JOptionPane.showMessageDialog(null, "File not found on server.");
                    model.setValueAt("Failed", rowIndex, 3);
                    return;
                }

                long fileSize = dis.readLong();
                File outputFile = new File(selectedFolder, fileName);

                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    long totalReceived = 0;
                    while ((totalReceived < fileSize) && (bytesRead = dis.read(buffer)) > 0) {
                        fos.write(buffer, 0, bytesRead);
                        totalReceived += bytesRead;
                        int progress = (int) ((totalReceived * 100) / fileSize);
                        progressBar.setValue(progress);
                        model.setValueAt(progress + "%", rowIndex, 2);
                    }
                    model.setValueAt("Completed", rowIndex, 3);
                    JOptionPane.showMessageDialog(null, "File downloaded successfully!");
                }
            } catch (IOException e) {
                e.printStackTrace();
                model.setValueAt("Failed", rowIndex, 3);
            }
        }).start();
    }
}
