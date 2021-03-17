package it.polimi.ds.ricciosorrentinotriuzzi.snaplib;

import java.io.Serializable;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public abstract class Node<S extends Serializable, M extends Serializable> {
    private String host;
    private int port;
    private String name;

    public abstract S getState();
    public abstract void restoreSnapshot(Snapshot<S,M> snapshot);

    public Node(String host, int port, String name) {
        this.host = host;
        this.port = port;
        this.name = name;
    }

    public int getPort() {return port;}
    public String getHost() {return host;}
    public String getName() {return name;}
}
