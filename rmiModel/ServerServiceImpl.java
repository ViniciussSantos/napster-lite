package rmiModel;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class ServerServiceImpl extends UnicastRemoteObject implements ServerService {

    ConcurrentHashMap<String, Files> peers = new ConcurrentHashMap<String, Files>();

    public ServerServiceImpl() throws RemoteException {
        super();
    }

    @Override
    public String registerPeer(String ipAddress, String port, Vector<String> files) throws RemoteException {
        if (peers.containsKey(ipAddress + ":" + port)) {
            return "JOIN_ALREADY";
        }

        peers.put(ipAddress + ":" + port, new Files(files));
        System.out.println("Peer " + ipAddress + ":" + port + " adicionado com arquivos " + String.join(", ", files));
        return "JOIN_OK";
    }

    public void unregisterPeer(String ipAddress, String port) throws RemoteException {
        peers.remove(ipAddress + ":" + port);
    }

    @Override
    public String updateFiles(String ipAddress, String port, Vector<String> files) throws RemoteException {
        peers.put(ipAddress + ":" + port, new Files(files));
        return "UPDATE_OK";
    }

    @Override
    public String[] searchFile(String ipAddress, String port, String fileName) throws RemoteException {
        System.out.println("Peer " + ipAddress + ":" + port + " solicitou arquivo " + fileName);

        Vector<String> result = new Vector<String>();

        synchronized (this) {
            peers.forEach((k, v) -> {
                if (v.contains(fileName)) {
                    result.add(k);
                }
            });
        }
        return result.toArray(new String[0]);
    }

    /**
     * Class that represents the files of a peer
     */
    public static class Files {

        Vector<String> files;

        public Files(Vector<String> files) {
            this.files = files;
        }

        public boolean contains(String fileName) {
            return files.contains(fileName);
        }
    }

}
