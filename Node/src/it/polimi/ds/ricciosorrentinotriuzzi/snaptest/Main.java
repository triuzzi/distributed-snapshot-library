package it.polimi.ds.ricciosorrentinotriuzzi.snaptest;

import it.polimi.ds.ricciosorrentinotriuzzi.snaplib.Snapshot;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException{

        Snapshot<Message> snapshot = new Snapshot("id");
        Message message1 = new Message("metodo", new Class<?>[] {String.class}, new String[]{"stringaDiProva"});
        snapshot.addMessage(message1);
        Snapshot.saveSnapshot(snapshot, "snapshot.snap");
        snapshot = Snapshot.readSnapshot("snapshot.snap");

        for (Message message : snapshot.getMessages()) {
            Method method = Main.class.getMethod(message.getMethodName(), message.getParameterTypes());
            method.invoke(Main.class.getDeclaredConstructor().newInstance(), message.getParameters());
        }
    }

    public static void metodo (String msg){
        System.out.println(msg + "ritorno");
    }
}
