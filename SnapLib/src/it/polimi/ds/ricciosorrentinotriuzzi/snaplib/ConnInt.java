package it.polimi.ds.ricciosorrentinotriuzzi.snaplib;

import java.io.Serializable;

public interface ConnInt extends Serializable {
    Integer getPort();
    String getHost();
    String getName();
}

