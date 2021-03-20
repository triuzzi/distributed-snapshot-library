package it.polimi.ds.ricciosorrentinotriuzzi.snaptest;

import it.polimi.ds.ricciosorrentinotriuzzi.snaplib.*;
import org.apache.commons.configuration.XMLConfiguration;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Main2 {
    public static void main(String[] args) throws Exception {
        System.out.println("\nStarting server...");
        XMLConfiguration config = new XMLConfiguration("config.xml");
        System.setProperty("java.rmi.server.hostname",config.getString("myself.host"));
/*
        Registry registry = LocateRegistry.createRegistry(1099);

        NodeImpl self = new NodeImpl(config);
        PublicInt stub = (PublicInt) UnicastRemoteObject.exportObject(self, 1099);
        registry.bind("PublicInt", stub);

        SnapLib<State, Message> snapLib = new SnapLib<State, Message>(registry, self);
*/
        System.out.println("Server ready\n");

        NodeImpl self = new NodeImpl(config);
        if (self.getName().equals("Vince")) {
            Thread.sleep(3000);
            System.out.println("Avvio snap!");
            self.initiateSnapshot();
        }

        /*
        Thread.sleep(8000);
        if (self.getName().equals("Vince")) {
            snapLib.restore();
        }
        */



    }
}

//Naming.bind("rmi://localhost:1099/NodeInt",stub);
//NodeInt remint = (NodeInt) Naming.lookup("rmi://93.148.117.106:1099/NodeInt")
