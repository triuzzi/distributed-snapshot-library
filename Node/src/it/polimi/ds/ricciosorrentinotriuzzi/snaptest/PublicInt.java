package it.polimi.ds.ricciosorrentinotriuzzi.snaptest;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface PublicInt extends Remote {
    void printStr(String toPrint) throws RemoteException;
    void whoami() throws RemoteException;
}