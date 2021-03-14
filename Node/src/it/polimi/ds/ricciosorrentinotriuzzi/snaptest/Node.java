package it.polimi.ds.ricciosorrentinotriuzzi.snaptest;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.*;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.util.*;

public class Node implements NodeInt, Serializable {
    NodeInt nodeB; //CAMBIA CON INTERF NODO B
    private Set<Connection> inConnections;
    private Set<Connection> outConnections;

    public Node() {
        inConnections = new HashSet<>();
        outConnections = new HashSet<>();
    }

    @Override
    public void whoami() throws RemoteException {
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            System.out.println("I am node "+inetAddress.getHostName()+" with IP "+inetAddress.getHostAddress());
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
            objectOut.writeObject(inConnections);
            objectOut.writeObject(outConnections);
        } catch (Exception e) {
            System.out.println("Raised exception while writing connections on file: " + e.toString());
            e.printStackTrace();
        }
    }

    public void addConnection(Connection c){
        if (c.isOutgoing()) {
            outConnections.add(c);
        } else {
            inConnections.add(c);
        }
    }

    public void readConnections(String fileName) {
        try (ObjectInputStream objectOut = new ObjectInputStream(new FileInputStream(fileName))) {
            inConnections = (Set<Connection>) objectOut.readObject();
            outConnections = (Set<Connection>) objectOut.readObject();
        } catch (Exception e) {
            System.out.println("Raised exception while reading connections from file: " + e.toString());
            e.printStackTrace();
        }
    }
}