/* Copyright CNRS-CREATIS
 *
 * Rafael Ferreira da Silva
 * rafael.silva@creatis.insa-lyon.fr
 * http://www.rafaelsilva.com
 *
 * This software is governed by the CeCILL  license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */
package fr.insalyon.creatis.gasw.parser;

import fr.insalyon.creatis.gasw.GaswConfiguration;
import fr.insalyon.creatis.gasw.GaswConstants;
import fr.insalyon.creatis.gasw.GaswException;
import fr.insalyon.creatis.gasw.GaswInput;
import fr.insalyon.creatis.gasw.GaswUpload;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 *
 * @author Rafael Ferreira da Silva
 */
public class GaswParser extends DefaultHandler {

    private static final Logger logger = Logger.getLogger("fr.insalyon.creatis.gasw");
    private XMLReader reader;
    private boolean parsing;
    private boolean parsingSandbox;
    private String lfcHost;
    private String executableName;
    private List<URI> downloads;
    private Map<String, String> gaswVariables;
    private Map<String, String> envVariables;
    private List<GaswArgument> arguments;
    private GaswInputArg inputArg;
    private GaswOutputArg outputArg;
    private List<String> inputsList;

    public GaswParser() throws GaswException {

        this.parsing = false;
        this.parsingSandbox = false;
        this.lfcHost = "lfn://" + GaswConfiguration.getInstance().getVoLFCHost() + "/";
        this.downloads = new ArrayList<URI>();
        this.gaswVariables = new HashMap<String, String>();
        this.envVariables = new HashMap<String, String>();
        this.arguments = new ArrayList<GaswArgument>();
        this.inputsList = new ArrayList<String>();
    }

    public void parse(String descriptorFileName) throws GaswException {

        try {
            File descriptor = new File(descriptorFileName);
            logger.info("Parsing GASW descriptor: " + descriptor.getAbsolutePath());
            reader = XMLReaderFactory.createXMLReader();
            reader.setContentHandler(this);
            reader.parse(new InputSource(new FileReader(descriptor)));

        } catch (IOException ex) {
            logger.error(ex);
            throw new GaswException(ex);
        } catch (SAXException ex) {
            logger.error(ex);
            throw new GaswException(ex);
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

        if (localName.equals("description")) {
            if (parsing) {
                throw new SAXException("Nested <description> tags.");
            }
            parsing = true;

        } else if (localName.equals("executable")) {
            if (executableName != null) {
                throw new SAXException("Nested <executable> tags.");
            }
            executableName = getAttributeValue(attributes, "name", "No executable name defined.");

        } else if (localName.equals("value") && !parsingSandbox) {
            try {
                String value = getAttributeValue(attributes, "value", "No value defined.");
                if (value.contains("lfn:/")) {
                    value = lfcHost + new URI(value + ".tar.gz").getPath();
                } else {
                    value = lfcHost + value + ".tar.gz";
                }
                downloads.add(new URI(value));

            } catch (URISyntaxException ex) {
                throw new SAXException(ex);
            }

        } else if (localName.equals("nodeNumber")) {
            String value = getAttributeValue(attributes, "value", "No value defined.");
            gaswVariables.put("nodeNumber", value);
            gaswVariables.put(GaswConstants.ENV_EXECUTOR, "gLite");

        } else if (localName.equals("executor")) {
            String value = getAttributeValue(attributes, "value", "No value defined.");
            gaswVariables.put(GaswConstants.ENV_EXECUTOR, value);

        } else if (localName.equals("requirement")) {
            String value = getAttributeValue(attributes, "value", "No value defined.");

            if (value.toLowerCase().startsWith("dirac")) {
                String[] v = value.split(":");
                envVariables.put(v[0], v[1]);

            } else {
                envVariables.put("gLiteRequirement", value);
            }

        } else if (localName.equals("input")) {
            String name = getAttributeValue(attributes, "name", "No input name defined.");
            String option = getAttributeValue(attributes, "option", "No input option defined.");

            boolean implicit = false;
            if (option.startsWith("no")) {
                option = null;

            } else if (option.startsWith("invisible")) {
                option = null;
                implicit = true;
            }
            inputArg = new GaswInputArg(name, option, implicit);
            arguments.add(inputArg);
            inputsList.add(name);

        } else if (localName.equals("access")) {

            String type = getAttributeValue(attributes, "type", "No access type defined.");
            if (inputArg != null && type.equals("LFN")) {
                inputArg.setType(GaswArgument.Type.URI);
            }

        } else if (localName.equals("output")) {
            String name = getAttributeValue(attributes, "name", "No input name defined.");
            String option = attributes.getValue("option");

            boolean implicit = option.startsWith("invisible");

            if (option.startsWith("no")) {
                option = null;
            }

            outputArg = new GaswOutputArg(name, option, implicit);
            arguments.add(outputArg);

        } else if (localName.equals("template")) {
            if (outputArg == null) {
                throw new SAXException("<template> tags are just allowed inside <output> tags.");
            }
            String value = getAttributeValue(attributes, "value", "No template value defined.");
            outputArg.setTemplate(true);

            if (value.contains("$rep-")) {
                outputArg.setReplicas(new Integer(value.substring(value.lastIndexOf("-") + 1)));
                value = value.replaceAll("\\$rep-[0-9]*", "");
            }

            StringBuilder template = new StringBuilder();

            try {
                char[] param = value.toCharArray();
                for (int j = 0; j < param.length; j++) {
                    if (param[j] == '$') {
                        if (param[j + 1] == 'd') {
                            j += 4;
                            String num = "" + param[j++];
                            while (j < param.length && Character.isDigit(param[j])) {
                                num += param[j++];
                            }
                            String paramRef = inputsList.get(new Integer(num) - 1);
                            template.append("[$][");
                            template.append(paramRef);
                            template.append("]");
                        } else {
                            j += 3;
                            String num = "" + param[j++];
                            while (j < param.length && Character.isDigit(param[j])) {
                                num += param[j++];
                            }
                            String paramRef = inputsList.get(new Integer(num) - 1);
                            template.append("[%][");
                            template.append(paramRef);
                            template.append("]");
                        }
                    }
                    if (j < param.length && param[j] == '$') {
                        j--;
                    } else {
                        if (j < param.length) {
                            template.append(param[j]);
                        }
                    }
                }
                outputArg.setContent(template.toString());

            } catch (ArrayIndexOutOfBoundsException ex) {
                throw new SAXException("The index used in the output template does not exist.");
            }

        } else if (localName.equals("sandbox")) {
            parsingSandbox = true;
        }

    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {

        if (localName.equals("input")) {
            inputArg = null;

        } else if (localName.equals("output")) {
            outputArg = null;

        } else if (localName.equals("sandbox")) {
            parsingSandbox = false;
        }
    }

    private String getAttributeValue(Attributes attributes, String valueName,
            String errorMessage) throws SAXException {

        String attributeValue = attributes.getValue(valueName);
        if (attributeValue == null || attributeValue.length() == 0) {
            throw new SAXException(errorMessage);
        }
        return attributeValue;
    }

    public String getInputName(int index) {
        return inputsList.get(index);
    }

    /**
     *
     * @param inputsMap
     * @return
     */
    public GaswInput getGaswInput(Map<String, String> inputsMap)
            throws URISyntaxException {


        List<String> parameters = new ArrayList<String>();
        List<GaswUpload> uploads = new ArrayList<GaswUpload>();

        for (GaswArgument argument : arguments) {

            StringBuilder param = new StringBuilder();

            if (argument.getOption() != null) {
                param.append(argument.getOption());
                param.append(" ");
            }

            if (argument.getHookup() == GaswArgument.Hookup.Input) {
                String value = inputsMap.get(argument.getName());
                if (argument.getType() == GaswArgument.Type.URI) {
                    URI valueURI = new URI(lfcHost + value);
                    param.append(new File(valueURI.getPath()).getName());
                    downloads.add(valueURI);
                    
                } else {
                    param.append(value);
                }

            } else {
                GaswOutputArg output = (GaswOutputArg) argument;
                String value = parseTemplate(output.getContent(), inputsMap);
                URI valueURI = new URI(lfcHost + value);
                uploads.add(new GaswUpload(valueURI, output.getReplicas()));
                param.append(new File(valueURI.getPath()).getName());
            }

            if (!argument.isImplicit()) {
                parameters.add(param.toString());
            }
        }

        return new GaswInput(executableName, parameters, downloads, uploads,
                gaswVariables, envVariables);
    }

    private String parseTemplate(String template, Map<String, String> inputsMap) {


        StringBuilder content = new StringBuilder();

        char[] t = template.toCharArray();
        for (int i = 0; i < t.length; i++) {
            if (t[i] == '[') {
                boolean getDir = false;
                if (t[i + 1] == '$') {
                    getDir = true;
                }
                i += 4;
                StringBuilder inputName = new StringBuilder();
                while (t[i] != ']') {
                    inputName.append(t[i++]);
                }
                try {
                    URI u = new URI(inputsMap.get(inputName.toString()));
                    File f = new File(u.getPath());

                    content.append(getDir ? f.getParent() : f.getName());

                } catch (URISyntaxException ex) {
                    content.append(inputsMap.get(inputName.toString()));
                }

            } else {
                content.append(t[i]);
            }
        }
        return content.toString();
    }
}
