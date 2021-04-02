package it.polimi.ds.ricciosorrentinotriuzzi.snaptest;

import it.polimi.ds.ricciosorrentinotriuzzi.snaplib.ConnInt;
import java.io.Serializable;
import java.util.*;

public class State implements Serializable {
    private Map<String, Integer> ledger; //map(cliente, saldo)
    private Set<ConnInt> incomingConnections;
    private Set<ConnInt> outgoingConnections;

    public State() {
        incomingConnections = new HashSet<>();
        outgoingConnections = new HashSet<>();
        ledger = new HashMap<>();
    }

    public void emptyLedger() {
        ledger = new HashMap<>();
    }

    public Map<String, Integer> getLedger() {
        return ledger;
    }

    public void newCustomer(String id, Integer initialBalance) { ledger.putIfAbsent(id, initialBalance); }

    public void sumBalance(String customer, Integer quantity) {
        ledger.put(customer, ledger.get(customer)+quantity);
    }

    public Integer getCBalance(String customer) {
        return ledger.get(customer);
    }

    public Set<ConnInt> getIncomingConnections() { return incomingConnections; }

    public Set<ConnInt> getOutgoingConnections() { return outgoingConnections; }

}
