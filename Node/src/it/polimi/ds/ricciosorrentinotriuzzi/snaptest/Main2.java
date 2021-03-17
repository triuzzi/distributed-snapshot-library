package it.polimi.ds.ricciosorrentinotriuzzi.snaptest;

import java.rmi.registry.LocateRegistry;

public class Main2 {
    public static void main(String[] args) throws Exception {
        System.out.println("\nConnecting to B...");
        PublicInt remint = (PublicInt) LocateRegistry.getRegistry("93.148.117.106",1099).lookup("NodeInt");
        /*
        System.out.println("Il registro remoto ha queste interfacce");
        for (String s: r.list())
            System.out.println(s);
        NodeInt remint = (NodeInt) r.lookup("NodeInt");
        */

        //NodeInt remint = (NodeInt) Naming.lookup("rmi://93.148.117.106:1099/NodeInt");
        System.out.println("Connection to B established");
        System.out.println("Call to B's whoami method");
        remint.whoami();
        System.out.println("\n");
    }
}
