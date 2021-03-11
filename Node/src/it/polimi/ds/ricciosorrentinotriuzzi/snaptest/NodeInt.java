package it.polimi.ds.ricciosorrentinotriuzzi.snaptest;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface NodeInt extends Remote {
    void whoami() throws RemoteException;
}
