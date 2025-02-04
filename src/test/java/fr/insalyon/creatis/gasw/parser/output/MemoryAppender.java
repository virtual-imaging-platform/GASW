package fr.insalyon.creatis.gasw.parser.output;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

public class MemoryAppender extends AppenderSkeleton {
    private final List<String> logMessages = new ArrayList<>();

    @Override
    protected void append(LoggingEvent event) {
        logMessages.add(event.getRenderedMessage());
    }

    @Override
    public void close() {}

    @Override
    public boolean requiresLayout() {
        return false;
    }

    public List<String> getLogMessages() {
        return logMessages;
    }
}