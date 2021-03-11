package it.polimi.ds.ricciosorrentinotriuzzi.snaptest;

import it.polimi.ds.ricciosorrentinotriuzzi.snaplib.SnapLib;
import it.polimi.ds.ricciosorrentinotriuzzi.snaplib.Snapshot;

import java.lang.reflect.Method;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("\nStarting server...");
        Node self = new Node();
        NodeInt stub = (NodeInt) UnicastRemoteObject.exportObject(self, 33330);
        Registry registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
        registry.bind("NodeInt", stub);
        System.out.println("Server ready\n");

        SnapLib snaplib = new SnapLib(registry);


        // TEST CONNESSIONE A SE STESSO
        System.out.println("\nConnecting to B...");
        self.nodeB = (NodeInt) LocateRegistry
                .getRegistry("localhost",Registry.REGISTRY_PORT)
                .lookup("NodeInt");
        System.out.println("Connection to B established");
        System.out.println("Call to B's whoami method\n");
        self.nodeB.whoami();


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
