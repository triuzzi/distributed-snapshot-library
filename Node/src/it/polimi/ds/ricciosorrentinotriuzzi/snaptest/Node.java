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
    private Set<Connection> connections;

    public Node() {
        connections = new HashSet<>();
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


    public void saveConnections(Set<Connection> connections, String fileName) {
        try (ObjectOutputStream objectOut = new ObjectOutputStream(new FileOutputStream(fileName))) {
            objectOut.writeObject(connections);
        } catch (Exception e) {
            System.out.println("Raised exception while writing connections on file: " + e.toString());
            e.printStackTrace();
        }
    }

    public Set<Connection> readConnections(String fileName) {
        Set<Connection> connections = null;
        try (ObjectInputStream objectOut = new ObjectInputStream(new FileInputStream(fileName))) {
            connections = (Set<Connection>) objectOut.readObject();
        } catch (Exception e) {
            System.out.println("Raised exception while reading connections from file: " + e.toString());
            e.printStackTrace();
        }
        return connections;
    }
}