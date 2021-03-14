package it.polimi.ds.ricciosorrentinotriuzzi.snaplib;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

public class Snapshot<S extends Serializable, M extends Serializable> implements Serializable {
    private final String id;
    private S state;
    private List<M> messages;

    public Snapshot(String id, S state) {
        this.id = id;
        this.state = state;
        messages = new LinkedList<>();
    }

    public List<M> addMessage(M message){
        messages.add(message);
        return messages;
    }

    public String getId() {
        return id;
    }

    public S getState() {
        return state;
    }

    public List<M> getMessages() {
        return messages;
    }
}
