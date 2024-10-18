/* Copyright CNRS-CREATIS
 *
 * Rafael Ferreira da Silva
 * rafael.silva@creatis.insa-lyon.fr
 * http://www.rafaelsilva.com
 *
 * This software is a grid-enabled data-driven workflow manager and editor.
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
package fr.insalyon.creatis.gasw.util;

import fr.insalyon.creatis.gasw.GaswException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Properties;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.log.NullLogChute;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;

/**
 *
 * @author Rafael Ferreira da Silva
 */
public class VelocityUtil {

    private static final Logger logger = Logger.getLogger("fr.insalyon.creatis.gasw");
    private static volatile VelocityEngine ve;
    private Template template;
    private VelocityContext context;

    public VelocityUtil(String templatePath) throws Exception {
        this(templatePath, true);
    }

    public VelocityUtil(String templatePath, boolean enableLogging) throws Exception {

        if (ve == null) {
            Properties properties = new Properties();
            properties.setProperty("resource.loader", "string");
            properties.setProperty("string.resource.loader.class", "org.apache.velocity.runtime.resource.loader.StringResourceLoader");
            properties.setProperty("string.resource.loader.repository.class", "org.apache.velocity.runtime.resource.util.StringResourceRepositoryImpl");

            if ( ! enableLogging) {
                properties.setProperty("runtime.log.logsystem.class", NullLogChute.class.getName());
            }

            ve = new VelocityEngine(properties);
            ve.init();
        }
        
        if (!ve.resourceExists(templatePath)) {
            StringResourceRepository repo = StringResourceLoader.getRepository();
            repo.putStringResource(templatePath, getTemplateFromResource(templatePath));
        }

        template = ve.getTemplate(templatePath);
        context = new VelocityContext();
    }

    /**
     * Adds data to the context.
     *
     * @param key
     * @param value
     */
    public void put(String key, Object value) {

        context.put(key, value);
    }

    /**
     * Renders the template into a StringWriter.
     *
     * @return
     * @throws GaswException
     */
    public StringWriter merge() throws GaswException {

        try {
            StringWriter writer = new StringWriter();
            template.merge(context, writer);

            return writer;

        } catch (ResourceNotFoundException ex) {
            logger.error(ex);
            throw new GaswException(ex);
        } catch (ParseErrorException ex) {
            logger.error(ex);
            throw new GaswException(ex);
        } catch (MethodInvocationException ex) {
            logger.error(ex);
            throw new GaswException(ex);
        }
    }

    private String getTemplateFromResource(final String templatePath) {

        try {
            InputStream stream = ClassLoader.getSystemResourceAsStream(templatePath);
            return IOUtils.toString(stream, "UTF-8");

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
