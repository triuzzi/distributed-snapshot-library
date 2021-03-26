package it.polimi.ds.ricciosorrentinotriuzzi.snaptest;


import it.polimi.ds.ricciosorrentinotriuzzi.snaplib.ConnInt;
import it.polimi.ds.ricciosorrentinotriuzzi.snaplib.Snapshot;
import it.polimi.ds.ricciosorrentinotriuzzi.snaplib.Snapshottable;
import org.apache.commons.configuration.ConfigurationException;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NodeImpl extends Snapshottable<State, Message> implements PublicInt, Serializable {
    private State state;
    private Set<ConnInt> incomingConnections;
    private Set<ConnInt> outgoingConnections;
    private String host;
    private String name;
    private Integer port;
    private XMLConfiguration config;

    public NodeImpl(XMLConfiguration config) throws AlreadyBoundException, RemoteException {
        super(config.getInt("myself.port"));
        this.config = config;

        host = config.getString("myself.host");
        name = config.getString("myself.name");
        port = config.getInt("myself.port");
        incomingConnections = new HashSet<>();
        outgoingConnections = new HashSet<>();

        List<HierarchicalConfiguration> incomingConn =  config.configurationsAt("incoming.conn");
        for (HierarchicalConfiguration hc : incomingConn) {
            incomingConnections.add(new Connection(hc.getString("host"),hc.getInt("port"),hc.getString("name")));
        }
        List<HierarchicalConfiguration> outgoingConn =  config.configurationsAt("outgoing.conn");
        for (HierarchicalConfiguration hc : outgoingConn) {
            outgoingConnections.add(new Connection(hc.getString("host"), hc.getInt("port"), hc.getString("name")));
        }
        state = new State();
        //PublicInt n = (PublicInt) UnicastRemoteObject.exportObject(this, port);
        LocateRegistry.getRegistry(port).bind("PublicInt", this);
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public String getHost() {
        return host;
    }

    public String getName() {
        return name;
    }

    public Integer getPort() {
        return port;
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
    public Set<ConnInt> getInConn() {
        return incomingConnections;
    }

    @Override
    public Set<ConnInt> getOutConn() {
        return outgoingConnections;
    }


    @Override
    public void increase(Integer diff) {
        String sender = null;
        try {
            sender = RemoteServer.getClientHost();
        } catch (ServerNotActiveException e) {
        }
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
        } catch (ServerNotActiveException e) {
        }

        if (shouldDiscard(sender)) { System.out.println("Decrease di "+diff+" scartata"); return; }
        addMessage(sender, new Message("decrease", new Class<?>[]{Integer.class}, new Integer[]{diff}));
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

    @Override
    public boolean connect(boolean isOutgoing, String host, int port, String name) throws RemoteException {
        Connection toAdd = new Connection(host, port, name);
        if(isOutgoing)
            outgoingConnections.add(toAdd);
         else
            incomingConnections.add(toAdd);

        XMLConfiguration configuration = new XMLConfiguration();
        XMLConfiguration out = new XMLConfiguration();
        out.addProperty("conn", "");
        configuration.addProperty("host", host);
        configuration.addProperty("name", name);
        configuration.addProperty("port", port);
        out.configurationAt("conn").append(configuration);
        config.configurationAt((isOutgoing ? "outgoing":"incoming")).append(out);

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
        (isOutgoing ? outgoingConnections : incomingConnections).removeIf((o)-> o.getHost().equals(host) && o.getPort() == port && o.getName().equals(name));
        //per ora facciamo così, po s ver
        config.clearTree((isOutgoing ? "outgoing":"incoming"));

        for(ConnInt conn : isOutgoing ? outgoingConnections : incomingConnections){
            config.addProperty((isOutgoing ? "outgoing":"incoming") + ".conn", "'&lt;'host'&rt;'" + conn.getHost() + "</host> \n <port>" + conn.getPort() + "</port> \n <name>" + conn.getName() + "</name>");
        }
        try {
            config.save();
            return true;
        } catch (ConfigurationException e) {
            e.printStackTrace();
            return false;
        }
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