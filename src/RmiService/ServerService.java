package src.RmiService;

import java.rmi.Remote;
import java.util.Vector;

public interface ServerService extends Remote {

    /**
     * Faz a função da requisição JOIN
     *
     * @param ipAddress
     * @param port
     * @param files
     * @return
     * @throws java.rmi.RemoteException
     */
    public String registerPeer(String ipAddress, String port, Vector<String> files) throws java.rmi.RemoteException;

    /**
     * remove um peer da lista de peers
     *
     * @param ipAddress
     * @param port
     * @throws java.rmi.RemoteException
     */
    public void unregisterPeer(String ipAddress, String port) throws java.rmi.RemoteException;

    /**
     * Faz a função da requisição UPDATE
     *
     * @param ipAddress
     * @param port
     * @param files
     * @return
     * @throws java.rmi.RemoteException
     */
    public String updateFiles(String ipAddress, String port, Vector<String> files) throws java.rmi.RemoteException;

    /**
     * Faz a função da requisição SEARCH
     *
     * @param ipAddress
     * @param port
     * @param fileName
     * @return
     * @throws java.rmi.RemoteException
     */
    public String[] searchFile(String ipAddress, String port, String fileName) throws java.rmi.RemoteException;


}
