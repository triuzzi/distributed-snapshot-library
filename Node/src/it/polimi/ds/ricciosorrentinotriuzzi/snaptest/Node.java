package it.polimi.ds.ricciosorrentinotriuzzi.snaptest;


import org.apache.commons.configuration.*;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.*;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.util.*;

public class Node implements NodeInt, Serializable {
    NodeInt nodeB; //CAMBIA CON INTERF NODO B
    private String name;
    private String host;
    private Set<Connection> inConn;
    private Set<Connection> outConn;


    public Node() throws ConfigurationException {
        inConn = new HashSet<>();
        outConn = new HashSet<>();
        XMLConfiguration config = new XMLConfiguration("config.xml");
        name = config.getString("myself.name");
        host = config.getString("myself.host");
        System.setProperty("java.rmi.server.hostname",host);
        List<HierarchicalConfiguration> incomingConn =  config.configurationsAt("incoming.conn");
        for (HierarchicalConfiguration hc : incomingConn) {
            inConn.add(new Connection(hc.getString("host"),hc.getString("name")));
        }
        List<HierarchicalConfiguration> outgoingConn =  config.configurationsAt("outgoing.conn");
        for (HierarchicalConfiguration hc : outgoingConn) {
            outConn.add(new Connection(hc.getString("host"),hc.getInt("port"),hc.getString("name")));
        }
    }

    public NodeInt getNodeB() {
        return nodeB;
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }

    public Set<Connection> getInConn() {
        return inConn;
    }

    public Set<Connection> getOutConn() {
        return outConn;
    }

    @Override
    public void whoami() throws RemoteException {
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            System.out.println(
                    "\nWhoami\nAccording to InetAddress:\n"+
                    "I am node "+inetAddress.getHostName()+" with IP "+inetAddress.getHostAddress()+
                    "\nAccording to my config:\n"+
                    "I am node "+name+" with IP "+host
            );
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
        System.out.println(toPrint);
    }


    public void saveConnections(String fileName) {
        try (ObjectOutputStream objectOut = new ObjectOutputStream(new FileOutputStream(fileName))) {
            objectOut.writeObject(inConn);
            objectOut.writeObject(outConn);
        } catch (Exception e) {
            System.out.println("Raised exception while writing connections on file: " + e.toString());
            e.printStackTrace();
        }
    }

    public void addConnection(Connection c){
        if (c.isOutgoing()) {
            outConn.add(c);
        } else {
            inConn.add(c);
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
}