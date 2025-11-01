package edu.hofstra.csc17.proj.soclog.model.event;

import java.time.Instant;

import edu.hofstra.csc17.proj.soclog.model.entity.NetworkInfo;
import edu.hofstra.csc17.proj.soclog.model.entity.ProcessInfo;

/**
 * Represents a network receive operation.
 * Note: Uses NetworkInfo as object type and stores it for network-specific operations.
 */
public class ReceiveFromEvent extends Event {

    public ReceiveFromEvent(Instant timestamp, ProcessInfo subject, NetworkInfo networkInfo, String flags) {
        super(EventType.receivefrom, timestamp, subject, networkInfo, flags);
        // TODO: Add validation
    }

    public NetworkInfo getNetworkInfo() {
        return (NetworkInfo) getObject();
    }

}