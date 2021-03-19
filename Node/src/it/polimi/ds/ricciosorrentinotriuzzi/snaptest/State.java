package it.polimi.ds.ricciosorrentinotriuzzi.snaptest;

import java.io.Serializable;

public class State implements Serializable {
    String i;

    public State() {
        this.i = "ciao";
    }

    public String getI() {
        return i;
    }

    public void setI(String i) {
        this.i = i;
    }
}
