package it.polimi.ds.ricciosorrentinotriuzzi.snaplib;

import org.jetbrains.annotations.NotNull;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.ServerNotActiveException;


public interface SnapInt extends Remote {
    void configure(@NotNull Registry r) throws RemoteException, ServerNotActiveException;
    void printMsg() throws RemoteException, ServerNotActiveException;

}
