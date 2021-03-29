package it.polimi.ds.ricciosorrentinotriuzzi.snaptest;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;


public class Main2 {
    public static void main(String[] args) throws Exception {
        System.out.println("\nStarting server...");
        XMLConfiguration config = new XMLConfiguration("config.xml");
        config.setExpressionEngine(new XPathExpressionEngine());
        System.setProperty("java.rmi.server.hostname", config.getString("host"));
        NodeImpl self = new NodeImpl(config);
        System.out.println("Server ready\n");


/*
        Thread.sleep(10_000);
        self.connectTo("5.89.125.34",1099,"Sbi", true);
        Thread.sleep(3000);
        self.connectTo("151.70.145.224",1099,"Lele", true);
        Thread.sleep(10_000);
        self.disconnectFrom("151.70.145.224",1099, true);
        Thread.sleep(1000);
        self.disconnectFrom("5.89.125.34",1099, true);

        //connect che cerca se già c'è
        //A OGNI CONNECT/DIS VA FATTO PARTIRE LO SNAP
        //file con bit flag per detect il crash
        //metodo di appoggio della lib di "cambio del network"
        // esempio del map reduce, con il recover del codice già eseguito
        // NFT con vendita dei token che viaggiano nella rete. Se uno si vuole disconnettere cede tutti i ssuoi token
        //fai funzione che cambia le connessioni che fa partire lo snap in un nuovo thread
        //premi un tasto per fare lo snapshot


        /*if (self.getName().equals("Vinceee")) {
            Thread.sleep(5_000);
            System.out.println("Avvio snap con balance: "+self.getState().getBalance());
            self.initiateSnapshot();
            Thread.sleep(30_000);
            System.out.println("Inizio restore");
            //self.restore();
            Thread.sleep(20_000);
            System.out.println("My final balance: "+self.getState().getBalance());
        }*/

        self.safeExit();
    }
}


/*
    Registry registry = LocateRegistry.createRegistry(1099);
    NodeImpl self = new NodeImpl(config);
    PublicInt stub = (PublicInt) UnicastRemoteObject.exportObject(self, 1099);
    registry.bind("PublicInt", stub);

    PublicInt remoteNode = (PublicInt) LocateRegistry
            .getRegistry("151.75.54.217", 1099)
            .lookup("PublicInt");
    System.out.println("Connection established");
    remoteNode.increase(100);
    System.out.println("Increase called\n");
*/
//Naming.bind("rmi://localhost:1099/NodeInt",stub);
//NodeInt remint = (NodeInt) Naming.lookup("rmi://93.148.117.106:1099/NodeInt")
