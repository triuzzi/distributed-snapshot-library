package it.polimi.ds.ricciosorrentinotriuzzi.snaplib;

import java.io.*;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.stream.Collectors;

public abstract class Node<S extends Serializable, M extends Serializable> extends UnicastRemoteObject implements SnapInt<S,M> {
    private String host;
    private int port;
    private String name;
    private Set<ConnInt> incomingConns;
    private Set<ConnInt> outgoingConns;
    private Map<String, Set<String>> incomingStatus; //map(idSnap, set(inHost)) se è presente, vuol dire che non ho ancora ricevuto il marker)
    private Map<String, Snapshot<S, M>> snaps; //map(idSnap, Snap)
    private boolean restoring;
    private long clock;
    private Set<String> pendingRestores;

    public abstract S getState();
    public abstract void restoreSnapshot(Snapshot<S,M> snapshot);

    public Node(String host, int port, String name, Registry r) throws RemoteException, AlreadyBoundException {
        super(1099);
        this.host = host;
        this.port = port;
        this.name = name;
        incomingConns = new HashSet<>();
        outgoingConns = new HashSet<>();
        r.bind("SnapInt", this);
        incomingStatus = new HashMap<>();
        snaps = new HashMap<>();
        clock = 0L;
        restoring = false;
        this.pendingRestores = incomingInit();
        System.out.println("SnapLib configured");
    }

    public int getPort() {return port;}
    public String getHost() {return host;}
    public String getName() {return name;}

    public Set<ConnInt> getInConn() {
        return incomingConns;
    }

    public Set<ConnInt> getOutConn() {
        return outgoingConns;
    }

    public boolean addInConn(ConnInt incoming){
        return incomingConns.add(incoming);
    }

    public boolean addOutConn(ConnInt outgoing){
        return outgoingConns.add(outgoing);
    }

    /*public boolean saveMessage(){
        try {
            if(RemoteServer.getClientHost() != null)

            return true;
        } catch (ServerNotActiveException e) {
            e.printStackTrace();
        }
        return true;
    }

     */

    @Override
    public void startSnapshot(String id) {
        synchronized (this) {
            String tokenReceivedFrom = null;
            try {
                tokenReceivedFrom = RemoteServer.getClientHost();
            } catch (Exception e) {}
            //final String tokenReceivedFrom = TEMPtokenReceivedFrom;

            if (incomingStatus.containsKey(id)) { //c'è uno snap in corso e ricevo un token da un incoming
                if (tokenReceivedFrom != null) {
                    incomingStatus.get(id).remove(tokenReceivedFrom);
                    System.out.println("C'è uno snap in corso, ricevo un token da "+tokenReceivedFrom+" e l'ho rimosso dal set");
                }
                System.out.println("Devo aspettare il token ancora da:");
                for (String s: incomingStatus.get(id)){
                    System.out.println(s);
                }
                if (incomingStatus.get(id).isEmpty()) { //Se lo snapshot è finito
                    saveSnapshot(snaps.get(id));
                    incomingStatus.remove(id);
                    System.out.println("Lo snapshot creato da " + id + " è terminato");
                }
            } else { //è la prima volta che ricevo un token di startSnap (o sono io ad averlo fatto partire)
                System.out.println("Inizio il mio snapshot");
                clock = Math.max(Long.parseLong(id.split("\\.")[0]) + 1, clock);
                Snapshot<S, M> newSnapshot = new Snapshot<>(id, saveState(this.getState(), id));
                snaps.put(id, newSnapshot);
                //Inizializzo la mappa di connessioni in ingresso per questo snapshot
                Set<String> awaitingTokenFrom = new HashSet<>();
                for (ConnInt connInt : incomingConns) {
                    awaitingTokenFrom.add(connInt.getHost());
                }
                if (tokenReceivedFrom != null) {awaitingTokenFrom.remove(tokenReceivedFrom);}
                incomingStatus.put(id, awaitingTokenFrom);

                System.out.println("Avvio gli snap degli outgoing");
                try {
                    System.out.println("Outgoing nodes:");
                    for (ConnInt connInt : outgoingConns) {
                        System.out.println("Host: " + connInt.getHost() + " Port: " + connInt.getPort());
                        SnapInt<S, M> snapRemInt = ((SnapInt<S, M>) LocateRegistry
                                .getRegistry(connInt.getHost(), connInt.getPort())
                                .lookup("SnapInt"));
                        System.out.println("Remote interface di "+ connInt.getHost()+" trovata");
                        new Thread(() -> {
                            try {
                                snapRemInt.startSnapshot(id);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }).start();
                        System.out.println("Snapshot a "+ connInt.getHost()+" richiesto");
                    }
                    //Se ho ricevuto il token dall'unico canale in ingresso
                    System.out.println("set empty? "+incomingStatus.get(id).isEmpty());
                    if (incomingStatus.get(id).isEmpty()) {
                        saveSnapshot(snaps.get(id));
                        incomingStatus.remove(id);
                        System.out.println("Lo snapshot avviato da " + id + " è terminato xke ne avevo solo 1 in input");
                    }
                } catch (NotBoundException | RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //Per ogni snapshot attivo, se il nodo da cui riceviamo il messaggio è nello snap, salva il messaggio
    public synchronized void addMessage(M message) {
        try {
            String tokenReceivedFrom = RemoteServer.getClientHost();
            for (Snapshot<S, M> snapshot : snaps.values()){
                if (incomingStatus.get(snapshot.getId()).contains(tokenReceivedFrom))
                    snapshot.addMessage(message);
            }
            System.out.println("Message "+message+" received from "+tokenReceivedFrom+" added");
        } catch (Exception e) {}
    }

    private S saveState(S state, String snapshotID) {
        File f = new File("temp.state");
        S temp = null;
        try (ObjectOutputStream objectOut = new ObjectOutputStream(new FileOutputStream(f))) {
            objectOut.writeObject(state);
            //System.out.println("The state " + snapshotID + ".state" + " was successfully written to the file");
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        try (ObjectInputStream objectOut = new ObjectInputStream(new FileInputStream(f))) {
            temp = (S) objectOut.readObject();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        f.delete();
        System.out.println("The state for snapshot " + snapshotID + " was successfully saved");
        return temp;
    }

    public void initiateSnapshot() {
        String id = clock+"."+host/* + ip della macchina*/;
        startSnapshot(id);
    }

    @Override
    public void printMsg() throws ServerNotActiveException {
        System.out.println("printMsg invoked from "+RemoteServer.getClientHost());
    }


    public Snapshot<S, M> restoreLast() {
        File snapshots = new File(System.getProperty("user.dir"));
        List<String> snapnames = Arrays.stream(Objects.requireNonNull(snapshots.list())).filter(s -> s.matches("([^\\s]+(\\.(?i)(snap))$)")).collect(Collectors.toList());
        //snapnames.sort(String::compareTo);
        // System.out.println(snapnames.get(snapnames.size()-1));
        System.out.println("Lo snapshot selezionato per il restore è: "+snapnames.get(0).split("((\\.(?i)(snap))$)")[0]);
        return readSnapshot(snapnames.get(0).split("((\\.(?i)(snap))$)")[0]);
    }

    //TODO UPDATE CONNECTIONS (da chiamare quando il nodo cambia le sue connessioni)

    @Override
    public void restore() {
        synchronized (this) {
            try {
                String tokenReceivedFrom = RemoteServer.getClientHost();
                if (!pendingRestores.contains(tokenReceivedFrom) && restoring) {
                    restoring = false;
                    pendingRestores = incomingInit();
                }
                pendingRestores.remove(tokenReceivedFrom);
                if (!restoring) {
                    this.restoreSnapshot(restoreLast());
                    restoring = true;
                    //chiama il restore degli altri sulla current epoch
                    for (ConnInt connInt : outgoingConns) {
                        new Thread(() -> {
                            try {
                                ((SnapInt<S, M>) LocateRegistry
                                        .getRegistry(connInt.getHost(), connInt.getPort())
                                        .lookup("SnapInt")).restore();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();
                    }
                }
                if (pendingRestores.isEmpty()) {
                    restoring = false;
                    pendingRestores = incomingInit();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isRestoring(){return restoring;}

    public boolean discardMessage(String incomingConnection){
        return (isRestoring() && pendingRestores.contains(incomingConnection));
    }

    private Set<String> incomingInit(){
        Set<String> toRet = new HashSet<>();
        for (ConnInt c : incomingConns)
            toRet.add(c.getHost());
        return toRet;
    }

    private void saveSnapshot(Snapshot<S, M> snap) {
        try (ObjectOutputStream objectOut = new ObjectOutputStream(new FileOutputStream(snap.getId()+".snap"))) {
            objectOut.writeObject(snap);
            System.out.println("The snapshot " + snap.getId() + " was successfully written to the file");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private Snapshot<S, M> readSnapshot(String id) {
        Snapshot<S, M> snapshot;
        try (ObjectInputStream objectOut = new ObjectInputStream(new FileInputStream(id+".snap"))) {
            snapshot = (Snapshot<S, M>) objectOut.readObject();
            System.out.println("The snapshot " + snapshot.getId() + " was successfully read from the file");
            return snapshot;
        } catch (Exception ex) {
            return null;
        }
    }



}
