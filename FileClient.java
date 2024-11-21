import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.*;

public class FileClient {
    // Server connection details
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 42069;
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                createGUI();
            }
        });
    }

    // Method to create the main GUI
    private static void createGUI() {
        final JFrame frame = new JFrame("File Transfer Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);

        // Progress bar to display upload/download progress
        final JProgressBar progressBar = new JProgressBar(0, 100);

        // Table model to display files and their statuses
        final DefaultTableModel model = new DefaultTableModel(
            new String[]{"File Name", "Action", "Progress", "Status"}, 0) {
            @Override
            // Prevent editing of table cells
            public boolean isCellEditable(int row, int column) {
                return false; 
            }
        };

        // Table to display files
        final JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // Allow single row selection

        // Buttons for various actions
        JButton uploadButton = new JButton("Upload File"); //Button to upload files
        JButton downloadButton = new JButton("Download File"); //Button to download files
        JButton refreshButton = new JButton("Refresh File List"); //Button to refresh the list of files
        JButton clearButton = new JButton("Clear Files"); // Button to clear files from the server

        // Panel for drag-and-drop functionality
        JPanel dragAndDropPanel = createDragAndDropPanel(progressBar, model);

        // Action listener for the upload button
        uploadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                uploadFile(progressBar, model);
            }
        });

        // Action listener for the download button
        downloadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow == -1) {
                    JOptionPane.showMessageDialog(frame, "Please select a file to download.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String fileName = (String) model.getValueAt(selectedRow, 0);
                downloadFile(fileName, progressBar, model);
            }
        });

        // Action listener for the refresh button
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateFileList(model);
            }
        });

        // Action listener for the clear button
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int confirm = JOptionPane.showConfirmDialog(
                    frame,
                    "This will delete all files on the server. Are you sure?",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION
                );
                if (confirm == JOptionPane.YES_OPTION) {
                    clearFilesOnServer(model);
                }
            }
        });

        // Panel to hold the buttons
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.add(uploadButton); //Add the upload button to the panel
        buttonsPanel.add(downloadButton); //Add the download button to the panel
        buttonsPanel.add(refreshButton); //Add the refresh button to the panel
        buttonsPanel.add(clearButton); // Add the clear button to the panel

        // Set up the frame layout
        frame.setLayout(new BorderLayout());
        frame.add(dragAndDropPanel, BorderLayout.NORTH);
        frame.add(new JScrollPane(table), BorderLayout.CENTER);
        frame.add(progressBar, BorderLayout.SOUTH);
        frame.add(buttonsPanel, BorderLayout.PAGE_END);

        // Fetch and display the file list when the GUI starts
        updateFileList(model);

        frame.setVisible(true);
    }

    // Method to create the drag-and-drop panel
    private static JPanel createDragAndDropPanel(final JProgressBar progressBar, final DefaultTableModel model) {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Drag and Drop Files Here"));
        panel.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            @SuppressWarnings("unchecked")
            public boolean importData(TransferSupport support) {
                try {
                    // Get the list of files dropped
                    List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    for (File file : files) {
                        if (file.isDirectory()) {
                            // If it's a directory, upload all files inside
                            File[] innerFiles = file.listFiles();
                            if (innerFiles != null) {
                                for (File innerFile : innerFiles) {
                                    uploadFile(innerFile, progressBar, model);
                                }
                            }
                        } else {
                            // Upload the file
                            uploadFile(file, progressBar, model);
                        }
                    }
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        });
        panel.setPreferredSize(new Dimension(600, 100));
        return panel;
    }

    // Method to initiate file upload using a file chooser
    private static void uploadFile(final JProgressBar progressBar, final DefaultTableModel model) {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            uploadFile(file, progressBar, model);
        }
    }

    // Method to upload a specific file
    private static void uploadFile(final File file, final JProgressBar progressBar, final DefaultTableModel model) {
        // Find or add the file in the table and get the row index
        int rowIndex = -1;
        for (int i = 0; i < model.getRowCount(); i++) {
            if (model.getValueAt(i, 0).equals(file.getName())) {
                rowIndex = i;
                break;
            }
        }
        if (rowIndex == -1) {
            // Add new row if file not in table
            model.addRow(new Object[]{file.getName(), "Upload", "0%", "In Progress"});
            rowIndex = model.getRowCount() - 1;
        } else {
            // Update existing row
            model.setValueAt("Upload", rowIndex, 1);
            model.setValueAt("0%", rowIndex, 2);
            model.setValueAt("In Progress", rowIndex, 3);
        }

        final int finalRowIndex = rowIndex;

        // Start a new thread for uploading to prevent GUI freezing
        Thread uploadThread = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean uploadSuccessful = false;
                try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                     DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                     DataInputStream dis = new DataInputStream(socket.getInputStream())) {

                    // Send upload command and file details to the server
                    dos.writeUTF("UPLOAD");
                    dos.writeUTF(file.getName());
                    dos.writeLong(file.length());

                    // Read server response
                    String response = dis.readUTF();
                    if (!"OK".equals(response)) {
                        // Server indicates failure (e.g., file already exists)
                        JOptionPane.showMessageDialog(null, response);
                        model.setValueAt("Failed", finalRowIndex, 3);
                        return;
                    }

                    // Proceed to send the file data
                    try (FileInputStream fis = new FileInputStream(file)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        long totalSent = 0;
                        while ((bytesRead = fis.read(buffer)) > 0) {
                            dos.write(buffer, 0, bytesRead);
                            totalSent += bytesRead;
                            // Update progress
                            final int progress = (int) ((totalSent * 100) / file.length());
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setValue(progress);
                                    model.setValueAt(progress + "%", finalRowIndex, 2);
                                }
                            });
                        }
                    }
                    // Update status to completed
                    model.setValueAt("Completed", finalRowIndex, 3);
                    JOptionPane.showMessageDialog(null, "File uploaded successfully!");
                    uploadSuccessful = true;
                } catch (IOException e) {
                    e.printStackTrace();
                    model.setValueAt("Failed", finalRowIndex, 3);
                } finally {
                    if (uploadSuccessful) {
                        // Refresh the file list only if the upload was successful
                        updateFileList(model);
                    }
                }
            }
        });
        uploadThread.start();
    }

    // Method to download a selected file
    private static void downloadFile(final String fileName, final JProgressBar progressBar, final DefaultTableModel model) {
        // Let the user choose a directory to save the file
        JFileChooser folderChooser = new JFileChooser();
        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = folderChooser.showOpenDialog(null);
        if (result != JFileChooser.APPROVE_OPTION) return;

        final File selectedFolder = folderChooser.getSelectedFile();
        // Start a new thread for downloading
        Thread downloadThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int tempRowIndex = -1;
                // Find the row index of the file in the table
                for (int i = 0; i < model.getRowCount(); i++) {
                    if (model.getValueAt(i, 0).equals(fileName)) {
                        tempRowIndex = i;
                        break;
                    }
                }
                final int rowIndex = tempRowIndex;
                if (rowIndex == -1) {
                    return;
                }
                // Update table to reflect download action
                model.setValueAt("Download", rowIndex, 1);
                model.setValueAt("In Progress", rowIndex, 3);
                try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                     DataInputStream dis = new DataInputStream(socket.getInputStream());
                     DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

                    // Send download command and file name to the server
                    dos.writeUTF("DOWNLOAD");
                    dos.writeUTF(fileName);

                    // Read server response
                    String response = dis.readUTF();
                    if (!"OK".equals(response)) {
                        // Server indicates failure (e.g., file not found)
                        JOptionPane.showMessageDialog(null, "File not found on server.");
                        model.setValueAt("Failed", rowIndex, 3);
                        return;
                    }

                    // Get the file size from the server
                    long fileSize = dis.readLong();
                    File outputFile = new File(selectedFolder, fileName);

                    if (outputFile.exists()) {
                        // File already exists locally
                        JOptionPane.showMessageDialog(null, "File already exists in the directory.", "Error", JOptionPane.ERROR_MESSAGE);
                        model.setValueAt("Failed", rowIndex, 3);
                        return;
                    }

                    // Proceed to receive the file data
                    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        long totalReceived = 0;
                        while ((totalReceived < fileSize) && (bytesRead = dis.read(buffer)) > 0) {
                            fos.write(buffer, 0, bytesRead);
                            totalReceived += bytesRead;
                            // Update progress
                            final int progress = (int) ((totalReceived * 100) / fileSize);
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setValue(progress);
                                    model.setValueAt(progress + "%", rowIndex, 2);
                                }
                            });
                        }
                        // Update status to completed
                        model.setValueAt("Completed", rowIndex, 3);
                        JOptionPane.showMessageDialog(null, "File downloaded successfully!");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    model.setValueAt("Failed", rowIndex, 3);
                }
            }
        });
        downloadThread.start();
    }

    // Method to update the file list from the server
    private static void updateFileList(final DefaultTableModel model) {
        // Use SwingWorker to perform background tasks without freezing the GUI
        SwingWorker<Object[][], Void> worker = new SwingWorker<Object[][], Void>() {
            @Override
            protected Object[][] doInBackground() throws Exception {
                try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                     DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                     DataInputStream dis = new DataInputStream(socket.getInputStream())) {

                    // Send list command to the server
                    dos.writeUTF("LIST");
                    int fileCount = dis.readInt();
                    System.out.println("Received file count: " + fileCount);

                    // Read the list of files from the server
                    Object[][] data = new Object[fileCount][4];
                    for (int i = 0; i < fileCount; i++) {
                        String fileName = dis.readUTF();
                        System.out.println("Received file name: " + fileName);
                        data[i][0] = fileName;
                        data[i][1] = "";
                        data[i][2] = "";
                        data[i][3] = "Available";
                    }
                    return data;
                } catch (IOException e) {
                    e.printStackTrace();
                    throw e;
                }
            }

            @Override
            protected void done() {
                try {
                    Object[][] data = get();
                    // Clear the table and repopulate with new data
                    model.setRowCount(0);
                    for (Object[] row : data) {
                        model.addRow(row);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Failed to refresh file list:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

     
    private static void clearFilesOnServer(final DefaultTableModel model) {
        // Start a new thread to communicate with the server
        new Thread(new Runnable() {
            @Override
            public void run() {
                try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                     DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                     DataInputStream dis = new DataInputStream(socket.getInputStream())) {

                    // Send clear command to the server
                    dos.writeUTF("CLEAR");

                    // Read server response
                    String response = dis.readUTF();
                    if ("OK".equals(response)) {
                        int deletedFiles = dis.readInt();
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                model.setRowCount(0); 
                                JOptionPane.showMessageDialog(null, "Cleared " + deletedFiles + " files from the server.");
                            }
                        });
                    } else {
                        final String errorMsg = "Failed to clear files on the server.";
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                JOptionPane.showMessageDialog(null, errorMsg, "Error", JOptionPane.ERROR_MESSAGE);
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    final String errorMsg = "Error communicating with the server:\n" + e.getMessage();
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            JOptionPane.showMessageDialog(null, errorMsg, "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    });
                }
            }
        }).start();
    }
}
