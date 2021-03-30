package it.polimi.ds.ricciosorrentinotriuzzi.snaptest;

import it.polimi.ds.ricciosorrentinotriuzzi.snaplib.*;
import org.apache.commons.configuration.*;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.util.*;

public class NodeImpl extends Snapshottable<State, Message> implements PublicInt, Serializable {
    private State state;
    final private Set<ConnInt> incomingConnections;
    final private Set<ConnInt> outgoingConnections;
    final private String host;
    final private String name;
    final private Integer port;
    final private XMLConfiguration config;

    public NodeImpl(XMLConfiguration config) throws AlreadyBoundException, RemoteException {
        super(config.getInt("port"));
        this.config = config;
        host = config.getString("host");
        name = config.getString("name");
        port = config.getInt("port");
        state = new State();
        incomingConnections = new HashSet<>();
        outgoingConnections = new HashSet<>();

        List<HierarchicalConfiguration> incomingConn =  config.configurationsAt("incoming/conn");
        for (HierarchicalConfiguration hc : incomingConn) {
            incomingConnections.add(new Connection(hc.getString("host"),hc.getInt("port"),hc.getString("name")));
        }
        List<HierarchicalConfiguration> outgoingConn =  config.configurationsAt("outgoing/conn");
        for (HierarchicalConfiguration hc : outgoingConn) {
            outgoingConnections.add(new Connection(hc.getString("host"), hc.getInt("port"), hc.getString("name")));
        }
        //PublicInt n = (PublicInt) UnicastRemoteObject.exportObject(this, port);
        LocateRegistry.getRegistry(port).bind("PublicInt", this);
    }

    @Override
    public State getState() { return state; }

    @Override
    public String getHost() { return host; }

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
        String sender = null;
        try {
            sender = RemoteServer.getClientHost();
        } catch (ServerNotActiveException e) { }
        if (shouldDiscard(sender)) { System.out.println("Increase "+diff+" scartata"); return; }
        addMessage(sender, new Message("increase", new Class<?>[]{Integer.class}, new Integer[]{diff}));
        getState().increase(diff);
        System.out.println("Increase di "+diff);
        System.out.println("Balance: "+getState().getBalance());
    }

    @Override
    public void decrease(Integer diff) {
        String sender = null;
        try {
            sender = RemoteServer.getClientHost();
        } catch (ServerNotActiveException e) { }
        if (shouldDiscard(sender)) { System.out.println("Decrease di "+diff+" scartata"); return; }
        addMessage(sender, new Message("decrease", new Class<?>[]{Integer.class}, new Integer[]{diff}));
        getState().decrease(diff);
        System.out.println("Decrease di "+diff);
        System.out.println("Balance: "+getState().getBalance());
    }

    public boolean initConnection(Set<Connection> incomingsToConnect, (Set<Connection> outgoingsToConnect){
        boolean repeat = true;
        int sleepTime = 100;
        int sleepMax = 5_000;
        while (!(incomingsToConnect.isEmpty() && outgoingsToConnect.isEmpty()){
            try {
                for (Connection c: incomingsToConnect) {
                    if(connectTo(c.getHost(), c.getPort(), c.getName(), false)){
                        incomingConnections.add(c);
                        incomingsToConnect.remove(c);
                    }
                }
                for (Connection c: outgoingsToConnect) {
                    if(connectTo(c.getHost(), c.getPort(), c.getName(), true)){
                        outgoingConnections.add(c);
                        outgoingsToConnect.remove(c);
                    }
                }
                Thread.sleep(sleepTime);
                sleepTime = Math.min(sleepTime*2, sleepMax);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //for each outgoing e incoming, provo a connettemrici.

            //prendo tutti quelli dall'init. Prendo gli host dal config e provo
            //a contattarli. se non ci riesco ricomincio con soli quelli che mi rimangono. Non è facile.

            //altra soluzione. Il grafo viene costruito a step: ci sarà il primo nodo solo, il secondo si connette a 1 in maniera bidir.,
            //dal terzo in poi ci si può connettere a cazzo
        }


        return true;
    }

    //TODO assumiamo che vada sempre tutto bene (altrimenti il destinatario potrebbe avermi aggiunto e io fallisco ad aggiungere lui)
    public boolean connectTo(String host, Integer port, String name, boolean isOutgoingFromMe) {
        try {
            System.out.println("Connecting to "+host);
            addConn(isOutgoingFromMe, host, port, name);
            PublicInt node = (PublicInt) LocateRegistry.getRegistry(host, port).lookup("PublicInt");
            node.addConn(!isOutgoingFromMe, this.getHost(), this.getPort(), this.getName());
            System.out.println("Connected!");
        } catch (Exception e) {e.printStackTrace(); return false;}
        return true;
    }

    //TODO assumiamo che vada sempre tutto bene (altrimenti il destinatario potrebbe avermi aggiunto e io fallisco ad aggiungere lui)
    public boolean disconnectFrom(String host, Integer port, boolean isOutgoingFromMe) {
        try {
            System.out.println("Disconnecting from "+host);
            removeConn(isOutgoingFromMe, host);
            PublicInt node = (PublicInt) LocateRegistry.getRegistry(host, port).lookup("PublicInt");
            node.removeConn(!isOutgoingFromMe, getHost());
            System.out.println("Disconnected");
        } catch (Exception e) {e.printStackTrace(); return false;}
        return true;
    }

    @Override
    public void addConn(boolean toOutgoing, String host, int port, String name) throws RemoteException, ConfigurationException {
        String confSet = toOutgoing ? "outgoing" : "incoming";
        (toOutgoing ? outgoingConnections : incomingConnections).add(new Connection(host, port, name));
        if (!config.containsKey(confSet)) {config.addProperty(confSet,"");}
        SubnodeConfiguration subset = config.configurationAt(confSet);
        if (!subset.containsKey("conn[@host='"+host+"']")) { //TODO TEST
            subset.addProperty("conn","");
            subset.addProperty("conn[last()] @host", host);
            subset.addProperty("conn[last()] port", port);
            subset.addProperty("conn[last()] name", name);
            config.save();
        }
        if (!host.equals(getHost())) { applyNetworkChange(); } //se è una chiamata remota avvia lo snapshot
    }

    @Override
    public void removeConn(boolean fromOutgoing, String host) throws RemoteException, ConfigurationException {
        String confSet = fromOutgoing ? "outgoing" : "incoming";
        (fromOutgoing ? outgoingConnections : incomingConnections).removeIf( o -> o.getHost().equals(host));
        config.clearTree(confSet+"/conn[@host='"+host+"']");
        config.save();
        if (!host.equals(getHost())) { applyNetworkChange(); } //se è una chiamata remota avvia lo snapshot
    }

    @Override
    public Set<ConnInt> getInConn() { return incomingConnections; }

    @Override
    public Set<ConnInt> getOutConn() { return outgoingConnections; }

    public String getName() { return name; }
    public Integer getPort() { return port; }

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
@Override
    public boolean connect(boolean isOutgoing, String host, int port, String name) throws RemoteException {
        Connection toAdd = new Connection(host, port, name);
        if (isOutgoing)
            outgoingConnections.add(toAdd);
        else
            incomingConnections.add(toAdd);

        XMLConfiguration c = new XMLConfiguration();
        c.addProperty("conn", "");
        SubnodeConfiguration conn = c.configurationAt("conn");
        conn.addProperty("host", host);
        conn.addProperty("name", name);
        conn.addProperty("port", port);
        config.configurationAt(isOutgoing ? "outgoing" : "incoming").append(c);

        try {
            config.save();
            return true;
        } catch (ConfigurationException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean disconnect(boolean isOutgoing, String host, int port, String name) throws RemoteException {
        (isOutgoing ? outgoingConnections : incomingConnections).removeIf(o -> o.getHost().equals(host));
        config.clearTree(isOutgoing ? "outgoing"+"/conn[@host=\""+host+"\"]" : "incoming"+"/conn[@host=\""+host+"\"]");
        try {
            config.save();
            return true;
        } catch (ConfigurationException e) {
            e.printStackTrace();
            return false;
        }
    }
 */











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
     */