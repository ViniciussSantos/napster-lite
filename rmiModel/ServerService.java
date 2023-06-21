package rmiModel;

import java.rmi.Remote;
import java.util.Vector;

public interface ServerService extends Remote {

    /**
     * JOIN
     *
     * @param ipAddress
     * @param port
     * @param files
     * @return
     * @throws java.rmi.RemoteException
     */
    public String registerPeer(String ipAddress, String port, Vector<String> files) throws java.rmi.RemoteException;

    /**
     * UPDATE
     *
     * @param ipAddress
     * @param port
     * @param files
     * @return
     * @throws java.rmi.RemoteException
     */
    public String updateFiles(String ipAddress, String port, Vector<String> files) throws java.rmi.RemoteException;

    /**
     * SEARCH
     *
     * @param ipAddress
     * @param port
     * @param fileName
     * @return
     * @throws java.rmi.RemoteException
     */
    public String[] searchFile(String ipAddress, String port, String fileName) throws java.rmi.RemoteException;


}
