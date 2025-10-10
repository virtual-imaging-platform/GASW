package fr.insalyon.creatis.gasw.parser.output;

import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

public class MemoryAppender extends AppenderBase<ILoggingEvent> {
    private final List<String> logMessages = new ArrayList<>();

    @Override
    protected void append(ILoggingEvent event) {
        logMessages.add(event.getFormattedMessage());
    }

    public List<String> getLogMessages() {
        return logMessages;
    }
}