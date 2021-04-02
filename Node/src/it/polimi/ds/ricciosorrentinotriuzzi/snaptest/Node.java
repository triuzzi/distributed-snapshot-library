package it.polimi.ds.ricciosorrentinotriuzzi.snaptest;

import it.polimi.ds.ricciosorrentinotriuzzi.snaplib.*;
import org.apache.commons.configuration.*;
import java.io.*;
import java.lang.reflect.Method;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.util.*;

public class Node extends Snapshottable<State, Message> implements PublicInt, Serializable {
    private State state;
    final private String host;
    final private String name;
    final private String defaultCustomer;
    final private Integer port;
    final private XMLConfiguration config;

    public Node(XMLConfiguration config) throws Exception {
        super(config.getInt("port"));
        this.config = config;
        host = config.getString("host");
        name = config.getString("name");
        port = config.getInt("port");
        defaultCustomer = config.getString("defaultCustomer");
        state = new State();
        LocateRegistry.getRegistry(port).bind("PublicInt", this);
        if (!hasCrashed()) {
            if (!config.getBoolean("newNetwork", false)) {
                joinNetwork();
            }
            XMLConfiguration ledgers = new XMLConfiguration("ledgers.xml");
            List<HierarchicalConfiguration> users =  ledgers.configurationsAt("user");
            for (HierarchicalConfiguration hc : users) {
                getState().newCustomer(hc.getString("name"),hc.getInt("balance"));
            }
        }
        resetCrashDetector();
    }

    private void joinNetwork() throws Exception {
        System.out.println("Joining the network...");
        //aggiungi nodo in ingresso
        SubnodeConfiguration conf = config.configurationAt("incoming");
        Connection nodeConn = new Connection(conf.getString("host"),conf.getInt("port"),conf.getString("name"));
        getInConn().add(nodeConn);
        PublicInt node = (PublicInt) LocateRegistry.getRegistry(nodeConn.getHost(), nodeConn.getPort()).lookup("PublicInt");
        node.addConn(true, getHost(), getPort(), getName());
        //aggiungi nodo in uscita
        conf = config.configurationAt("outgoing");
        nodeConn = new Connection(conf.getString("host"),conf.getInt("port"),conf.getString("name"));
        getOutConn().add(nodeConn);
        node = (PublicInt) LocateRegistry.getRegistry(nodeConn.getHost(), nodeConn.getPort()).lookup("PublicInt");
        node.addConn(false, getHost(), getPort(), getName());
        //sincronizza clock col nodo in uscita
        super.joinNetwork(nodeConn.getHost(), nodeConn.getPort());
        //applyNetworkChange(); //avvia snap viene fatto dalla super.join ora
        System.out.println("Ora sono parte della rete!");
    }

    //assumiamo che non ci siano snapshot in corso quando viene chiamata né durante la sua esecuzione. Idem per le restore
    //synchronized per non fare operazioni durante la disconnessione
    public synchronized void leaveNetwork() throws Exception {
        config.reload();
        if (config.getBoolean("canLeaveNetworkSafely")) {
            System.out.println("Mi disconnetto dalla rete...");
            for (ConnInt c: getInConn()) {
                removeConn(false, c.getHost(), false);
                PublicInt node = (PublicInt) LocateRegistry.getRegistry(c.getHost(), c.getPort()).lookup("PublicInt");
                node.removeConn(true, getHost(), false);
            }
            for (ConnInt c: getOutConn()) {
                removeConn(true, c.getHost(), false);
                PublicInt node = (PublicInt) LocateRegistry.getRegistry(c.getHost(), c.getPort()).lookup("PublicInt");
                //lo snapshot lo avvia solo l'ultimo nodo dal quale mi disconnetto
                node.removeConn(false, this.host, !(getOutConn().size() > 1));
            }
            System.out.println("Disconnesso dalla rete");
        }
    }


    //PublicInt Implementation
    @Override
    public synchronized void transfer(String to, Integer amount) throws RemoteException {
        String sender;
        try {
            sender = RemoteServer.getClientHost();
            if (shouldDiscard(sender)) { System.out.println("Trasferimento di "+amount+" a "+to+" scartato"); return; }
            addMessage(sender, new Message("transfer", new Class<?>[]{String.class, Integer.class}, new Object[]{to, amount}));
        } catch (ServerNotActiveException e) { }
        if (getState().getCBalance(to) != null) { //problema: se non c'è, abbiamo perso dei soldi
            getState().sumBalance(to, amount);
            System.out.println("Trasferimento di "+amount+" a "+to+" effettuato. Nuovo saldo: "+getState().getCBalance(to));
        } else {
            System.out.println("Destinatario sconosciuto");
        }
    }

    @Override
    public void transferLedger(Map<String, Integer> ledgerToImport) throws RemoteException {
        Map<String, Integer> currentLedger = this.getState().getLedger();
        for (String customer : ledgerToImport.keySet())
            currentLedger.put(customer, ledgerToImport.get(customer) + (currentLedger.getOrDefault(customer, 0)));
    }

    @Override
    public void addConn(boolean toOutgoing, String host, int port, String name) throws RemoteException {
        (toOutgoing ? getOutConn() : getInConn()).add(new Connection(host, port, name));
        System.out.println("Aggiungo "+host+" negli "+(toOutgoing ? "outgoing" : "incoming"));
    }

    @Override
    public void removeConn(boolean fromOutgoing, String host, boolean shouldStartSnapshot) throws RemoteException {
        (fromOutgoing ? getOutConn() : getInConn()).removeIf(o -> o.getHost().equals(host));
        System.out.println("Rimuovo "+host+" dagli "+(fromOutgoing ? "outgoing" : "incoming"));
        if (shouldStartSnapshot) {
            try {
                RemoteServer.getClientHost();
                applyNetworkChange(); //se è una chiamata remota avvia lo snapshot
            } catch (ServerNotActiveException e) { }
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



    //Snapshottable Implementation
    @Override
    public State getState() { return state; }

    @Override
    public String getHost() { return host; }

    @Override
    public void restoreSnapshot(Snapshot<State,Message> snapshot) {
        synchronized (this) {
            this.state = snapshot.getState();
        }
        for (Message message : snapshot.getMessages()) {
            try {
                Method method = Node.class.getMethod(message.getMethodName(), message.getParameterTypes());
                method.invoke(this, message.getParameters());
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    @Override
    public Set<ConnInt> getInConn() { return state.getIncomingConnections(); }

    @Override
    public Set<ConnInt> getOutConn() { return state.getOutgoingConnections(); }



    //Funzioni interne di Node

    //TODO assumiamo che vada sempre tutto bene (altrimenti il destinatario potrebbe avermi aggiunto e io fallisco ad aggiungere lui)
    public boolean connectTo(String host, Integer port, String name, boolean isOutgoingFromMe) {
        try {
            System.out.println("Connecting to "+host);
            addConn(isOutgoingFromMe, host, port, name);
            PublicInt node = (PublicInt) LocateRegistry.getRegistry(host, port).lookup("PublicInt");
            node.addConn(!isOutgoingFromMe, this.getHost(), this.getPort(), this.getName());
            applyNetworkChange(); //avvio snapshot
            System.out.println("Connected!");
        } catch (Exception e) {e.printStackTrace(); return false;}
        return true;
    }

    //assumiamo che viene chiamata solo quando la disconnessione non altera la proprietà di strongly connected graph
    //TODO assumiamo che vada sempre tutto bene (altrimenti il destinatario potrebbe avermi aggiunto e io fallisco ad aggiungere lui)
    public boolean disconnectFrom(String host, Integer port, boolean isOutgoingFromMe) {
        try {
            System.out.println("Disconnecting from "+host);
            removeConn(isOutgoingFromMe, host, false);
            PublicInt node = (PublicInt) LocateRegistry.getRegistry(host, port).lookup("PublicInt");
            node.removeConn(!isOutgoingFromMe, getHost(), true);
            System.out.println("Disconnected");
        } catch (Exception e) { e.printStackTrace(); return false; }
        return true;
    }

    public Integer getCBalance(String customer) {
        return state.getCBalance(customer);
    }

    public void transferMoney(String customer, String receiverBankID, String receiver, Integer amount) {
        if (getCBalance(customer) == null) {
            System.out.println("Non è stato trovato il cliente " + customer);
            return;
        }
        if (getCBalance(customer) >= amount) {
            for (ConnInt connection : getOutConn())
                if (connection.getName().equalsIgnoreCase(receiverBankID)) {
                    try {
                        PublicInt receiverBank = (PublicInt) LocateRegistry
                                .getRegistry(connection.getHost(), connection.getPort())
                                .lookup("PublicInt");
                        System.out.println("Invio il trasferimento");
                        receiverBank.transfer(receiver, amount);
                        state.sumBalance(customer, -amount);
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("La banca " + receiverBankID + " non è disponibile");
                    }
                }
        } else {
            System.out.println("Non hai abbastanza fondi per trasferire l'importo selezionato\nIl tuo saldo è " + getCBalance(customer));
        }
    }

    public String getName() { return name; }
    public Integer getPort() { return port; }
    public String getDefaultCustomer() { return defaultCustomer; }

}


/*

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

    List<HierarchicalConfiguration> incomingConn =  config.configurationsAt("incoming/conn");
    for (HierarchicalConfiguration hc : incomingConn) {
        state.getIncomingConnections().add(new Connection(hc.getString("host"),hc.getInt("port"),hc.getString("name")));
    }
    List<HierarchicalConfiguration> outgoingConn =  config.configurationsAt("outgoing/conn");
    for (HierarchicalConfiguration hc : outgoingConn) {
        state.getOutgoingConnections().add(new Connection(hc.getString("host"), hc.getInt("port"), hc.getString("name")));
    }
    //PublicInt n = (PublicInt) UnicastRemoteObject.exportObject(this, port);

    if (!config.containsKey(confSet)) {config.addProperty(confSet,"");}
    SubnodeConfiguration subset = config.configurationAt(confSet);
    if (!subset.containsKey("conn[@host='"+host+"']")) {
        subset.addProperty("conn","");
        subset.addProperty("conn[last()] @host", host);
        subset.addProperty("conn[last()] port", port);
        subset.addProperty("conn[last()] name", name);
        config.save();
    }

    config.clearTree(confSet+"/conn[@host='"+host+"']");
    config.save();




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
    /*
    @Override
    public synchronized void withdraw(String from, Integer amount) throws RemoteException {
        String sender = null;
        try {
            sender = RemoteServer.getClientHost();
        } catch (ServerNotActiveException e) { }
        if (shouldDiscard(sender)) { System.out.println("Prelievo di "+amount+" da "+from+" scartato"); return; }
        addMessage(sender, new Message("withdraw", new Class<?>[]{String.class, Integer.class}, new Object[]{from, amount}));
        Integer balance = getState().getCBalance(from);
        if (balance != null && balance >= amount) {
            getState().sumBalance(from, -amount);
            System.out.println("Prelievo di "+amount+" da "+from+" effettuato. Nuovo saldo: "+getState().getCBalance(from));
        } else {
            System.out.println("Destinatario sconosciuto");
        }
    }

    @Override
    public synchronized void register(String customer) throws RemoteException {
        getState().newCustomer(customer);
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