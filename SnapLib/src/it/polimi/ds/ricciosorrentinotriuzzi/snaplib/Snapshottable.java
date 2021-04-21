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
    //variabili da cambiare per i test, non sarebbero incluse nella libreria
    public int sleepSnapshot = 0;
    public int sleepRestore = 0;

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

    public Snapshottable(Integer port) throws RemoteException, AlreadyBoundException {
        super(port);
        Registry r;
        try {
            r = LocateRegistry.createRegistry(port);
            //System.out.println(r);
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
        if (hasCrashed()) {
            restore(null);
        } //se c'è stato un crash, avvia la restore dell'ultimo snapshot
        resetCrashDetector();
        //non c'è un thread periodico solo per la dimostrazione
    }


    public abstract S getState();

    public abstract String getHost();

    public abstract Set<ConnInt> getInConn();

    public abstract Set<ConnInt> getOutConn();

    // L'implementazione della restore prevere il ripristino dello stato e deve gestire i messaggi contenuti nello snapshot
    public abstract void restoreSnapshot(Snapshot<S, M> snapshot);

    // Crea un nuovo snapshot. Questo sarà identificato dal lamport clock corrente e dall'id del nodo corrente
    public void initiateSnapshot() {
        String id = clock + "." + getHost();
        startSnapshot(id);
    }

    // Funzione remota per la ricezione dei token di snapshot. Esegue lo snapshot locale ed inoltra il token sui canali in uscita
    @Override
    public synchronized void startSnapshot(String id) {
        String markerReceivedFrom = null;
        try {
            markerReceivedFrom = RemoteServer.getClientHost();
        } catch (Exception e) {
            System.out.println("Snapshot iniziato di mia iniziativa");
        }

        // Se sto facendo la restore, posso avviare lo snapshot solo se il nodo che me l'ha richiesto è un nodo che ha
        // già terminato la sua di restore,
        // in quando il suo stato sarà consistente con quello ripristinato dallo snapshot a cui convergerà la rete.
        // I nodi che non rispettano questa condizione, e che invece richiedono uno snapshot, vanno ignorati.
        // Questo è per gestire il caso in cui un nodo A faccia partire la restore in una porzione della rete e un altro nodo B
        // faccia partire uno snapshot prima di ricevere il marker di restore da A, ma comunque dopo l'avvio della restore di A
        if (!restoring || (restoring && markerReceivedFrom == null) || (restoring && markerReceivedFrom != null && !pendingRestores.contains(markerReceivedFrom))) {
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
                    System.out.println("Lo snapshot " + id + " è terminato\n\n");
                }
            } else { // Il nodo riceve per la prima volta il token associato all'id, oppure è il nodo stesso ad avviare
                // lo snapshot con quell'id
                System.out.println("Inizio per la prima volta lo snapshot con id=" + id);
                clock = Math.max(Long.parseLong(id.split("\\.")[0]) + 1, clock);
                Snapshot<S, M> newSnapshot = new Snapshot<>(id, saveState(id));
                runningSnapshots.put(id, newSnapshot);

                // Inizializzo la mappa di connessioni in ingresso per questo snapshot
                Set<String> awaitingTokenFrom = new HashSet<>();
                for (ConnInt connInt : getInConn()) {
                    awaitingTokenFrom.add(connInt.getHost());
                }
                // Rimuovo il mittente dal set di nodi in ingresso da cui aspettare il token associato allo snapshot
                if (markerReceivedFrom != null) {
                    awaitingTokenFrom.remove(markerReceivedFrom);
                }
                incomingStatus.put(id, awaitingTokenFrom);

                System.out.println("Invio il token di snapshot ai nodi in uscita");
                try {
                    // System.out.println("Outgoing nodes:");
                    for (ConnInt connInt : getOutConn()) {
                        // System.out.println(connInt.getName() + " at " + connInt.getHost() + ":" + connInt.getPort());
                        SnapInt snapRemInt = ((SnapInt) LocateRegistry
                                .getRegistry(connInt.getHost(), connInt.getPort())
                                .lookup("SnapInt"));
                        System.out.println("Connessione a " + connInt.getHost() + " riuscita. Richiedo lo snapshot");
                        new Thread(() -> {
                            try {
                                Thread.sleep(sleepSnapshot);
                                snapRemInt.startSnapshot(id);
                            } catch (RemoteException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        }).start();
                    }
                    // Se il nodo ha una singola connessione in ingresso, lo snapshot può considerarsi terminato
                    if (incomingStatus.get(id).isEmpty()) {
                        saveSnapshot(runningSnapshots.get(id));
                        incomingStatus.remove(id);
                        runningSnapshots.remove(id);
                        System.out.println("Il mio snapshot locale " + id + " è già terminato (marker dall'unico ingresso)\n\n");
                    }
                } catch (NotBoundException | RemoteException e) { e.printStackTrace(); }
            }
        }
        else {
            System.out.println("È in corso una restore, lo snapshot " + id + " è stato scartato");
        }

    }

    // Per ogni snapshot attivo, se dal nodo da cui riceviamo il messaggio non ho ancora ricevuto il marker,
    // salva il messaggio nel relativo snapshot
    public void addMessage(String sender, M message) {
        for (Snapshot<S, M> snapshot : runningSnapshots.values()) {
            if (incomingStatus.get(snapshot.getId()).contains(sender))
                snapshot.addMessage(sender, message);
        }
        System.out.println("Message " + message + " received from " + sender + " added");
    }

    @Override
    public synchronized void restore(String id) { //se id è null, viene fatta la restore sello snap più recente, altrimenti la restore viene fatta
        // dello snapshot che ha l'id specificato
        String tokenReceivedFrom = null;
        Snapshot<S, M> toRestore = null;
        // Innanzitutto, dobbiamo fare in modo che tra lo svuotamento delle due map ed il set di restoring (a true) non
        // venga processato alcun messaggio di snapshot, che altrimenti non sarebbe ignorato
        // Blocco synchronized perché se si ricevono altri messaggi
        try {
            tokenReceivedFrom = RemoteServer.getClientHost();
            System.out.println("Marker per il restore ricevuto da: " + tokenReceivedFrom);
        } catch (Exception e) {
            System.out.println("Restore iniziata di mia iniziativa");
        }

        if (restoring && !pendingRestores.contains(tokenReceivedFrom))
            restoring = false;
            //se ero in restore e tokenReceivedFrom mi aveva già mandato il marker
            //se ne ricevo un altro significa che c'è stato un altro crash e devo ricominciare da capo la restore

        if (!restoring) {
            //Avvio la restore e inizializzo i set di snapshot, rendendoli impossibili
            restoring = true;
            runningSnapshots = new HashMap<>();
            incomingStatus = new HashMap<>();
            toRestore = readSnapshot(id);

            //chiama il restore degli altri sulla current epoch
            for (ConnInt connInt : getOutConn()) {
                Snapshot<S, M> finalToRestore = toRestore;
                new Thread(() -> {
                    //try { Thread.sleep(15_000); } catch (InterruptedException e) { e.printStackTrace(); }
                    System.out.println("Invio il marker di restore a " + connInt.getHost());
                    try {
                        Thread.sleep(sleepRestore);
                        ((SnapInt) LocateRegistry
                                .getRegistry(connInt.getHost(), connInt.getPort())
                                .lookup("SnapInt")).restore(finalToRestore.getId());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
            this.restoreSnapshot(toRestore);
            pendingRestores = incomingInit();
        }

        if (tokenReceivedFrom != null) {
            //rimuovo chi mi ha mandato il marker dal set se non ho avviato io la restore
            pendingRestores.remove(tokenReceivedFrom);
        }

        System.out.println("Mi mancano ancora i marker di restore di");
        for (String waiting : pendingRestores) {
            System.out.println(waiting);
        }
        // se non devo aspettare il marker più da nessuno la restore è terminata
        if (pendingRestores.isEmpty()) {
            restoring = false;
            System.out.println("Restore terminata!\n\n");
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
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Non può restituire null perché dopo la connessione un nodo avrà sempre almeno uno snapshot,
    // che è quello fatto prtire alla connessione
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
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        clock = Long.parseLong(toReturn.getId().split("\\.")[0]) + 1;
        return toReturn;
    }

    /* Elimina gli snapshot con nome > since */
    @SuppressWarnings("unchecked")
    private void deleteSnapshots(String since) {
        boolean atLeastOne = false;
        File snapshotsDir = new File(System.getProperty("user.dir"));
        for (File snapshotFile : snapshotsDir.listFiles()) {
            if (snapshotFile.getName().matches("([^\\s]+(\\.(?i)(snap))$)") && snapshotFile.getName().compareTo(since + ".snap") > 0) {
                snapshotFile.delete();
                atLeastOne = true;
            }
        }
        if (atLeastOne)
            System.out.println("Gli snapshot successivi a " + since + "sono stati eliminati, essendo stati avviati " +
                "mentre era in corso una restore");
    }

    public boolean isRestoring() {
        return restoring;
    }

    public void applyNetworkChange() {
        new Thread(this::initiateSnapshot).start();
    }

    public void resetCrashDetector() {
        try (ObjectOutputStream objectOut = new ObjectOutputStream(new FileOutputStream("crash_reporter.dat"))) {
            objectOut.writeObject(Boolean.TRUE);
            //System.out.println("The process has successfully started.");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void safeExit() {
        try (ObjectOutputStream objectOut = new ObjectOutputStream(new FileOutputStream("crash_reporter.dat"))) {
            objectOut.writeObject(Boolean.FALSE);
            System.out.println("The process has been successfully closed");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public boolean hasCrashed() {
        try (ObjectInputStream objectIn = new ObjectInputStream(new FileInputStream("crash_reporter.dat"))) {
            return (boolean) objectIn.readObject();
        } catch (Exception ex) {
            return false;
        }
    }


    @Override
    public long getClock() {
        return clock;
    }


    protected void joinNetwork(String withHost, Integer port) throws Exception {
        clock = ((SnapInt) LocateRegistry
                .getRegistry(withHost, port)
                .lookup("SnapInt")).getClock();
        applyNetworkChange();
    }


    // Se c'è una restore in corso i messaggi vanno scartati (le chiamate RMI devono terminare immediatamente)
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

}
