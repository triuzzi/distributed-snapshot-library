package it.polimi.ds.ricciosorrentinotriuzzi.snaplib;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.ServerNotActiveException;


public interface SnapInt<S extends Serializable, M extends Serializable> extends Remote {
    void printMsg() throws RemoteException, ServerNotActiveException;
    //void saveSnapshot(Snapshot<S, M> snap);
    //Snapshot<S, M> readSnapshot(String id);
    void startSnapshot(String id) throws RemoteException;
    //void initiateSnapshot(String id);
    void restore() throws RemoteException;
}
