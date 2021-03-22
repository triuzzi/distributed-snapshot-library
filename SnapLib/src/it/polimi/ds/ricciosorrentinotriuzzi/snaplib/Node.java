package it.polimi.ds.ricciosorrentinotriuzzi.snaplib;

import java.io.*;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.stream.Collectors;

public abstract class Node<S extends Serializable, M extends Serializable> extends UnicastRemoteObject implements SnapInt {
    private final String host;
    private final int port;
    private final String name;
    private final Set<ConnInt> incomingConns;
    private final Set<ConnInt> outgoingConns;
    //map(idSnap, set(inHost)) se inHost è presente, vuol dire che non ho ancora ricevuto il marker da lui)
    private final Map<String, Set<String>> incomingStatus;
    private final Map<String, Snapshot<S, M>> snaps; //map(idSnap, Snap)
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


    @Override
    public void startSnapshot(String id) {
        synchronized (this) {
            String markerReceivedFrom = null;
            try {
                markerReceivedFrom = RemoteServer.getClientHost();
            } catch (Exception e) {}

            if (incomingStatus.containsKey(id)) { //lo snap identificato da id è in corso
                System.out.println("Lo snap "+id+" era già in corso");
                if (markerReceivedFrom != null) { //se non sono io ad aver richiesto lo snap
                    incomingStatus.get(id).remove(markerReceivedFrom);
                    System.out.println("Ho ricevuto il marker da "+markerReceivedFrom+" e l'ho rimosso dal set");
                }
                System.out.println("Devo aspettare il marker ancora da:");
                for (String s: incomingStatus.get(id)){
                    System.out.println(s);
                }
                if (incomingStatus.get(id).isEmpty()) { //Se lo snapshot è finito
                    saveSnapshot(snaps.get(id));
                    incomingStatus.remove(id);
                    System.out.println("Lo snapshot " + id + " è terminato");
                }
            } else { //è la prima volta che ricevo il marker id di startSnap (o sono io ad averlo fatto partire)
                System.out.println("Inizio per la prima volta lo snapshot con id="+id);
                clock = Math.max(Long.parseLong(id.split("\\.")[0]) + 1, clock);
                Snapshot<S, M> newSnapshot = new Snapshot<>(id, saveState(id));
                snaps.put(id, newSnapshot);

                //Inizializzo la mappa di connessioni in ingresso per questo snapshot
                Set<String> awaitingTokenFrom = new HashSet<>();
                for (ConnInt connInt : incomingConns) {
                    awaitingTokenFrom.add(connInt.getHost());
                }
                //Rimuovo chi mi ha mandato il token dal set di host da cui aspettare il marker
                if (markerReceivedFrom != null) {awaitingTokenFrom.remove(markerReceivedFrom);}
                incomingStatus.put(id, awaitingTokenFrom);

                System.out.println("Avvio gli snap degli outgoing");
                try {
                    System.out.println("Outgoing nodes:");
                    for (ConnInt connInt : outgoingConns) {
                        System.out.println(connInt.getName() + " at " + connInt.getHost() + ":" + connInt.getPort());
                        SnapInt snapRemInt = ((SnapInt) LocateRegistry
                                .getRegistry(connInt.getHost(), connInt.getPort())
                                .lookup("SnapInt"));
                        System.out.println("Connessione a "+ connInt.getHost()+" riuscita. Richiedo lo snap");
                        new Thread(() -> {
                            try {
                                snapRemInt.startSnapshot(id);
                            } catch (RemoteException e) { e.printStackTrace(); }
                        }).start();
                    }
                    //Se ho ricevuto il token dall'unico canale in ingresso
                    //System.out.println("set empty? "+incomingStatus.get(id).isEmpty());
                    if (incomingStatus.get(id).isEmpty()) {
                        saveSnapshot(snaps.get(id));
                        incomingStatus.remove(id);
                        System.out.println("Il mio snapshot locale " + id + " è già terminato (marker dall'unico ingresso)");
                    }
                } catch (NotBoundException | RemoteException e) { e.printStackTrace(); }
            }
        }
    }

    //Per ogni snapshot attivo, se dal nodo da cui riceviamo il messaggio non ho ancora ricevuto il marker, salva il messaggio
    public synchronized void addMessage(M message) {
        try {
            String tokenReceivedFrom = RemoteServer.getClientHost();
            for (Snapshot<S, M> snapshot : snaps.values()){
                System.out.println("Entro nel foreach");
                System.out.println(snapshot.getId());
                if (incomingStatus.get(snapshot.getId()).contains(tokenReceivedFrom))
                    snapshot.addMessage(message);
            }
            System.out.println("Message "+message+" received from "+tokenReceivedFrom+" added");
        } catch (Exception e) { e.printStackTrace(); }
    }

    //Salva lo stato per lo snapshot identificato da snapshotID
    private S saveState(String snapshotID) {
        File f = new File("temp.state");
        S temp;
        try (ObjectOutputStream objectOut = new ObjectOutputStream(new FileOutputStream(f))) {
            objectOut.writeObject(getState());
            //System.out.println("The state " + snapshotID + ".state" + " was successfully written to the file");
        } catch (Exception ex) { ex.printStackTrace(); return null; }
        try (ObjectInputStream objectOut = new ObjectInputStream(new FileInputStream(f))) {
            temp = (S) objectOut.readObject();
        } catch (Exception ex) { ex.printStackTrace(); return null; }
        f.delete();
        System.out.println("The state for snapshot " + snapshotID + " was successfully saved");
        return temp;
    }

    public void initiateSnapshot() {
        String id = clock+"."+host;
        startSnapshot(id);
    }




    public Snapshot<S, M> restoreLast() {
        File snapshots = new File(System.getProperty("user.dir"));
        System.out.println("Snapshots list: " + snapshots.list());
        List<String> snapnames = Arrays.stream(Objects.requireNonNull(snapshots.list())).filter(s -> s.matches("([^\\s]+(\\.(?i)(snap))$)")).collect(Collectors.toList());
        if(!snapnames.isEmpty()) {
            System.out.println("Lo snapshot selezionato per il restore è: " + snapnames.get(0).split("((\\.(?i)(snap))$)")[0]);
            return readSnapshot(snapnames.get(0).split("((\\.(?i)(snap))$)")[0]);
        }
        else {
            System.out.println("Snapnames è vuota!");
            return null;
        }
    }

    //TODO UPDATE CONNECTIONS (da chiamare quando il nodo cambia le sue connessioni)

    @Override
    public void restore() {
        synchronized (this) {
            String tokenReceivedFrom = null;
            try {
                tokenReceivedFrom = RemoteServer.getClientHost();
            } catch (Exception e) {}
            try {
                if (restoring && !pendingRestores.contains(tokenReceivedFrom)) {
                    restoring = false;
                    pendingRestores = incomingInit();
                }
                if (tokenReceivedFrom != null) pendingRestores.remove(tokenReceivedFrom);
                if (!restoring) {
                    this.restoreSnapshot(restoreLast());
                    restoring = true;
                    //chiama il restore degli altri sulla current epoch
                    for (ConnInt connInt : outgoingConns) {
                        new Thread(() -> {
                            try {
                                ((SnapInt) LocateRegistry
                                        .getRegistry(connInt.getHost(), connInt.getPort())
                                        .lookup("SnapInt")).restore();
                            } catch (Exception e) { e.printStackTrace(); }
                        }).start();
                    }
                }
                if (pendingRestores.isEmpty()) {
                    restoring = false;
                    pendingRestores = incomingInit();
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    public boolean isRestoring() { return restoring; }

    public boolean discardMessage(String incomingConnection) {
        return (isRestoring() && pendingRestores.contains(incomingConnection));
    }

    private Set<String> incomingInit() {
        Set<String> toRet = new HashSet<>();
        for (ConnInt c : incomingConns)
            toRet.add(c.getHost());
        return toRet;
    }

    private void saveSnapshot(Snapshot<S, M> snap) {
        try (ObjectOutputStream objectOut = new ObjectOutputStream(new FileOutputStream(snap.getId()+".snap"))) {
            objectOut.writeObject(snap);
            System.out.println("The snapshot " + snap.getId() + " was successfully written to the file");
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private Snapshot<S, M> readSnapshot(String id) {
        try (ObjectInputStream objectOut = new ObjectInputStream(new FileInputStream(id+".snap"))) {
            Snapshot<S, M> snapshot = (Snapshot<S, M>) objectOut.readObject();
            System.out.println("The snapshot " + snapshot.getId() + " was successfully read from the file");
            return snapshot;
        } catch (Exception ex) { ex.printStackTrace(); return null; }
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
}
