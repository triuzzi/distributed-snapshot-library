package it.polimi.ds.ricciosorrentinotriuzzi.snaplib;

import java.io.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.*;
import java.util.*;
import java.util.stream.Collectors;


public class SnapLib <S extends Serializable, M extends Serializable> implements SnapInt<S, M> {
    private Set<Node> incomingConnections;
    private Set<Node> outgoingConnections;
    private Map<String, Set<String>> incomingStatus; //map(idSnap, set(inHost)) se è presente, vuol dire che non ho ancora ricevuto il marker)
    private Map<String, Snapshot<S, M>> snaps; //map(idSnap, Snap)
    private boolean restoring;
    private final Node<S,M> node;
    private long clock;
    private Set<String> pendingRestores;

    public SnapLib(Registry r, Set<Node> incomingConnections, Set<Node> outgoingConnections, Node<S,M> node) throws Exception {
        r.bind("SnapInt", UnicastRemoteObject.exportObject(this, 1099));
        incomingStatus = new HashMap<>();
        snaps = new HashMap<>();
        clock = 0L;
        restoring = false;
        this.incomingConnections = incomingConnections;
        this.outgoingConnections = outgoingConnections;
        this.pendingRestores = incomingInit();
        this.node = node;
        System.out.println("SnapLib configured");
    }

    public void saveSnapshot(Snapshot<S, M> snap) throws SnapEx {
        try (ObjectOutputStream objectOut = new ObjectOutputStream(new FileOutputStream(snap.getId()+".snap"))) {
            objectOut.writeObject(snap);
            System.out.println("The snapshot " + snap.getId() + " was successfully written to the file");
        } catch (Exception ex) {
            throw new SnapEx();
        }
    }

    public Snapshot<S, M> readSnapshot(String id) throws SnapEx {
        Snapshot<S, M> snapshot;
        try (ObjectInputStream objectOut = new ObjectInputStream(new FileInputStream(id+".snap"))) {
            snapshot = (Snapshot<S, M>) objectOut.readObject();
            System.out.println("The snapshot " + snapshot.getId() + " was successfully read from the file");
            return snapshot;
        } catch (Exception ex) {
            throw new SnapEx();
        }
    }

    @Override
    public void startSnapshot(String id) throws SnapEx {
        String tokenReceivedFrom = null;
        try {
            tokenReceivedFrom = RemoteServer.getClientHost();
        } catch (Exception e) {}
       // synchronized (node) {
            if (incomingStatus.containsKey(id)) { //c'è uno snap in corso e ricevo un token da un incoming
                System.out.println("c'è uno snap in corso e ricevo un token da un incoming");
                System.out.println("Devo aspettare il token da:");
                for (String s: incomingStatus.get(id)){
                    System.out.println(s);
                }
                if (tokenReceivedFrom != null) {
                    incomingStatus.get(id).remove(tokenReceivedFrom);
                    System.out.println("Sono stato chiamato da "+tokenReceivedFrom+" e l'ho rimosso dal set");
                }
                System.out.println("Devo aspettare il token ancora da:");
                for (String s: incomingStatus.get(id)){
                    System.out.println(s);
                }
                //Se lo snapshot è finito
                if (incomingStatus.get(id).isEmpty()) {
                    saveSnapshot(snaps.get(id));
                    incomingStatus.remove(id);
                    System.out.println("Lo snapshot creato da " + id + " è terminato");
                }
            } else { //è la prima volta che ricevo un token di startSnap
                System.out.println("Inizio il mio snapshot");
                clock = Math.max(Long.parseLong(id.split("\\.")[0]) + 1, clock);
                Snapshot<S, M> newSnapshot = new Snapshot<>(id, saveState(node.getState(), id));
                //System.out.println("CI ARRIVO");
                snaps.put(id, newSnapshot);
                //Inizializzo la mappa di connessioni in ingresso per questo snapshot
                Set<String> incoming = new HashSet<>();
                for (Node connection : incomingConnections) {
                    incoming.add(connection.getHost());
                }
                if (tokenReceivedFrom != null) {incoming.remove(tokenReceivedFrom);}
                incomingStatus.put(id, incoming);

                System.out.println("Avvia gli snap degli outgoing");
                try {
                    System.out.println("Outgoing nodes:");
                    for (Node connection : outgoingConnections) {
                        System.out.println("Host: " + connection.getHost() + " Port: " + connection.getPort());
                        SnapInt<S, M> snapRemInt = ((SnapInt<S, M>) LocateRegistry
                                .getRegistry(connection.getHost(), connection.getPort())
                                .lookup("SnapInt"));
                        System.out.println("Remote interface di "+connection.getHost()+" trovata");
                        snapRemInt.startSnapshot(id);
                        System.out.println("Snapshot a "+connection.getHost()+" richiesto");
                    }
                    System.out.println("set empty? "+incomingStatus.get(id).isEmpty());
                    if (incomingStatus.get(id).isEmpty()) {
                        saveSnapshot(snaps.get(id));
                        incomingStatus.remove(id);
                        System.out.println("Lo snapshot appena avviato da me " + id + " è terminato xke ne avevo solo 1 in input");
                    }
                } catch (NotBoundException | RemoteException e) {
                    e.printStackTrace();
                    throw new SnapEx();
                }
            }
       // }
    }

    //Per ogni snapshot attivo, se il nodo da cui riceviamo il messaggio è nello snap, salva il messaggio
    public void addMessage(String hostname, M message) {
        for (Snapshot<S, M> snapshot : snaps.values()){
            if (incomingStatus.get(snapshot.getId()).contains(hostname))
                snapshot.addMessage(message);
        }
    }

    private void serializeState(S state, String snapshotID) throws SnapEx {
        try (ObjectOutputStream objectOut = new ObjectOutputStream(new FileOutputStream(snapshotID+".state"))) {
            objectOut.writeObject(state);
            System.out.println("The state " + snapshotID + ".state" + " was successfully written to the file");
        } catch (Exception ex) {
            throw new SnapEx();
        }
    }

    private S readState(String snapshotID) throws SnapEx {
        S state;
        try (ObjectInputStream objectOut = new ObjectInputStream(new FileInputStream(snapshotID+".state"))) {
            state = (S) objectOut.readObject();
            System.out.println("The state " + snapshotID + ".state was successfully read from the file");
            return state;
        } catch (Exception ex) {
            throw new SnapEx();
        }
    }

    private S saveState(S state, String snapshotID) throws SnapEx {
        serializeState(state, snapshotID);
        return readState(snapshotID);
    }

    public void initiateSnapshot(String ip) throws SnapEx {
        //TODO genera id lamp clock
        String id = clock+"."+ip/* + ip della macchina*/;
        startSnapshot(id);
    }

    @Override
    public void printMsg() throws ServerNotActiveException {
        System.out.println("printMsg invoked from "+RemoteServer.getClientHost());
    }

    public Snapshot<S, M> restoreLast() throws SnapEx {
        File snapshots = new File(System.getProperty("user.dir"));
        List<String> snapnames = Arrays.stream(Objects.requireNonNull(snapshots.list())).filter(s -> s.matches("([^\\s]+(\\.(?i)(snap))$)")).collect(Collectors.toList());
        //snapnames.sort(String::compareTo);
        // System.out.println(snapnames.get(snapnames.size()-1));
        System.out.println(snapnames.get(0).split("((\\.(?i)(snap))$)")[0]);
        return readSnapshot(snapnames.get(0).split("((\\.(?i)(snap))$)")[0]);
    }

    //TODO UPDATE CONNECTIONS (da chiamare quando il nodo cambia le sue connessioni)

    public void restore() throws SnapEx {
        synchronized (node) {
            try {
                if (!pendingRestores.contains(RemoteServer.getClientHost()) && restoring) {
                    restoring = false;
                    pendingRestores = incomingInit();
                }
                pendingRestores.remove(RemoteServer.getClientHost());
                if (!restoring) {
                    node.restoreSnapshot(restoreLast());
                    restoring = true;
                    //chiama il restore degli altri sulla current epoch
                    for (Node connection : outgoingConnections) {
                        ((SnapInt<S, M>) LocateRegistry
                                .getRegistry(connection.getHost(), connection.getPort())
                                .lookup("SnapInt")).restore();//TODO CAMBIA NODE INT
                    }
                }
                if (pendingRestores.isEmpty()) {
                    restoring = false;
                    pendingRestores = incomingInit();
                }
            }
            catch (Exception e) {
                throw new SnapEx();
            }
        }
    }

    public boolean isRestoring(){return restoring;}

    public boolean discardMessage(String incomingConnection){
        return (isRestoring() && pendingRestores.contains(incomingConnection));
    }

    private Set<String> incomingInit(){
        Set<String> toRet = new HashSet<>();
        for (Node c : incomingConnections)
            toRet.add(c.getHost());
        return toRet;
    }
}
