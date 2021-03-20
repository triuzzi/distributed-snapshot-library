package it.polimi.ds.ricciosorrentinotriuzzi.snaplib;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public abstract class OldNode <S extends Serializable, M extends Serializable> {
    private String host;
    private int port;
    private String name;
    private Set<ConnInt> incomingConnInts;
    private Set<ConnInt> outgoingConnInts;

    public abstract S getState();
    public abstract void restoreSnapshot(Snapshot<S,M> snapshot);

    public OldNode(String host, int port, String name) {
        this.host = host;
        this.port = port;
        this.name = name;
        incomingConnInts = new HashSet<>();
        outgoingConnInts = new HashSet<>();
    }

    public int getPort() {return port;}
    public String getHost() {return host;}
    public String getName() {return name;}

    public Set<ConnInt> getInConn() {
        return incomingConnInts;
    }

    public Set<ConnInt> getOutConn() {
        return outgoingConnInts;
    }

    public boolean addInConn(ConnInt incoming){
        return incomingConnInts.add(incoming);
    }

    public boolean addOutConn(ConnInt outgoing){
        return outgoingConnInts.add(outgoing);
    }

    /*public boolean saveMessage(){
        try {
            if(RemoteServer.getClientHost() != null)

            return true;
        } catch (ServerNotActiveException e) {
            e.printStackTrace();
        }
        return true;
    }

     */
}


