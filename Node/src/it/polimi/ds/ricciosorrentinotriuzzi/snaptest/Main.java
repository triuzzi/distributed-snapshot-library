package it.polimi.ds.ricciosorrentinotriuzzi.snaptest;

import it.polimi.ds.ricciosorrentinotriuzzi.snaplib.ConnInt;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

import java.rmi.registry.LocateRegistry;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {

        System.out.println("\nStarting server...");
        XMLConfiguration config = new XMLConfiguration("config.xml");
        System.setProperty("java.rmi.server.hostname", config.getString("host"));
        Scanner scan = new Scanner(System.in);
        Node self = new Node(config);

        System.out.println("Server ready\n");

        //CAPIRE NFT, da regalare ai propri outgoing
        XMLConfiguration ledgers = new XMLConfiguration("ledgers.xml");

        List<HierarchicalConfiguration> users =  ledgers.configurationsAt("user");
        for (HierarchicalConfiguration hc : users) {
            self.getState().newCustomer(hc.getString("name"),hc.getInt("balance"));
        }

        for (String name: self.getState().getLedger().keySet()) {
            System.out.println("User: " + name + " Balance " + self.getState().getLedger().get(name));
        }


        String exitCondition = "N";
        while (!exitCondition.equalsIgnoreCase("y")) {
            System.out.println("Inserisci il tuo identificativo");
            String customer = scan.next();

            System.out.println("Inserisci il nome della banca a cui è indirizzato il bonifico");
            String bank = scan.next();

            System.out.println("Inserisci l'identificativo del destinatario");
            String receiver = scan.next();

            System.out.println("Inserisci la somma da trasferire");
            Integer toTransfer = Integer.valueOf(scan.next());
            PublicInt recieverBank = null;

            self.transfer(customer, bank, receiver, toTransfer);

            System.out.println("Conti aggiornati");
            for (String name: self.getState().getLedger().keySet()) {
                System.out.println("User: " + name + " Balance " + self.getState().getLedger().get(name));
            }

            do {
                System.out.println("Vuoi chiudere l'applicazione? Y/N");
                exitCondition = scan.next(); //qui il programma attende l'immissione dei dati
            } while (!exitCondition.equalsIgnoreCase("y") && !exitCondition.equalsIgnoreCase("n"));


        }
        self.safeExit();


        //chi sei? Seleziona il tuo nome.
        //che vuoi fare? Prelevare, versare o trasferire?

            /* Problema: si viola la consistenza del sistema, non è più a somma costante
            //prelievo: togli sia dal conto che dal balance
            //versamento: aggiungi a entrambi
            */

        //trasferimento: Dimmi la banca a cui vuoi traferire. Dimmi il cliente a cui vuoi trasferire. Dimmmi la somma che vuoi trasferire.

        //System.out.println("Mi connetto a Vincenzo");

        /*Thread.sleep(10000);
        self.connectTo("87.20.154.215",1099,"Gianc", true);
        Thread.sleep(3000);
        self.connectTo("87.20.154.215",1099,"Gianc", true);
        self.connectTo("151.70.145.224",1099,"Lele", true);
        Thread.sleep(10000);
        self.disconnectFrom("87.20.154.215",1099, true);
        self.disconnectFrom("151.70.145.224",1099, true);*/


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

        //Thread.sleep(45_000);

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
