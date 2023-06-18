package rmiModel;

import java.rmi.Remote;
import java.util.Vector;

public interface PeerService extends Remote {

    public String registerPeer(String ipAddress, String port, Vector<String> files) throws java.rmi.RemoteException;

    public String updateFiles(String ipAddress, String port, Vector<String> files) throws java.rmi.RemoteException;

    public String[] searchFile(String ipAddress, String port, String fileName) throws java.rmi.RemoteException;


}
