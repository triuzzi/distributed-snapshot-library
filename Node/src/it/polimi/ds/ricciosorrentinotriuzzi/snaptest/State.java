package it.polimi.ds.ricciosorrentinotriuzzi.snaptest;

import java.io.Serializable;

public class State implements Serializable {
    Integer balance;

    public State() {
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
}
