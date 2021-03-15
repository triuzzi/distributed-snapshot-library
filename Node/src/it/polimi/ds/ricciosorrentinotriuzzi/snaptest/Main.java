package it.polimi.ds.ricciosorrentinotriuzzi.snaptest;

import it.polimi.ds.ricciosorrentinotriuzzi.snaplib.SnapLib;
import it.polimi.ds.ricciosorrentinotriuzzi.snaplib.Snapshot;

import java.lang.reflect.Method;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Main {
    public static void main(String[] args) {
        try {
            //System.setProperty("java.rmi.server.hostname","PUBLIC_IP");
            //OPPURE
            //java -Djava.rmi.server.hostname="PUBLIC_IP" -jar Node.jar
            System.out.println("\nStarting server...");
            Node self = new Node();
            NodeInt stub = (NodeInt) UnicastRemoteObject.exportObject(self, 1099);
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.bind("NodeInt", stub);
            //Naming.bind("rmi://localhost:1099/NodeInt",stub);
            System.out.println("Server ready\n");


            SnapLib<Node,Message> snaplib = new SnapLib(registry, self.getInConn(), self.getOutConn(), self);

            Thread.sleep(10000);


            // TEST CONNESSIONE AI NODI OUTGOING
            for (Connection c : self.getOutConn()) {
                System.out.println("\nConnecting to "+c.getName()+"...");
                self.nodeB = (NodeInt) LocateRegistry
                        .getRegistry(c.getHost(), c.getPort())
                        .lookup("NodeInt");
                System.out.println("Connection to "+c.getName()+" established");
                System.out.println("Call to "+c.getName()+"'s whoami method");
                self.nodeB.whoami();
                System.out.println("Whoami of "+c.getName()+" called\n");
            }



            // MINI TEST SNAPSHOT
            Snapshot<Node, Message> snapshot = new Snapshot("snap1", self);
            Message message1 = new Message("printStr", new Class<?>[]{String.class}, new String[]{"stringaDiProva"});
            snapshot.addMessage(message1);
            snaplib.saveSnapshot(snapshot);
            snapshot = snaplib.readSnapshot(snapshot.getId());
            for (Message message : snapshot.getMessages()) {
                Method method = Node.class.getMethod(message.getMethodName(), message.getParameterTypes());
                method.invoke(Node.class.getDeclaredConstructor().newInstance(), message.getParameters());
            }


            // MINI TEST CONNECTIONS
            Connection c1 = new Connection(true, "testHost", 3306, "IntTest");
            self.addConnection(c1);
            self.saveConnections("connections.cts");
            self.readConnections("connections.cts");


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
