package fr.insalyon.creatis.gasw.parser.output;

import java.io.File;

import fr.insalyon.creatis.gasw.GaswException;
import fr.insalyon.creatis.gasw.GaswOutput;
import fr.insalyon.creatis.gasw.execution.GaswOutputParser;

public class DumpOutputParser extends GaswOutputParser {

    public DumpOutputParser(String jobID) {
        super(jobID);
    }
    
    @Override
    public GaswOutput getGaswOutput() throws GaswException {
        return null;
    }

    @Override
    protected void resubmit() throws GaswException {}

    public void parseStdout(File file) throws GaswException {
        parseStdOut(file);
    }
}
