import java.io.Serializable;

public class Connection implements Serializable {
    private String host;
    private int port;
    private String name;

    public Connection(String host, int port, String name) {
        this.host = host;
        this.port = port;
        this.name = name;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public String getName() {
        return name;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Connection{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Connection that = (Connection) o;

        if (port != that.port) return false;
        if (host != null ? !host.equals(that.host) : that.host != null) return false;
        return name != null ? name.equals(that.name) : that.name == null;
    }

    @Override
    public int hashCode() {
        int result = host != null ? host.hashCode() : 0;
        result = 31 * result + port;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}
