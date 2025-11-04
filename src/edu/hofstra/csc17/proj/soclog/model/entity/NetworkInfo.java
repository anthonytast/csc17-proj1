package edu.hofstra.csc17.proj.soclog.model.entity;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Represents network endpoint information for sendto/receivefrom events.
 */
public class NetworkInfo extends ObjectInfo {
    private static final Pattern IPV4_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );
    
    private final String ipAddress;
    private final int port;
    private final String protocol;

    public NetworkInfo(String ipAddress, int port, String protocol) {
        this.ipAddress = Objects.requireNonNull(ipAddress, "IP address cannot be null");
        this.port = port;
        
        // Normalize protocol to uppercase (case-insensitive)
        Objects.requireNonNull(protocol, "Protocol cannot be null");
        String protocolUpper = protocol.toUpperCase();
        
        // Validate IP address format (IPv4)
        if (!IPV4_PATTERN.matcher(ipAddress).matches()) {
            throw new IllegalArgumentException("Invalid IPv4 address format: " + ipAddress);
        }
        
        // Validate port range (1-65535)
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1-65535, got: " + port);
        }
        
        // Validate protocol (must be TCP, UDP, or ICMP)
        if (!protocolUpper.equals("TCP") && !protocolUpper.equals("UDP") && !protocolUpper.equals("ICMP")) {
            throw new IllegalArgumentException("Protocol must be TCP, UDP, or ICMP, got: " + protocol);
        }
        
        // Store normalized protocol
        this.protocol = protocolUpper;
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