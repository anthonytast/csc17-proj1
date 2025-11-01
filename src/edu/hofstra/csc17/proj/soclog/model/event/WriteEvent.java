package edu.hofstra.csc17.proj.soclog.model.event;

import java.time.Instant;

import edu.hofstra.csc17.proj.soclog.model.entity.ObjectInfo;
import edu.hofstra.csc17.proj.soclog.model.entity.ProcessInfo;

/**
 * Represents a file write operation.
 */
public class WriteEvent extends Event {

    public WriteEvent(Instant timestamp, ProcessInfo subject, ObjectInfo object, String flags) {
        super(EventType.write, timestamp, subject, object, flags);
        // TODO: Add validation
    }

}