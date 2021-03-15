package it.polimi.ds.ricciosorrentinotriuzzi.snaplib;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public interface NodeInt {
    int getPort();
    String getHost();
    String getName();
}
