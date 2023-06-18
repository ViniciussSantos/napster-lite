import rmiModel.PeerServiceImpl;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Servidor {
    public static void main(String[] args) throws Exception {

        LocateRegistry.createRegistry(1099);
        Registry registry = LocateRegistry.getRegistry();
        PeerServiceImpl peerService = new PeerServiceImpl();
        registry.bind("rmi://127.0.0.1/PeerService", peerService);
        System.out.println("Servidor iniciado");
    }
}
