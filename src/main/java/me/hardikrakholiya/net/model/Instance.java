package me.hardikrakholiya.net.model;

import java.io.Serializable;

public class Instance implements Serializable {
    private final String address;
    private final int port;

    public Instance(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "{address='" + address + '\'' +
                ", port=" + port +
                '}';
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) return false;
        if (!(other instanceof Instance))
            return false;
        if (other == this)
            return true;
        Instance otherInstance = (Instance) other;
        return this.getAddress().equals(otherInstance.getAddress()) &&
                this.getPort() == otherInstance.getPort();
    }
}
