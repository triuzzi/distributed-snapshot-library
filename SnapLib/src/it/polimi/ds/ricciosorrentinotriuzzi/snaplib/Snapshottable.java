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

public abstract class Snapshottable<S extends Serializable, M extends Serializable> extends UnicastRemoteObject implements SnapInt {
    //metodi abstract che mi passino il riferimento (siamo sicuri di volerlo fare? Ci fidiamo dell'implementazione del metodo?)

    //map(idSnap, set(inHost)) se inHost è presente, vuol dire che non ho ancora ricevuto il marker da lui)
    private Map<String, Set<String>> incomingStatus;
    private Map<String, Snapshot<S, M>> runningSnapshots; //map(idSnap, Snap)
    private boolean restoring;
    private long clock;
    private Set<String> pendingRestores; //set di host in ingresso da cui devo ancora ricevere il marker se c'è una restore in corso.
    // Non ci serve una map con l'id della restore perché non possono esserci due restore attive in contemporanea data l'assunzione
    // che, avviato uno snapshot, se va a buon fine tutti nella rete avranno quello snapshot, e se non va a buon fine chi
    // l'aveva già avviato e/o salvato lo scarterà, riportandosi all'ultimo snapshot comunque, che è quello indicato dall'id
    // della restore

    //private Registry registry = LocateRegistry.createRegistry(1099);

    //TODO VEDERE SE VA CAMBIANDO
    public Snapshottable(Integer port) throws RemoteException, AlreadyBoundException {
        super(port);
        Registry r;
        try {
            r = LocateRegistry.createRegistry(port);
            System.out.println(r);
        } catch (RemoteException e) {
            r = LocateRegistry.getRegistry(port);
        }
        r.bind("SnapInt", this);
        incomingStatus = new HashMap<>();
        runningSnapshots = new HashMap<>();
        pendingRestores = new HashSet<>();
        clock = 0L;
        restoring = false;
        System.out.println("SnapLib configured");
        if (checkCrash()) { restore(null); } //se c'è stato un crash, avvia la restore dell'ultimo snapshot
        initCrashChecker();
    }

    public abstract S getState();
    public abstract String getHost();
    public abstract void restoreSnapshot(Snapshot<S, M> snapshot);
    public abstract Set<ConnInt> getInConn();
    public abstract Set<ConnInt> getOutConn();


    @Override
    public void startSnapshot(String id) {
        synchronized (this) {
            String markerReceivedFrom = null;
            try {
                markerReceivedFrom = RemoteServer.getClientHost();
            } catch (Exception e) {
                System.out.println("Snapshot iniziato di mia iniziativa");
            }

            //Se sto facendo la restore, posso avviare lo snapshot solo se il nodo che me l'ha richiesto è un nodo che ha già terminato la sua di restore,
            // in quando il suo stato sarà consistente con quello di cui ha fatto la restore. Tutti gli altri vanno ignorati.
            //Questo è per gestire il caso in cui un nodo A faccia partire la restore in una porzione della rete e un altro nodo B
            //faccia partire uno snapshot prima di ricevere il marker di restore da A, ma comunque dopo l'avvio della restore di A
            if (!restoring || (restoring && markerReceivedFrom != null && !pendingRestores.contains(markerReceivedFrom))) {
                if (incomingStatus.containsKey(id)) { //lo snap identificato da id è in corso
                    System.out.println("Lo snap " + id + " era già in corso");
                    if (markerReceivedFrom != null) { //se non sono io ad aver richiesto lo snap
                        incomingStatus.get(id).remove(markerReceivedFrom);
                        System.out.println("Ho ricevuto il marker da " + markerReceivedFrom + " e l'ho rimosso dal set");
                    }
                    System.out.println("Devo aspettare il marker ancora da:");
                    for (String s : incomingStatus.get(id)) {
                        System.out.println(s);
                    }
                    if (incomingStatus.get(id).isEmpty()) { //Se lo snapshot è finito
                        saveSnapshot(runningSnapshots.get(id));
                        incomingStatus.remove(id);
                        runningSnapshots.remove(id);
                        System.out.println("Lo snapshot " + id + " è terminato");
                    }
                } else { //è la prima volta che ricevo il marker id di startSnap (o sono io ad averlo fatto partire)
                    System.out.println("Inizio per la prima volta lo snapshot con id=" + id);
                    clock = Math.max(Long.parseLong(id.split("\\.")[0]) + 1, clock);
                    Snapshot<S, M> newSnapshot = new Snapshot<>(id, saveState(id));
                    runningSnapshots.put(id, newSnapshot);

                    //Inizializzo la mappa di connessioni in ingresso per questo snapshot
                    Set<String> awaitingTokenFrom = new HashSet<>();
                    for (ConnInt connInt : getInConn()) {
                        awaitingTokenFrom.add(connInt.getHost());
                    }
                    //Rimuovo chi mi ha mandato il token dal set di host da cui aspettare il marker
                    if (markerReceivedFrom != null) {
                        awaitingTokenFrom.remove(markerReceivedFrom);
                    }
                    incomingStatus.put(id, awaitingTokenFrom);

                    System.out.println("Avvio gli snap degli outgoing 🤯🥵");
                    try {
                        System.out.println("Outgoing nodes:");
                        for (ConnInt connInt : getOutConn()) {
                            System.out.println(connInt.getName() + " at " + connInt.getHost() + ":" + connInt.getPort());
                            SnapInt snapRemInt = ((SnapInt) LocateRegistry
                                    .getRegistry(connInt.getHost(), connInt.getPort())
                                    .lookup("SnapInt"));
                            System.out.println("Connessione a " + connInt.getHost() + " riuscita. Richiedo lo snap");
                            new Thread(() -> {
                                try {
                                    snapRemInt.startSnapshot(id);
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }
                            }).start();
                        }
                        //Se ho ricevuto il token dall'unico canale in ingresso
                        //System.out.println("set empty? "+incomingStatus.get(id).isEmpty());
                        if (incomingStatus.get(id).isEmpty()) {
                            saveSnapshot(runningSnapshots.get(id));
                            incomingStatus.remove(id);
                            runningSnapshots.remove(id);
                            System.out.println("Il mio snapshot locale " + id + " è già terminato (marker dall'unico ingresso)");
                        }
                    } catch (NotBoundException | RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    //TODO DA CAMBIARE
    //Per ogni snapshot attivo, se dal nodo da cui riceviamo il messaggio non ho ancora ricevuto il marker, salva il messaggio
    public synchronized void addMessage(String sender, M message) {
        for (Snapshot<S, M> snapshot : runningSnapshots.values()) {
            if (incomingStatus.get(snapshot.getId()).contains(sender))
                snapshot.addMessage(sender, message);
        }
        System.out.println("Message " + message + " received from " + sender + " added");
    }

    //Se c'è una restore in corso i messaggi vanno scartati (le chiamate RMI devono terminare immediatamente)
    public boolean shouldDiscard(String sender) {
        return (isRestoring() && pendingRestores.contains(sender));
    }

    //Salva lo stato per lo snapshot identificato da snapshotID
    @SuppressWarnings("unchecked")
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
        String id = clock + "." + getHost();
        startSnapshot(id);
    }


    @Override
    public void restore(String id) { //se id è null, viene fatta la restore sello snap più recente
        synchronized (this) {
            runningSnapshots = new HashMap<>();
            incomingStatus = new HashMap<>();
            String tokenReceivedFrom = null;
            try {
                tokenReceivedFrom = RemoteServer.getClientHost();
                System.out.println("Marker per il restore ricevuto da: " + tokenReceivedFrom);
            } catch (Exception e) {
                System.out.println("Restore iniziata di mia iniziativa");
            }
            try {
                //se ero in restore e tokenReceivedFrom mi aveva già mandato il marker
                //se ne ricevo un altro significa che c'è stato un altro crash e devo ricominciare da capo
                if (restoring && !pendingRestores.contains(tokenReceivedFrom)) {
                    restoring = false;
                }
                if (!restoring) {
                    pendingRestores = incomingInit();
                    //rimuovo chi mi ha mandato il marker dal set se non ho avviato io la restore
                    restoring = true;
                    Snapshot<S, M> toRestore = readSnapshot(id);
                    this.restoreSnapshot(toRestore);
                    //chiama il restore degli altri sulla current epoch
                    for (ConnInt connInt : getOutConn()) {
                        new Thread(() -> {
                            try {
                                ((SnapInt) LocateRegistry
                                        .getRegistry(connInt.getHost(), connInt.getPort())
                                        .lookup("SnapInt")).restore(toRestore.getId());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();
                    }
                }
                if (tokenReceivedFrom != null) {
                    pendingRestores.remove(tokenReceivedFrom);
                }
                // se non devo aspettare il marker più da nessuno la restore è terminata
                if (pendingRestores.isEmpty()) {
                    restoring = false;
                    System.out.println("Restore terminata!");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Set<String> incomingInit() {
        Set<String> toRet = new HashSet<>();
        for (ConnInt c : getInConn())
            toRet.add(c.getHost());
        return toRet;
    }

    private void saveSnapshot(Snapshot<S, M> snap) {
        try (ObjectOutputStream objectOut = new ObjectOutputStream(new FileOutputStream(snap.getId() + ".snap"))) {
            objectOut.writeObject(snap);
            System.out.println("The snapshot " + snap.getId() + " was successfully written to the file");
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    //TODO Potrebbe restituire null se il nodo che si è connesso non effettua lo snapshot in tempo prima che
    // venga chiamata la restore
    @SuppressWarnings("unchecked")
    private Snapshot<S, M> readSnapshot(String id) {
        Snapshot<S, M> toReturn = null;
        if (id == null) {
            File snapshots = new File(System.getProperty("user.dir"));
            //System.out.println("Snapshots list: " + snapshots.list());
            List<String> snapnames = Arrays.stream(Objects.requireNonNull(snapshots.list())).filter(s -> s.matches("([^\\s]+(\\.(?i)(snap))$)")).collect(Collectors.toList());
            snapnames.sort(String::compareTo);
            if (!snapnames.isEmpty()) {
                System.out.println("Lo snapshot selezionato per il restore è: " + snapnames.get(snapnames.size() - 1).split("((\\.(?i)(snap))$)")[0]);
                id = snapnames.get(snapnames.size() - 1).split("((\\.(?i)(snap))$)")[0];
            }
        } else deleteSnapshots(id);
        try (ObjectInputStream objectOut = new ObjectInputStream(new FileInputStream(id + ".snap"))) {
            toReturn = (Snapshot<S, M>) objectOut.readObject();
            System.out.println("The snapshot " + toReturn.getId() + " was successfully read from the file");
        } catch (Exception ex) { ex.printStackTrace(); return null; }
        clock = Long.parseLong(toReturn.getId().split("\\.")[0]) + 1;
        return toReturn;
    }

    /* Elimina gli snapshot con nome > since */
    @SuppressWarnings("unchecked")
    private void deleteSnapshots(String since) {
        File snapshotsDir = new File(System.getProperty("user.dir"));
        for (File snapshotFile : snapshotsDir.listFiles()) {
            if (snapshotFile.getName().matches("([^\\s]+(\\.(?i)(snap))$)") && snapshotFile.getName().compareTo(since + ".snap") > 0)
                snapshotFile.delete();
        }
    }

    public boolean isRestoring() {
        return restoring;
    }

    public void applyNetworkChange(){
        new Thread(this::initiateSnapshot).start();
    }

    private void initCrashChecker(){
        try (ObjectOutputStream objectOut = new ObjectOutputStream(new FileOutputStream("crash_reporter.dat"))) {
            objectOut.writeObject(Boolean.TRUE);
            System.out.println("The snapshot process has been successfully started.");
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    public void safeExit() {
        try (ObjectOutputStream objectOut = new ObjectOutputStream(new FileOutputStream("crash_reporter.dat"))) {
            objectOut.writeObject(Boolean.FALSE);
            System.out.println("The snapshot process has been successfully closed.");
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private boolean checkCrash() {
        try (ObjectInputStream objectIn = new ObjectInputStream(new FileInputStream("crash_reporter.dat"))) {
            return (boolean) objectIn.readObject();
        } catch (Exception ex) { return false; }
    }
}
