
package it.polimi.ds.ricciosorrentinotriuzzi.snaptest;

import it.polimi.ds.ricciosorrentinotriuzzi.snaplib.ConnInt;

public class Connection implements ConnInt {
    private String host;
    private Integer port;
    private String name;

    public Connection(String outHostname, Integer port, String name) {
        this.host = outHostname;
        this.port = port;
        this.name = name;
    }

    public Integer getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public String getName() {
        return name;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnInt connInt = (ConnInt) o;
        return  getHost().equals(connInt.getHost());
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (host != null ? host.hashCode() : 0);
        result = 31 * result + port;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }


}


