package it.polimi.ds.ricciosorrentinotriuzzi.snaplib;

import org.jetbrains.annotations.*;

import java.io.*;
import java.rmi.registry.Registry;
import java.rmi.server.*;
import java.util.Map;


public class SnapLib implements SnapInt {
    private Map<String, Map<String, Boolean>> incomingStatus; //map(idSnap, map(inHost, flag_HoRicevutoIlMarker))
    private Map<String, Snapshot> snaps; //map(idSnap, Snap)

    public SnapLib(Registry r) throws Exception {
        r.bind("SnapInt",this);
        System.out.println("SnapLib configured");
    }

    public void saveSnapshot(Snapshot snap) throws SnapEx {
        try (ObjectOutputStream objectOut = new ObjectOutputStream(new FileOutputStream(snap.getId()+".snap"))) {
            objectOut.writeObject(snap);
            System.out.println("The snapshot " + snap.getId() + " was successfully written to the file");
        } catch (Exception ex) {
            throw new SnapEx();
        }
    }

    public Snapshot readSnapshot(String id) throws SnapEx {
        Snapshot snapshot = null;
        try (ObjectInputStream objectOut = new ObjectInputStream(new FileInputStream(id+".snap"))) {
            snapshot = (Snapshot) objectOut.readObject();
            System.out.println("The snapshot " + snapshot.getId() + " was successfully read from the file");
            return snapshot;
        } catch (Exception ex) {
            throw new SnapEx();
        }
    }


    @Override
    public void printMsg() throws ServerNotActiveException {
        System.out.println("printMsg invoked from "+RemoteServer.getClientHost());
    }

}
