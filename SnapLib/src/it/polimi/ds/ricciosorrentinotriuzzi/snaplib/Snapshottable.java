package it.polimi.ds.ricciosorrentinotriuzzi.snaplib;

import java.io.Serializable;
import java.rmi.registry.Registry;

public abstract class Snapshottable<S extends Serializable,M extends Serializable> implements NodeInt {
    SnapLib<S, M> snapLib;

    public abstract S getState();

    public abstract void restoreSnapshot(Snapshot<S,M> snapshot);

    public boolean discardMessage(){
        return snapLib.isRestoring();//togliendolo non serve la classe astratta
    }
}
