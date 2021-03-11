package it.polimi.ds.ricciosorrentinotriuzzi.snaplib;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

public class Snapshot<T extends Serializable> implements Serializable {
    private final String id;
    private List<T> messages;

    public Snapshot(String id) {
        this.id = id;
        messages = new LinkedList<>();
    }

    public List<T> addMessage(T message){
        messages.add(message);
        return messages;
    }

    public String getId() {
        return id;
    }

    public List<T> getMessages() {
        return messages;
    }

    public static void saveSnapshot(Snapshot snapshot, String filepath){
        try (FileOutputStream fileOut = new FileOutputStream(filepath);
             ObjectOutputStream objectOut = new ObjectOutputStream(fileOut)) {
            objectOut.writeObject(snapshot);
            System.out.println("The snapshot " + snapshot.getId() + " was successfully written to the file " + filepath);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static Snapshot readSnapshot(String filepath) {
        Snapshot snapshot = null;
        try (FileInputStream fileOut = new FileInputStream(filepath);
             ObjectInputStream objectOut = new ObjectInputStream(fileOut)) {
            snapshot = (Snapshot) objectOut.readObject();
            System.out.println("The snapshot " + snapshot.getId() + " was successfully read from the file " + filepath);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return snapshot;
    }
}
