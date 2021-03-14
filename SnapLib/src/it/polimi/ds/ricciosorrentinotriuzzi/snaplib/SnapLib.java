package it.polimi.ds.ricciosorrentinotriuzzi.snaplib;

import java.io.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class SnapLib <S extends Serializable, M extends Serializable> implements SnapInt<S, M> {
    private Set<Connection> incomingConnections;
    private Set<Connection> outgoingConnections;
    private Map<String, Set<String>> incomingStatus; //map(idSnap, set(inHost) se è presente, vuol dire che non ho ancora ricevuto il marker)
    private Map<String, Snapshot<S, M>> snaps; //map(idSnap, Snap)
    private S node;
    private Double clock;

    public SnapLib(Registry r, Set<Connection> incomingConnections, Set<Connection> outgoingConnections, S node) throws Exception {
        r.bind("SnapInt",this);
        System.out.println("SnapLib configured");
        incomingStatus = new HashMap<>();
        snaps = new HashMap<>();
        clock = 0D;
        this.incomingConnections = incomingConnections;
        this.outgoingConnections = outgoingConnections;
        this.node = node;
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
        synchronized (node) {
            if (incomingStatus.containsKey(id)) {
                try {
                    incomingStatus.get(id).remove(RemoteServer.getClientHost());
                } catch (ServerNotActiveException e) {
                    e.printStackTrace();
                    throw new SnapEx();
                }
                //Se lo snapshot è finito
                if (incomingStatus.get(id).isEmpty()) {
                    saveSnapshot(snaps.get(id));
                    incomingStatus.remove(id);
                }
            } else {
                clock = Math.max(Double.parseDouble(id.split("\\.")[0]) + 1, clock);
                Snapshot<S, M> newSnapshot = new Snapshot<>(id, saveState(node, id));
                System.out.println("Non sono lo stesso, vero? : " + node == saveState(node, id));
                snaps.put(id, newSnapshot);
                //Inizializzo la mappa di connessioni in ingresso per questo snapshot
                Set<String> incoming = new HashSet<>();
                for (Connection connection : incomingConnections) {
                    incoming.add(connection.getHost());
                }
                incomingStatus.put(id, incoming);

                //Avvia gli snap degli outgoing
                try {
                    for (Connection connection : outgoingConnections) {
                        ((SnapInt<S, M>) LocateRegistry
                                .getRegistry(connection.getHost(), connection.getPort())
                                .lookup("SnapInt")).startSnapshot(id);
                    }
                } catch (NotBoundException | RemoteException e) {
                    e.printStackTrace();
                    throw new SnapEx();
                }
            }
        }
    }

    //Per ogni snapshot attivo, se il nodo da cui riceviamo il messaggio è nello snap, salva il messaggio
    public void addMessage(String hostname, M message){
        for (Snapshot<S, M> snapshot : snaps.values()){
            if (incomingStatus.get(snapshot.getId()).contains(hostname))
                snapshot.addMessage(message);
        }
    }

    private void serializeState(S state, String snapshotID)throws SnapEx {
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

    private S saveState(S state, String snapshotID) throws SnapEx{
        serializeState(state, snapshotID);
        return readState(snapshotID);
    }

    @Override
    public void initiateSnapshot(String ip) throws SnapEx {
        //TODO genera id lamp clock
        String id = String.valueOf(clock).split("\\.")[0]+"."+ip/* + ip della macchina*/;
        startSnapshot(id);
    }

    @Override
    public void printMsg() throws ServerNotActiveException {
        System.out.println("printMsg invoked from "+RemoteServer.getClientHost());
    }

    //TODO UPDATE CONNECTIONS (da chiamare quando il nodo cambia le sue connessioni)
}
