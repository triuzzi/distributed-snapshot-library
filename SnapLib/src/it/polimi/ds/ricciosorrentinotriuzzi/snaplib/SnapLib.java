package it.polimi.ds.ricciosorrentinotriuzzi.snaplib;

import org.jetbrains.annotations.*;
import java.rmi.*;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;


public class SnapLib implements SnapInt {
    private SnapLib instance = new SnapLib();

    public SnapLib getInstance() {return instance;}

    public void configure(@NotNull Registry r) throws RemoteException, ServerNotActiveException {
        System.out.println("Configuring SnapLib for "+RemoteServer.getClientHost());
        r.rebind("SnapInt",getInstance());
        System.out.println("SnapLib configured");
    }

    @Override
    public void printMsg() throws ServerNotActiveException {
        System.out.println("printMsg invoked from "+RemoteServer.getClientHost());
    }

}
