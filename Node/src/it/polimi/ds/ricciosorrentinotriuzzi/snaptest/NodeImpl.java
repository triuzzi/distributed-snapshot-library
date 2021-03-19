package it.polimi.ds.ricciosorrentinotriuzzi.snaptest;


import it.polimi.ds.ricciosorrentinotriuzzi.snaplib.*;
import org.apache.commons.configuration.*;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.util.*;

public class NodeImpl extends Node implements PublicInt, Serializable {
    PublicInt nodeB; //CAMBIA CON INTERF NODO B
    private Set<Node> inConn;
    private Set<Node> outConn;
    private State state;


    public NodeImpl(XMLConfiguration config) throws ConfigurationException {
        super(config.getString("myself.host"), config.getInt("myself.port"), config.getString("myself.name"));
        inConn = new HashSet<>();
        outConn = new HashSet<>();
        List<HierarchicalConfiguration> incomingConn =  config.configurationsAt("incoming.conn");
        for (HierarchicalConfiguration hc : incomingConn) {
            inConn.add(new NodeImpl(hc.getString("host"),hc.getInt("port"),hc.getString("name")));
        }
        List<HierarchicalConfiguration> outgoingConn =  config.configurationsAt("outgoing.conn");
        for (HierarchicalConfiguration hc : outgoingConn) {
            outConn.add(new NodeImpl(hc.getString("host"), hc.getInt("port"), hc.getString("name")));
        }
        state = new State();
    }

    public NodeImpl(String host, int port, String name) {
        super(host,port,name);
    }

    @Override
    public Serializable getState() {
        return state;
    }

    @Override
    public void restoreSnapshot(Snapshot snapshot) {
        this.state = (State) snapshot.getState();

    }


    public PublicInt getNodeB() {
        return nodeB;
    }


    public Set<Node> getInConn() {
        return inConn;
    }

    public Set<Node> getOutConn() {
        return outConn;
    }

    public void addConnection(Node c, boolean isOutgoing){
        if (isOutgoing) {
            outConn.add(c);
        } else {
            inConn.add(c);
        }
    }

    @Override
    public void whoami() throws RemoteException {
        try {
            try {
                System.out.println("whoami invoked from " + RemoteServer.getClientHost());
            } catch (ServerNotActiveException e) {
                System.out.println("whoami invoked from local");
            }
            InetAddress inetAddress = InetAddress.getLocalHost();
            System.out.println(
                    "\nWhoami\nAccording to InetAddress:\n"+
                    "I am node "+inetAddress.getHostName()+" with IP "+inetAddress.getHostAddress()+
                    "\nAccording to my config:\n"+
                    "I am node "+getName()+" with IP "+getHost()
            );
            state.setI(100);
            System.out.println("I settato a " + state.getI());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void printStr(String toPrint) throws RemoteException {
        try {
            System.out.println("printStr invoked from "+ RemoteServer.getClientHost());
        } catch (ServerNotActiveException e) {
            System.out.println("printStr autoinvoked");
        }
        System.out.println("Stato attuale: " + state.getI());
        //System.out.println(toPrint);
    }



    /*
    public void saveConnections(String fileName) {
        try (ObjectOutputStream objectOut = new ObjectOutputStream(new FileOutputStream(fileName))) {
            objectOut.writeObject(inConn);
            objectOut.writeObject(outConn);
        } catch (Exception e) {
            System.out.println("Raised exception while writing connections on file: " + e.toString());
            e.printStackTrace();
        }
    }

    public void readConnections(String fileName) {
        try (ObjectInputStream objectOut = new ObjectInputStream(new FileInputStream(fileName))) {
            inConn = (Set<Connection>) objectOut.readObject();
            outConn = (Set<Connection>) objectOut.readObject();
        } catch (Exception e) {
            System.out.println("Raised exception while reading connections from file: " + e.toString());
            e.printStackTrace();
        }
    }
     */
}