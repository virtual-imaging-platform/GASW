package fr.insalyon.creatis.gasw.parser.output;

import java.io.File;
import java.io.IOException;
import java.util.List;

import fr.insalyon.creatis.gasw.GaswConfiguration;
import fr.insalyon.creatis.gasw.GaswException;
import fr.insalyon.creatis.gasw.GaswNotification;
import fr.insalyon.creatis.gasw.GaswOutput;
import fr.insalyon.creatis.gasw.dao.JobDAO;
import fr.insalyon.creatis.gasw.dao.JobMinorStatusDAO;
import fr.insalyon.creatis.gasw.dao.NodeDAO;
import fr.insalyon.creatis.gasw.execution.GaswOutputParser;
import fr.insalyon.creatis.gasw.execution.GaswParsingContext;
import fr.insalyon.creatis.gasw.plugin.ListenerPlugin;
import org.springframework.stereotype.Service;

@Service
public class DumpOutputParser extends GaswOutputParser {

    protected DumpOutputParser(GaswConfiguration config, GaswNotification gaswNotification,
                               JobDAO jobDAO, JobMinorStatusDAO jobMinorStatusDAO, NodeDAO nodeDAO, List<ListenerPlugin> listenerPlugins) {
        super(config, gaswNotification, jobDAO, jobMinorStatusDAO, nodeDAO, listenerPlugins);
    }
    
    @Override
    public GaswOutput getGaswOutput(GaswParsingContext context) throws GaswException {
        return null;
    }

    @Override
    protected void resubmit() throws GaswException {}

    public int parseStdout(File file, GaswParsingContext context) throws IOException {
        return parseStdOut(file, context);
    }
}
