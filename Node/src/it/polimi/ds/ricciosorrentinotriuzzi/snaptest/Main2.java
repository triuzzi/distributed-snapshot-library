package it.polimi.ds.ricciosorrentinotriuzzi.snaptest;

import org.apache.commons.configuration.XMLConfiguration;

import java.rmi.registry.LocateRegistry;

public class Main2 {
    public static void main(String[] args) throws Exception {
        System.out.println("\nStarting server...");
        XMLConfiguration config = new XMLConfiguration("config.xml");
        System.setProperty("java.rmi.server.hostname",config.getString("myself.host"));
        NodeImpl self = new NodeImpl(config);
        System.out.println("Server ready\n");


        if (self.getName().equals("Vince")) {
            Thread.sleep(3000);
            System.out.println("Avvio snap con balance: "+self.getState().getBalance());
            self.initiateSnapshot();
        }

        if (self.getName().equals("Vince")) {
            Thread.sleep(15000);
            System.out.println("Inizio restore");
            self.restore();
        }

        Thread.sleep(10000);
        PublicInt vinc = (PublicInt) LocateRegistry.getRegistry("5.89.125.34", 1099).lookup("PublicInt");
        System.out.println("Call to Vinc increase method");
        vinc.increase(100);

        Thread.sleep(5000);
        System.out.println(self.getState().getBalance());

    }
}


/*
    Registry registry = LocateRegistry.createRegistry(1099);
    NodeImpl self = new NodeImpl(config);
    PublicInt stub = (PublicInt) UnicastRemoteObject.exportObject(self, 1099);
    registry.bind("PublicInt", stub);
*/
//Naming.bind("rmi://localhost:1099/NodeInt",stub);
//NodeInt remint = (NodeInt) Naming.lookup("rmi://93.148.117.106:1099/NodeInt")
