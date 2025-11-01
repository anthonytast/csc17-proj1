package edu.hofstra.csc17.proj.soclog.model.entity;

import java.util.Objects;

/**
 * Represents network endpoint information for sendto/receivefrom events.
 */
public class NetworkInfo extends ObjectInfo {
    private final String ipAddress;
    private final int port;
    private final String protocol;

    public NetworkInfo(String ipAddress, int port, String protocol) {
        this.ipAddress = Objects.requireNonNull(ipAddress, "IP address cannot be null");
        this.port = port;
        this.protocol = Objects.requireNonNull(protocol, "Protocol cannot be null");
        // TODO: Add validation for IP address format and port range
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getEndpoint() {
        return ipAddress + ":" + port;
    }

    @Override
    public String getDisplayName() {
        return getEndpoint() + " (" + protocol + ")";
    }

    @Override
    public String getCanonicalId() {
        return "network:" + getEndpoint() + ":" + protocol;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetworkInfo that = (NetworkInfo) o;
        return port == that.port &&
               Objects.equals(ipAddress, that.ipAddress) &&
               Objects.equals(protocol, that.protocol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipAddress, port, protocol);
    }

    @Override
    public String toString() {
        return "NetworkInfo{" +
               "endpoint='" + getEndpoint() + '\'' +
               ", protocol='" + protocol + '\'' +
               '}';
    }
}