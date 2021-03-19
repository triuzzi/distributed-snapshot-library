package it.polimi.ds.ricciosorrentinotriuzzi.snaptest;

import java.io.Serializable;

public class State implements Serializable {
    int i;

    public State() {
        this.i = 10;
    }

    public int getI() {
        return i;
    }

    public void setI(int i) {
        this.i = i;
    }
}
