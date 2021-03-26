package it.polimi.ds.ricciosorrentinotriuzzi.snaplib;

import java.io.Serializable;

public interface ConnInt extends Serializable {
    Integer getPort();
    String getHost();
    String getName();
}

/*
public abstract class ConnInt implements Serializable {

    public abstract Integer getPort();

    public abstract String getHost();

    public abstract String getName();



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnInt connInt = (ConnInt) o;
        return  getHost().equals(connInt.getHost());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPort(), getHost(), getName());
    }
}
 */
