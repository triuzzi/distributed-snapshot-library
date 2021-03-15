package it.polimi.ds.ricciosorrentinotriuzzi.snaptest;

import it.polimi.ds.ricciosorrentinotriuzzi.snaplib.ConnectionInt;

import java.io.Serializable;

public class Connection implements Serializable, ConnectionInt {
    private boolean outgoing;
    private String host;
    private Integer port;
    private String name;

    public Connection(String inHostname, String name) {
        this.outgoing = false;
        this.host = inHostname;
        this.port = null;
        this.name = name;
    }

    public Connection(String outHostname, Integer port, String name) {
        this.outgoing = true;
        this.host = outHostname;
        this.port = port;
        this.name = name;
    }

    public Connection(boolean outgoing, String host, Integer port, String name) {
        this.outgoing = outgoing;
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

    public boolean isOutgoing() {
        return outgoing;
    }

    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        if (!super.equals(object)) return false;

        Connection that = (Connection) object;

        if (outgoing != that.outgoing) return false;
        if (host != null ? !host.equals(that.host) : that.host != null) return false;
        if (port != null ? !port.equals(that.port) : that.port != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (outgoing ? 1 : 0);
        result = 31 * result + (host != null ? host.hashCode() : 0);
        result = 31 * result + (port != null ? port.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}
