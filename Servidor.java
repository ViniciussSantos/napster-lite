import rmiModel.ServerServiceImpl;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Servidor {
    public static void main(String[] args) throws Exception {

        LocateRegistry.createRegistry(1099);
        Registry registry = LocateRegistry.getRegistry();
        ServerServiceImpl peerService = new ServerServiceImpl();
        registry.bind("rmi://127.0.0.1/ServerService", peerService);
        System.out.println("Servidor iniciado");
    }
}
