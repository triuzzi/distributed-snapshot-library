import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class Node implements Serializable {
    private Set<Connection> connections;

    public Node(){
        connections = new HashSet<>();
    }

    public static void saveConnections(Set<Connection> connections, String fileName){
        try (FileOutputStream fileOut = new FileOutputStream(fileName);
             ObjectOutputStream objectOut = new ObjectOutputStream(fileOut)) {
            objectOut.writeObject(connections);
        } catch (Exception e) {
            System.out.println("Raised exception while writing connections on file: " + e.toString());
            e.printStackTrace();
        }
    }

    public static Set<Connection> readConnections(String fileName) {
        Set<Connection> connections = null;
        try (FileInputStream fileOut = new FileInputStream(fileName);
             ObjectInputStream objectOut = new ObjectInputStream(fileOut)) {
            connections = (Set<Connection>) objectOut.readObject();
        } catch (Exception e) {
            System.out.println("Raised exception while reading connections from file: " + e.toString());
            e.printStackTrace();
        }
        return connections;
    }
}