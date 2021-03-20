package it.polimi.ds.ricciosorrentinotriuzzi.snaplib;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public abstract class Node<S extends Serializable, M extends Serializable> {
    private String host;
    private int port;
    private String name;
    private Set<Node> incomingConnections;
    private Set<Node> outgoingConnections;

    public abstract S getState();
    public abstract void restoreSnapshot(Snapshot<S,M> snapshot);

    public Node(String host, int port, String name) {
        this.host = host;
        this.port = port;
        this.name = name;
        incomingConnections = new HashSet<>();
        outgoingConnections = new HashSet<>();
    }

    public int getPort() {return port;}
    public String getHost() {return host;}
    public String getName() {return name;}

    public Set<Node> getInConn() {
        return incomingConnections;
    }

    public Set<Node> getOutConn() {
        return outgoingConnections;
    }

    public boolean addInConn(Node incoming){
        return incomingConnections.add(incoming);
    }

    public boolean addOutConn(Node outgoing){
        return incomingConnections.add(outgoing);
    }
}
