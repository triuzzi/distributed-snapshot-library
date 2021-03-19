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

        if (config.getString("myself.host").equals("Vince")) {
            Thread.sleep(3000);
        }

        Registry registry = LocateRegistry.createRegistry(1099);

        NodeImpl self = new NodeImpl(config);
        PublicInt stub = (PublicInt) UnicastRemoteObject.exportObject(self, 1099);
        registry.bind("PublicInt", stub);

        SnapLib<State, Message> snapLib = new SnapLib<State, Message>(registry, self.getInConn(), self.getOutConn(), self);


        //Naming.bind("rmi://localhost:1099/NodeInt",stub);
        System.out.println("Server ready\n");

        //Thread.sleep(5000);
        //PublicInt remint = (PublicInt) LocateRegistry.getRegistry("192.168.1.10",1099).lookup(PublicInt.class.getName());
        //SnapInt<State, Message> snapRemInt = (SnapInt<State, Message>) LocateRegistry.getRegistry("192.168.1.10",1099).lookup(SnapInt.class.getName());
        ////remint.printStr("CIAO!");
        System.out.println("Avvio snap!");
        if (self.getName().equals("Vince")) {
            snapLib.startSnapshot(self.getHost());
        }

        Thread.sleep(8000);
        if (self.getName().equals("Vince")) {
            snapLib.restore();
        }
        Thread.sleep(5000);
        System.out.println(((State) self.getState()).getI());

/*

        System.out.println("\nConnecting to B...");
        PublicInt remint = (PublicInt) LocateRegistry.getRegistry("192.168.1.10",1099).lookup(PublicInt.class.getName());
        SnapInt<State, Message> snapRemInt = (SnapInt<State, Message>) LocateRegistry.getRegistry("192.168.1.10",1099).lookup(SnapInt.class.getName());

        System.out.println("Il registro remoto ha queste interfacce");
        for (String s: r.list())
            System.out.println(s);
        NodeInt remint = (NodeInt) r.lookup("NodeInt");
        */

        /*
        //NodeInt remint = (NodeInt) Naming.lookup("rmi://93.148.117.106:1099/NodeInt");
        System.out.println("Connection to B established");
        System.out.println("Start snap");

        snapRemInt.startSnapshot("192.168.1.13");
        System.out.println("Call to B's whoami method");
        remint.whoami();
        System.out.println("Call to B's restore");
        snapRemInt.restore();
        System.out.println("Call to B's printStr");
        remint.printStr("");
        System.out.println("\n");

         */
    }
}
