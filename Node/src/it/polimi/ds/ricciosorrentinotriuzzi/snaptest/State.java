package it.polimi.ds.ricciosorrentinotriuzzi.snaptest;

import java.io.Serializable;

public class State implements Serializable {
    int balance;

    public State() {
        this.balance = 100;
    }

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

    public void increase(int diff) {
        balance += diff;
    }

    public void decrease(int diff) {
        balance -= diff;
    }
}
