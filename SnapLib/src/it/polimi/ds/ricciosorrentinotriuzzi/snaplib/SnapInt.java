package it.polimi.ds.ricciosorrentinotriuzzi.snaplib;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.ServerNotActiveException;


public interface SnapInt<S extends Serializable, M extends Serializable> extends Remote {
    void printMsg() throws RemoteException, ServerNotActiveException;
    void saveSnapshot(Snapshot<S, M> snap) throws SnapEx;
    Snapshot<S, M> readSnapshot(String id) throws SnapEx;
    void startSnapshot(@NotNull String id) throws SnapEx;
    void initiateSnapshot() throws SnapEx;
}
