import java.util.HashSet;
import java.util.Set;

public class MainConnections {
    public static void main(String[] args){

        Connection c1 = new Connection("testHost", 3306, "IntTest");
        Set<Connection> connections = new HashSet<>();
        connections.add(c1);

        Node.saveConnections(connections, "b.txt");

        Set<Connection> connections2 = Node.readConnections("b.txt");
        System.out.println(connections2.toString());
    }
}