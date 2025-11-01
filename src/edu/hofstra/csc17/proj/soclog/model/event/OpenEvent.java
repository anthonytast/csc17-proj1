package edu.hofstra.csc17.proj.soclog.model.event;

import java.time.Instant;

import edu.hofstra.csc17.proj.soclog.model.entity.ObjectInfo;
import edu.hofstra.csc17.proj.soclog.model.entity.ProcessInfo;

/**
 * Represents a file/resource open operation.
 */
public class OpenEvent extends Event {

    public OpenEvent(Instant timestamp, ProcessInfo subject, ObjectInfo object, String flags) {
        super(EventType.open, timestamp, subject, object, flags);
        // TODO: Add validation 
    }

}