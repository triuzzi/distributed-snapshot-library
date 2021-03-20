package it.polimi.ds.ricciosorrentinotriuzzi.snaplib;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface SnapInt extends Remote {
    void startSnapshot(String id) throws RemoteException;
    void restore() throws RemoteException;
}
