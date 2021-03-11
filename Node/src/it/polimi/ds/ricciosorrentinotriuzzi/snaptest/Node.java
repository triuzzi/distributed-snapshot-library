package it.polimi.ds.ricciosorrentinotriuzzi.snaptest;

import it.polimi.ds.ricciosorrentinotriuzzi.snaplib.*;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

public class Node implements NodeInt {
    Remote nodeB;
    public Node() {}

    @Override
    public void whoami() throws RemoteException {
        System.out.println("I am node A");
    }

    public static void main(String[] args) {
        try {
            System.out.println("Starting server...");
            Node self = new Node();
            NodeInt stub = (NodeInt) UnicastRemoteObject.exportObject(self, 33330);
            Registry registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
            registry.bind("NodeInt", stub);
            System.out.println("Server ready");

            System.out.println("Connecting to B...");
            self.nodeB = (Remote) LocateRegistry //SOSTITUISCI CON REMOTE
                        .getRegistry("192.168.1.8",Registry.REGISTRY_PORT)
                        .lookup("IntTest");
            System.out.println("Connection to B established");

        } catch (Exception e) {
            System.err.println("\n\nServer exception!\n" + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }
}