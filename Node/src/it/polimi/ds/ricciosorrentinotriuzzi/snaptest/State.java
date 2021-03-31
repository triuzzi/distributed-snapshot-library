package it.polimi.ds.ricciosorrentinotriuzzi.snaptest;

import it.polimi.ds.ricciosorrentinotriuzzi.snaplib.ConnInt;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class State implements Serializable {
    Integer balance;
    private Set<ConnInt> incomingConnections;
    private Set<ConnInt> outgoingConnections;

    public State() {
        incomingConnections = new HashSet<>();
        outgoingConnections = new HashSet<>();
        this.balance = 100;
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

    public Set<ConnInt> getIncomingConnections() {
        return incomingConnections;
    }

    public Set<ConnInt> getOutgoingConnections() {
        return outgoingConnections;
    }

}
