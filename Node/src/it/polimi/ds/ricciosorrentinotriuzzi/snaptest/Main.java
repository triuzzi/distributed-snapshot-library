package it.polimi.ds.ricciosorrentinotriuzzi.snaptest;

import it.polimi.ds.ricciosorrentinotriuzzi.snaplib.ConnInt;
import org.apache.commons.configuration.XMLConfiguration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.rmi.registry.LocateRegistry;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(new FileOutputStream(new File("out.log"))));
        System.out.println("\nStarting server...");
        XMLConfiguration config = new XMLConfiguration("config.xml");
        System.setProperty("java.rmi.server.hostname", config.getString("host"));
        Node self = new Node(config);
        System.out.println("Server ready\n");

        Thread.sleep(2500);
        System.out.println("I miei clienti:");
        for (String name: self.getState().getLedger().keySet())
            System.out.println(name + ", balance " + self.getState().getLedger().get(name));
        Scanner scan = new Scanner(System.in);
        while (true) {
            System.out.println("\n\n" +
                    "Cosa vuoi fare?\n" +
                    " - Snapshot -> premi A \n" +
                    " - Bonifico generico -> premi B \n" +
                    " - Bonifico a Emanuele da 2€ -> premi E \n" +
                    " - Bonifico a Giancarlo da 3€ -> premi G \n" +
                    " - Bonifico a Vincenzo da 4€ -> premi V \n" +
                    " - Connessione a un nuovo nodo in uscita -> premi C \n" +
                    " - Stampa di tutte le connessioni -> premi CC \n" +
                    " - Saldo di tutti i client -> premi S \n" +
                    " - Modifica sleep snapshot -> premi 1 \n" +
                    " - Modifica sleep restore -> premi 2 \n" +
                    " - Chiudi la filiale -> premi X \n" +
                    "La tua scelta:");
            String selection = scan.next();

            if (selection.equalsIgnoreCase("b")) {
                System.out.println("\nInserisci il tuo identificativo");
                String customer = scan.next();
                System.out.println("Inserisci il nome della banca a cui è indirizzato il bonifico");
                String bank = scan.next();
                System.out.println("Inserisci l'identificativo del destinatario");
                String receiver = scan.next();
                System.out.println("Inserisci la somma da trasferire");
                Integer toTransfer = Integer.parseInt(scan.next());
                self.transferMoney(customer, bank, receiver, toTransfer);
            } else if (selection.equalsIgnoreCase("a")){
                self.initiateSnapshot();
            } else if (selection.equalsIgnoreCase("v")){
                self.transferMoney(self.getDefaultCustomer(), "Intesa", "Vincenzo", 4);
            } else if (selection.equalsIgnoreCase("e")){
                self.transferMoney(self.getDefaultCustomer(), "Hype", "Emanuele", 2);
            } else if (selection.equalsIgnoreCase("g")){
                self.transferMoney(self.getDefaultCustomer(), "Paypal", "Giancarlo", 3);
            } else if (selection.equalsIgnoreCase("1")){
                System.out.println("\nInserisci il valore in secondi");
                self.sleepSnapshot = Integer.parseInt(scan.next())*1000;
            } else if (selection.equalsIgnoreCase("2")){
                System.out.println("\nInserisci il valore in secondi");
                self.sleepRestore = Integer.parseInt(scan.next())*1000;
            } else if (selection.equalsIgnoreCase("c")){
                System.out.println("\nInserisci l'indirizzo della banca a cui connetterti");
                String bankHost = scan.next();
                System.out.println("Inserisci la sua porta remota");
                Integer bankPort = Integer.parseInt(scan.next());
                System.out.println("Inserisci il nome della banca");
                String bankName = scan.next();
                self.connectTo(bankHost,bankPort,bankName,true);
            } else if (selection.equalsIgnoreCase("cc")){
                System.out.println("\nConnessioni in ingresso:");
                for (ConnInt c : self.getInConn())
                    System.out.println(c.getName()+", "+c.getHost()+":"+c.getPort());
                System.out.println("Connessioni in uscita:");
                for (ConnInt c : self.getOutConn())
                    System.out.println(c.getName()+", "+c.getHost()+":"+c.getPort());
            } else if (selection.equalsIgnoreCase("s")){
                System.out.println("\nConti aggiornati");
                for (String name : self.getState().getLedger().keySet())
                    System.out.println(name + " with balance " + self.getState().getLedger().get(name));
                System.out.println("\n");
            } else if (selection.equalsIgnoreCase("x")){
                System.out.println("Inserisci il nome della banca a cui trasferire i clienti");
                String bank = scan.next();
                for (ConnInt connection : self.getOutConn())
                    if (connection.getName().equalsIgnoreCase(bank)) {
                        try {
                            PublicInt receiverBank = (PublicInt) LocateRegistry
                                    .getRegistry(connection.getHost(), connection.getPort())
                                    .lookup("PublicInt");
                            System.out.println("Trasferisco i clienti");
                            receiverBank.transferLedger(self.getState().getLedger());
                            self.leaveNetwork();
                            self.safeExit();
                            System.exit(1);
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("La banca " + bank + " non è disponibile. Non posso chiudere la filiale");
                        }
                    }
            } else {}
        }



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
