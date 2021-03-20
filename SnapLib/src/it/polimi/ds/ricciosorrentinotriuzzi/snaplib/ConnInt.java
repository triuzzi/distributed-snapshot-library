package it.polimi.ds.ricciosorrentinotriuzzi.snaplib;

import java.io.Serializable;

public interface ConnInt extends Serializable {
    int getPort();
    String getHost();
    String getName();
}
