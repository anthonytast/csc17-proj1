package edu.hofstra.csc17.proj.soclog.ingest.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import edu.hofstra.csc17.proj.soclog.model.event.Event;

public class EventParser {

    public static final class ParseResult {
        private final List<Event> events;
        private final List<String> errors;

        public ParseResult(List<Event> events, List<String> errors) {
            this.events = events;
            this.errors = errors;
        }

        public List<Event> getEvents() {
            return events;
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    public ParseResult parse(Path path) throws IOException {
        // TODO: Implement streaming parse with validation & error tracking.
        List<Event> events = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            long lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                // TODO: parse the raw data, validate, and either add to events or errors.
                lineNumber++;
            }
        }
        return new ParseResult(events, errors);
    }
}
