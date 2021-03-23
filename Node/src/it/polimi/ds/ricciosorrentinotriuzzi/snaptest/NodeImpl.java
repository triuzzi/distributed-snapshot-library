package it.polimi.ds.ricciosorrentinotriuzzi.snaptest;


import it.polimi.ds.ricciosorrentinotriuzzi.snaplib.Node;
import it.polimi.ds.ricciosorrentinotriuzzi.snaplib.Snapshot;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.util.List;

public class NodeImpl extends Node<State, Message> implements PublicInt, Serializable {
    private State state;

    public NodeImpl(XMLConfiguration config) throws AlreadyBoundException, RemoteException {
        super(config.getString("myself.host"), config.getInt("myself.port"), config.getString("myself.name"), LocateRegistry.createRegistry(1099));

        List<HierarchicalConfiguration> incomingConn =  config.configurationsAt("incoming.conn");
        for (HierarchicalConfiguration hc : incomingConn) {
            addInConn(new Connection(hc.getString("host"),hc.getInt("port"),hc.getString("name")));
        }
        List<HierarchicalConfiguration> outgoingConn =  config.configurationsAt("outgoing.conn");
        for (HierarchicalConfiguration hc : outgoingConn) {
            addOutConn(new Connection(hc.getString("host"), hc.getInt("port"), hc.getString("name")));
        }
        state = new State();
        //PublicInt n = (PublicInt) UnicastRemoteObject.exportObject(this, 1099);
        LocateRegistry.getRegistry(1099).bind("PublicInt", this);
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void restoreSnapshot(Snapshot<State,Message> snapshot) {
        this.state = snapshot.getState();
        for (Message message : snapshot.getMessages()) {
            try {
                Method method = NodeImpl.class.getMethod(message.getMethodName(), message.getParameterTypes());
                method.invoke(this, message.getParameters());
            } catch (Exception e) { e.printStackTrace(); }
        }
    }


    @Override
    public void increase(Integer diff) {
        if (shouldDiscard()) { System.out.println("Increase "+diff+" scartata"); return; }
        addMessage(new Message("increase", new Class<?>[]{Integer.class}, new Integer[]{diff}));
        getState().increase(diff);
        System.out.println("Increase di "+diff);
        System.out.println("Balance: "+getState().getBalance());
    }

    @Override
    public void decrease(Integer diff) {
        if (shouldDiscard()) { System.out.println("Decrease di "+diff+" scartata"); return; }
        addMessage(new Message("decrease", new Class<?>[]{Integer.class}, new Integer[]{diff}));
        getState().decrease(diff);
        System.out.println("Decrease di "+diff);
        System.out.println("Balance: "+getState().getBalance());
    }





    @Override
    public void whoami() throws RemoteException {
        try {
            try {
                System.out.println("whoami invoked from " + RemoteServer.getClientHost());
            } catch (ServerNotActiveException e) {
                System.out.println("whoami invoked from local");
            }
            InetAddress inetAddress = InetAddress.getLocalHost();
            System.out.println(
                    "\nWhoami\nAccording to InetAddress:\n"+
                            "I am node "+inetAddress.getHostName()+" with IP "+inetAddress.getHostAddress()+
                            "\nAccording to my config:\n"+
                            "I am node "+getName()+" with IP "+getHost()
            );
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void printStr(String toPrint) throws RemoteException {
        try {
            System.out.println("printStr invoked from "+ RemoteServer.getClientHost());
        } catch (ServerNotActiveException e) {
            System.out.println("printStr autoinvoked");
        }
        System.out.println(toPrint);
    }


}











/*
    public void saveConnections(String fileName) {
        try (ObjectOutputStream objectOut = new ObjectOutputStream(new FileOutputStream(fileName))) {
            objectOut.writeObject(inConn);
            objectOut.writeObject(outConn);
        } catch (Exception e) {
            System.out.println("Raised exception while writing connections on file: " + e.toString());
            e.printStackTrace();
        }
    }

    public void readConnections(String fileName) {
        try (ObjectInputStream objectOut = new ObjectInputStream(new FileInputStream(fileName))) {
            inConn = (Set<Connection>) objectOut.readObject();
            outConn = (Set<Connection>) objectOut.readObject();
        } catch (Exception e) {
            System.out.println("Raised exception while reading connections from file: " + e.toString());
            e.printStackTrace();
        }
    }
     */