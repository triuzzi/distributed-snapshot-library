import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
public class Main {
    public static void main(String[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException{
        Snapshot snapshot = new Snapshot("id");
        Message message1 = new Message("metodo", new Class<?>[] {String.class}, new String[]{"stringaDiProva"});
        snapshot.addMessage(message1);
        Snapshot.saveSnapshot(snapshot, "/Users/gian/Desktop/snapshot.snap");
        snapshot = Snapshot.readSnapshot("/Users/gian/Desktop/snapshot.snap");

        for (Message message : snapshot.getMessages()) {
            Method method = Main.class.getMethod(message.getMethodName(), message.getParameterTypes());
            method.invoke(Main.class.getDeclaredConstructor().newInstance(), message.getParameters());
        }
    }

    public static void metodo (String msg){
        System.out.println(msg + "ritorno");
    }
}
