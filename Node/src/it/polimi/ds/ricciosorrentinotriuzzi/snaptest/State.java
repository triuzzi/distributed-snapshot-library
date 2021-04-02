package it.polimi.ds.ricciosorrentinotriuzzi.snaptest;

import it.polimi.ds.ricciosorrentinotriuzzi.snaplib.ConnInt;
import java.io.Serializable;
import java.util.*;

public class State implements Serializable {
    Integer balance;
    private Map<String, Integer> ledger; //map(cliente, saldo)
    private Set<ConnInt> incomingConnections;
    private Set<ConnInt> outgoingConnections;

    public State() {
        incomingConnections = new HashSet<>();
        outgoingConnections = new HashSet<>();
        ledger = new HashMap<>();
        this.balance = 100;
    }

    public Map<String, Integer> getLedger() {
        return ledger;
    }

    public Integer getBalance() {
        return balance;
    }

    public void setBalance(Integer balance) {
        this.balance = balance;
    }

    public void increase(Integer diff) {
        balance += diff;
    }

    public void decrease(Integer diff) {
        balance -= diff;
    }

    public void newCustomer(String id) { ledger.putIfAbsent(id, 0); }

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
