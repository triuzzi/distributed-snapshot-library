package it.polimi.ds.ricciosorrentinotriuzzi.snaptest;

import it.polimi.ds.ricciosorrentinotriuzzi.snaplib.ConnInt;
import org.apache.commons.configuration.XMLConfiguration;

import java.io.*;
import java.rmi.registry.LocateRegistry;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        //System.setOut(new PrintStream(new FileOutputStream(new File("out.log"))));
        System.err.println("\nStarting server...");
        XMLConfiguration config = new XMLConfiguration("config.xml");
        System.setProperty("java.rmi.server.hostname", config.getString("host"));
        Node self = new Node(config);
        System.err.println("Server ready\n");

        Thread.sleep(2500);
        System.err.println("I miei clienti:");
        for (String name: self.getState().getLedger().keySet())
            System.err.println(name + ", balance " + self.getState().getLedger().get(name));
        Scanner scan = new Scanner(System.in);
        while (true) {
            System.err.println("\n\n" +
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
                System.err.println("\nInserisci il tuo identificativo");
                String customer = scan.next();
                System.err.println("Inserisci il nome della banca a cui è indirizzato il bonifico");
                String bank = scan.next();
                System.err.println("Inserisci l'identificativo del destinatario");
                String receiver = scan.next();
                System.err.println("Inserisci la somma da trasferire");
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
                System.err.println("\nInserisci il valore in secondi");
                self.sleepSnapshot = Integer.parseInt(scan.next())*1000;
            } else if (selection.equalsIgnoreCase("2")){
                System.err.println("\nInserisci il valore in secondi");
                self.sleepRestore = Integer.parseInt(scan.next())*1000;
            } else if (selection.equalsIgnoreCase("c")){
                System.err.println("\nInserisci l'indirizzo della banca a cui connetterti");
                String bankHost = scan.next();
                System.err.println("Inserisci la sua porta remota");
                Integer bankPort = Integer.parseInt(scan.next());
                System.err.println("Inserisci il nome della banca");
                String bankName = scan.next();
                self.connectTo(bankHost,bankPort,bankName,true);
            } else if (selection.equalsIgnoreCase("cc")){
                System.err.println("\nConnessioni in ingresso:");
                for (ConnInt c : self.getInConn())
                    System.err.println(c.getName()+", "+c.getHost()+":"+c.getPort());
                System.err.println("Connessioni in uscita:");
                for (ConnInt c : self.getOutConn())
                    System.err.println(c.getName()+", "+c.getHost()+":"+c.getPort());
            } else if (selection.equalsIgnoreCase("s")){
                System.err.println("\nConti aggiornati");
                for (String name : self.getState().getLedger().keySet())
                    System.err.println(name + " with balance " + self.getState().getLedger().get(name));
                System.err.println("\n");
            } else if (selection.equalsIgnoreCase("x")){
                System.err.println("Inserisci il nome della banca a cui trasferire i clienti");
                String bank = scan.next();
                for (ConnInt connection : self.getOutConn()) {
                    if (connection.getName().equalsIgnoreCase(bank)) {
                        try {
                            PublicInt receiverBank = (PublicInt) LocateRegistry
                                    .getRegistry(connection.getHost(), connection.getPort())
                                    .lookup("PublicInt");
                            System.err.println("Trasferisco i clienti");
                            receiverBank.transferLedger(self.getState().getLedger());
                            self.leaveNetwork();
                            self.safeExit();
                            System.exit(1);
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.err.println("La banca " + bank + " non è disponibile. Non posso chiudere la filiale");
                        }
                    }
                }
            }
        }

    }
}

//Naming.bind("rmi://localhost:1099/NodeInt",stub);
//NodeInt remint = (NodeInt) Naming.lookup("rmi://93.148.117.106:1099/NodeInt")
