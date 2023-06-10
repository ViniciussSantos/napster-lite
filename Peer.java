import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Peer {

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("Usage: java Peer <IpAddress> <port> <folderPath>");
            System.exit(1);
        }

        String IpAddress = args[0];
        int port = Integer.parseInt(args[1]);
        String folderPath = args[2];

        FileHelper.createFolderIfNotExists(folderPath);
        String[] files = FileHelper.getFilesInFolder(folderPath);

        // Start the server thread to listen for incoming connections from other peers
        new ServerThread(port, folderPath).start();

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("Enter command: ");
            String command = scanner.nextLine();
            String[] commandParts = command.split(" ");

            switch (commandParts[0].trim()) {
                case "SEARCH":
                    System.out.println("NOT IMPLEMENTED YET");
                    break;
                case "DOWNLOAD":
                    if (commandParts.length != 3) {
                        System.out.println("Invalid command");
                        break;
                    }
                    String peerPort = commandParts[1].split(":")[1].trim();
                    String fileName = commandParts[2].trim();
                    new ClientThread(Integer.parseInt(peerPort), folderPath, fileName).start();
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
                System.out.println("Server listening on port " + serverSocket.getLocalPort());

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
                File file = new File(filePath + "/" + fileName);
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
                writer.println("DOWNLOAD " + fileName);

                File file = new File(folderPath + "/" + fileName);

                FileOutputStream fos = new FileOutputStream(file);
                InputStream is = socket.getInputStream();

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }

                fos.close();
                is.close();

                System.out.println("File received: " + folderPath);
                socket.close();
            } catch (IOException e) {
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
        public static String[] getFilesInFolder(String folderPath) {
            File folder = new File(folderPath);
            File[] files = folder.listFiles();

            if (files == null) {
                return new String[0];
            }

            String[] fileNames = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    fileNames[i] = files[i].getName();
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
            } else {
                System.out.println("Folder already exists: " + folderPath);
            }
        }
    }


}



