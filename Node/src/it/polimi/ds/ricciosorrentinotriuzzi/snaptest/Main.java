package it.polimi.ds.ricciosorrentinotriuzzi.snaptest;

import it.polimi.ds.ricciosorrentinotriuzzi.snaplib.SnapLib;
import it.polimi.ds.ricciosorrentinotriuzzi.snaplib.Snapshot;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        //System.setProperty("java.rmi.server.hostname","192.168.1.100");
        //OPPURE
        //java -Djava.rmi.server.hostname="192.168.1.100" -jar Node.jar

        System.out.println("\nStarting server...");
        Node self = new Node();
        Registry registry = LocateRegistry.createRegistry(1099);
        NodeInt stub = (NodeInt) UnicastRemoteObject.exportObject(self, 1099);
        registry.bind("NodeInt", stub);
        //Naming.bind("rmi://localhost:1099/NodeInt",stub);
        System.out.println("Server ready\n");


        SnapLib snaplib = new SnapLib(registry);

        // TEST CONNESSIONE A SE STESSO
        System.out.println("\nConnecting to myself...");
        self.nodeB = (NodeInt) LocateRegistry
                .getRegistry("localhost",Registry.REGISTRY_PORT)
                .lookup("NodeInt");
        System.out.println("Connection to myself established");
        System.out.println("Call to myself's whoami method");
        self.nodeB.whoami();
        System.out.println("\n");




        // MINI TEST SNAPSHOT
        Snapshot<Node, Message> snapshot = new Snapshot("snap1", self);
        Message message1 = new Message("printStr", new Class<?>[] {String.class}, new String[]{"stringaDiProva"});
        snapshot.addMessage(message1);
        snaplib.saveSnapshot(snapshot);
        snapshot = snaplib.readSnapshot(snapshot.getId());
        for (Message message : snapshot.getMessages()) {
            Method method = Node.class.getMethod(message.getMethodName(), message.getParameterTypes());
            method.invoke(Node.class.getDeclaredConstructor().newInstance(), message.getParameters());
        }


        // MINI TEST CONNECTIONS
        Connection c1 = new Connection("testHost", 3306, "IntTest");
        Set<Connection> connections = new HashSet<>();
        connections.add(c1);
        self.saveConnections(connections, "connections.cts");
        Set<Connection> connections2 = self.readConnections("connections.cts");
        System.out.println(connections2.toString());


    }
}
