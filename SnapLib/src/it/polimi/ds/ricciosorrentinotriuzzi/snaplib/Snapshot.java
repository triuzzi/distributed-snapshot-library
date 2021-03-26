package it.polimi.ds.ricciosorrentinotriuzzi.snaplib;

import java.io.*;
import java.util.*;
/*
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

 */

public class Snapshot<S extends Serializable, M extends Serializable> implements Serializable {
    private final String id;
    private S state;
    private Map<String, Queue<M>> channelStates;
    // String data l'assunzione che i nodi sono univ. ident. dai loro ip, e un canale connette il nodo corrente
    // a un solo altro nodo. Quindi il canale può essere identificato dall'ip del nodo a cui ci si connette

    public Snapshot(String id, S state) {
        this.id = id;
        this.state = state;
        channelStates = new HashMap<>();
    }

    public boolean addMessage(String channel, M message){
        Queue<M> messages = channelStates.get(channel);
        if (messages == null){
            messages = new LinkedList<>();
            channelStates.put(channel, messages);
        }
        return messages.add(message);
    }

    public String getId() {
        return id;
    }

    public S getState() {
        return state;
    }

    public Map<String, Queue<M>> getChannelStates() {
        return channelStates;
    }

    public Queue<M> getMessages() {
        Queue<M> toReturn = new LinkedList<>();
        for (Queue<M> messageQueue : channelStates.values()){
            toReturn.addAll(messageQueue);
        }
        return toReturn;
    }
}

