package it.polimi.ds.ricciosorrentinotriuzzi.snaptest;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface PublicInt extends Remote {
    void printStr(String toPrint) throws RemoteException;
    void transfer(String to, Integer amount) throws RemoteException;
    void addConn(boolean toOutgoing, String host, int port, String name) throws RemoteException;
    void removeConn(boolean fromOutgoing, String host) throws RemoteException;
}
