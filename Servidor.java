import rmiModel.ServerServiceImpl;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Servidor {
    public static void main(String[] args) throws Exception {

        String ipAddress = "127.0.0.1";
        String port = "1099";

        if (args.length != 2) {
            System.out.println("Usage: java Servidor <ip> <port>");
        }

        if (args.length == 1) {
            ipAddress = args[0];
        }

        if (args.length == 2) {
            ipAddress = args[0];
            port = args[1];
        }

        LocateRegistry.createRegistry(Integer.parseInt(port));
        Registry registry = LocateRegistry.getRegistry();
        ServerServiceImpl serverService = new ServerServiceImpl();
        registry.bind("rmi://" + ipAddress + "/ServerService", serverService);
        System.out.println("Servidor iniciado");
    }
}
