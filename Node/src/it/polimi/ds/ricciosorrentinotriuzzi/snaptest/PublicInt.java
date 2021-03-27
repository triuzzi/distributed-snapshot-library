package it.polimi.ds.ricciosorrentinotriuzzi.snaptest;

import org.apache.commons.configuration.ConfigurationException;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface PublicInt extends Remote {
    void printStr(String toPrint) throws RemoteException;
    void increase(Integer diff) throws RemoteException;
    void decrease(Integer diff) throws RemoteException;
    void addConn(boolean toOutgoing, String host, int port, String name) throws RemoteException, ConfigurationException;
    void removeConn(boolean fromOutgoing, String host) throws RemoteException, ConfigurationException;

    //void whoami() throws RemoteException;
    /*boolean connect(boolean isOutgoing, String host, int port, String name) throws RemoteException;
    boolean disconnect(boolean isOutgoing, String host, int port, String name) throws RemoteException;*/
}
