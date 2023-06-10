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
        new ServerThread(port).start();

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("Enter command: ");
            String command = scanner.nextLine();
            String[] commandParts = command.split(" ");

            switch (commandParts[0]) {
                case "SEARCH":
                    System.out.println("NOT IMPLEMENTED YET");
                    break;
                case "DOWNLOAD":
                    if (commandParts.length != 2) {
                        System.out.println("Invalid command");
                        break;
                    }
                    String peerPort = commandParts[1];
                    new ClientThread(Integer.parseInt(peerPort)).start();
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

        public ServerThread(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            try {
                ServerSocket serverSocket = new ServerSocket(port);
                System.out.println("Server listening on port " + serverSocket.getLocalPort());

                while (!Thread.currentThread().isInterrupted()) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());

                    new Thread(new ServerHandler(clientSocket)).start();
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

        public ServerHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                //TODO: Add the code to handle the download requests from other peers
                //This is just a sample code to read the message sent by the client
                BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String input = br.readLine();
                System.out.println("Received message from client: " + input);
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

        public ClientThread(int port) {
            this.port = port;
        }

        @Override
        public void run() {

            try {
                String serverAddress = "localhost";
                Socket socket = new Socket(serverAddress, port);
                System.out.println("Connected to server: " + socket.getInetAddress().getHostAddress());

                OutputStream os = socket.getOutputStream();
                DataOutputStream serverWriter = new DataOutputStream(os);
                serverWriter.writeUTF("Hello from client " + socket.getLocalSocketAddress() + "\n");
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



