import rmiModel.ServerService;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Objects;
import java.util.Scanner;
import java.util.Vector;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;

public class Peer {

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("Usage: java Peer <IpAddress> <port> <folderPath>");
            System.exit(1);
        }

        String IpAddress = args[0];
        String port = args[1];
        String folderPath = args[2];

        // Checks to see if the folder exists, and if not, creates it
        FileHelper.createFolderIfNotExists(folderPath);
        // Get files in folder
        Vector<String> files = FileHelper.getFilesInFolder(folderPath);

        // RMI
        Registry registry = LocateRegistry.getRegistry();
        ServerService serverService = (ServerService) registry.lookup("rmi://127.0.0.1/ServerService");

        // Start the server thread to listen for incoming connections from other peers
        new ServerThread(Integer.parseInt(port), folderPath).start();

        // Start the file watcher thread to watch for changes in the folder
        new FileWatcher(folderPath, files, IpAddress, port, serverService).start();

        // CLI (menu interativo)
        Scanner scanner = new Scanner(System.in);
        String lastSearchedFilename = null;
        while (true) {
            System.out.println("Enter command - JOIN, SEARCH OR DOWNLOAD: ");
            String command = scanner.nextLine();
            String[] commandParts = command.split(" ");

            switch (commandParts[0].trim()) {
                case "JOIN":
                    if (commandParts.length != 4) {
                        System.out.println("Invalid command");
                        System.out.println("Usage: JOIN <IpAddress> <port> <folderPath>");
                        break;
                    }
                    if (!Objects.equals(IpAddress, commandParts[1].trim()) || !port.equals(commandParts[2].trim()) || !Objects.equals(folderPath, commandParts[3].trim())) {
                        System.out.println("Comandos passados no JOIN divergem dos passados na inicialização");
                        break;
                    }

                    String response = serverService.registerPeer(IpAddress, port, files);
                    if (response.equals("JOIN_OK")) {
                        System.out.println("Sou Peer" + IpAddress + ":" + port + " com arquivos: " + String.join(", ", files));
                    } else {
                        System.out.println("Error registering peer");
                    }
                    break;
                case "SEARCH":
                    if (commandParts.length != 2) {
                        System.out.println("Invalid command");
                        System.out.println("Usage: SEARCH <fileName>");
                        break;
                    }
                    lastSearchedFilename = commandParts[1].trim();
                    String[] peers = serverService.searchFile(IpAddress, port, lastSearchedFilename);
                    if (peers.length == 0) {
                        System.out.println("Nenhum peer com o arquivo solicitado");
                    } else {
                        System.out.println("Peers com arquivo solicitado: " + String.join(", ", peers));
                    }
                    break;
                case "DOWNLOAD":
                    if (commandParts.length != 2) {
                        System.out.println("Invalid command");
                        System.out.println("Usage: DOWNLOAD <peerIp:peerPort>");
                        break;
                    }

                    if (lastSearchedFilename == null) {
                        System.out.println("Nenhum arquivo foi pesquisado");
                        break;
                    }

                    String peerPort = commandParts[1].split(":")[1].trim();
                    new ClientThread(Integer.parseInt(peerPort), folderPath, lastSearchedFilename).start();
                    break;
                default:
                    System.out.println("Invalid command");
            }

        }


    }

    /**
     * This class represents the server thread that will listen for incoming connections from other peers.
     * When a connection is accepted, a new thread is created to handle the client.
     */
    private static class ServerThread extends Thread {
        private final int port;
        private final String folderPath;


        public ServerThread(int port, String folderPath) {
            this.port = port;
            this.folderPath = folderPath;
        }

        @Override
        public void run() {
            try {
                ServerSocket serverSocket = new ServerSocket(port);

                while (!Thread.currentThread().isInterrupted()) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(new ServerHandler(clientSocket, folderPath)).start();
                }

                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This class handles the connection accepted by the server thread.
     */
    private static class ServerHandler implements Runnable {
        private final Socket clientSocket;
        private final String filePath;


        public ServerHandler(Socket clientSocket, String filePath) {
            this.clientSocket = clientSocket;
            this.filePath = filePath;
        }

        @Override
        public void run() {
            try {

                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);


                String request = reader.readLine();
                String[] requestParts = request.split(" ");

                if (requestParts.length != 2 || !requestParts[0].equals("DOWNLOAD")) {
                    writer.println("Invalid request");
                    clientSocket.close();
                    return;
                }

                String fileName = requestParts[1];
                System.out.println("File name: " + fileName);

                if (!FileHelper.checkIfFileExists(filePath, fileName)) {
                    writer.println("File not found");
                    clientSocket.close();
                    return;
                }
                writer.println("FILE FOUND");
                File file = new File(filePath + FileSystems.getDefault().getSeparator() + fileName);
                FileInputStream fis = new FileInputStream(file);
                OutputStream os = clientSocket.getOutputStream();

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }

                os.flush();
                os.close();
                fis.close();
                clientSocket.close();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * This class represents the client thread that will connect to other peers.
     */
    private static class ClientThread extends Thread {
        private final int port;
        private final String folderPath;
        private final String fileName;

        public ClientThread(int port, String folderPath, String fileName) {
            this.port = port;
            this.folderPath = folderPath;
            this.fileName = fileName;
        }

        @Override
        public void run() {

            try {
                if (FileHelper.checkIfFileExists(folderPath, fileName)) {
                    System.out.println("File already exists in this peer");
                    return;
                }

                String serverAddress = "localhost";
                Socket socket = new Socket(serverAddress, port);
                System.out.println("Connected to server: " + socket.getInetAddress().getHostAddress());

                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                // Send DOWNLOAD request to another peer
                writer.println("DOWNLOAD " + fileName);

                //Prints error message if file is not found
                String line;
                if (!Objects.equals(line = reader.readLine(), "FILE FOUND")) {
                    System.out.println(line);
                    return;
                }

                // Receive file
                File file = new File(folderPath + FileSystems.getDefault().getSeparator() + fileName);

                FileOutputStream fos = new FileOutputStream(file);
                InputStream is = socket.getInputStream();

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }

                fos.close();
                is.close();

                socket.close();
                System.out.println("Arquivo" + fileName + " baixado com sucesso na pasta " + folderPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This class represents the thread that will watch for changes in the folder and notifies the central server.
     */
    public static class FileWatcher extends Thread {

        String path;
        Vector<String> files;

        String ipAddress;
        String port;

        ServerService serverService;

        public FileWatcher(String path, Vector<String> files, String ipAddress, String port, ServerService serverService) {
            this.path = path;
            this.files = files;
            this.ipAddress = ipAddress;
            this.port = port;
            this.serverService = serverService;
        }

        @Override
        public void run() {

            try {
                // Creates a WatchService and registers the logDir below with the ENTRY_CREATE and ENTRY_DELETE events.
                WatchService watchService = FileSystems.getDefault().newWatchService();
                Path path = Paths.get(this.path);
                path.register(watchService, ENTRY_CREATE, ENTRY_DELETE);
                while (true) {
                    WatchKey key;
                    while ((key = watchService.take()) != null) {
                        for (WatchEvent<?> event : ((WatchKey) key).pollEvents()) {
                            // If a new file is created, it is added to the Peer's list of files and the central server is notified.
                            if (event.kind() == ENTRY_CREATE) {
                                Path newPath = ((Path) key.watchable()).resolve((Path) event.context());
                                String fileName = newPath.getFileName().toString();
                                if (!files.contains(fileName)) {
                                    files.add(fileName);
                                    String response = serverService.updateFiles(ipAddress, port, files);
                                    if (!Objects.equals(response, "UPDATE_OK")) {
                                        System.out.println("Error updating files");
                                    }
                                }
                            }
                            // If a file is deleted, it is removed from the Peer's list of files and the central server is notified.
                            if (event.kind() == ENTRY_DELETE) {

                                Path newPath = ((Path) key.watchable()).resolve((Path) event.context());
                                String fileName = newPath.getFileName().toString();
                                files.remove(fileName);
                                String response = serverService.updateFiles(ipAddress, port, files);
                                if (!Objects.equals(response, "UPDATE_OK")) {
                                    System.out.println("Error updating files");
                                }
                            }
                        }
                        key.reset();
                    }
                }

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * This FileHelper class has methods to work files and paths.
     */
    private static class FileHelper {
        /*
         * This method returns the name of files in the specified path.
         */
        public static Vector<String> getFilesInFolder(String folderPath) {
            File folder = new File(folderPath);
            File[] files = folder.listFiles();

            if (files == null) {
                System.out.println("No files in folder: " + folderPath);
                return new Vector<String>();
            }

            Vector<String> fileNames = new Vector<String>();
            for (File file : files) {
                if (file.isFile()) {
                    fileNames.add(file.getName());
                }
            }

            return fileNames;
        }

        /*
         * This method checks if a folder exists and creates it if it doesn't.
         */
        public static void createFolderIfNotExists(String folderPath) {
            File folder = new File(folderPath);
            if (!folder.exists()) {
                if (folder.mkdirs()) {
                    System.out.println("Folder created: " + folderPath);
                } else {
                    System.out.println("Failed to create the folder: " + folderPath);
                }
            }
        }

        /*
         * This method checks if a file exists.
         */

        public static boolean checkIfFileExists(String folderPath, String fileName) {
            return new File(folderPath + FileSystems.getDefault().getSeparator() + fileName).exists();
        }
    }
}
