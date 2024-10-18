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
package fr.insalyon.creatis.gasw.script;

import fr.insalyon.creatis.gasw.util.VelocityUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Check velocity templates")
class DataManagementGeneratorTest {

    @Test
    @DisplayName("Check checkCacheDownloadAndCacheLFNFunction template")
    public void checkCheckCacheDownloadAndCacheLFNFunctionTemplate()
        throws Exception {

        VelocityUtil velocity = new VelocityUtil("vm/script/datamanagement/checkCacheDownloadAndCacheLFNFunction.vm", false);

        velocity.put("cacheDir", "dir");
        velocity.put("cacheFile", "file");

        velocity.merge().toString();
    }

    @Test
    @DisplayName("Check variables template")
    public void checkVariablesTemplateNoVariable() throws Exception {
        VelocityUtil velocity = new VelocityUtil("vm/script/execution/variables.vm", false);

        velocity.put("variables", new HashMap<>());

        String res = velocity.merge().toString();

        Assertions.assertFalse(res.contains("export"));
    }

    @Test
    @DisplayName("Check variables template")
    public void checkVariablesTemplate() throws Exception {
        VelocityUtil velocity = new VelocityUtil("vm/script/execution/variables.vm", false);

        Map<String,String> variables = new HashMap<>() {{
            put("var1", "value1");
            put("var2", "value2");
        }};
        velocity.put("variables", variables);

        String res = velocity.merge().toString();

        Assertions.assertEquals(2, res.lines().filter(l -> l.contains("export")).count());
        Assertions.assertTrue(res.contains("export var1=\"value1\""));
        Assertions.assertTrue(res.contains("export var2=\"value2\""));
    }

    @Test
    @DisplayName("Check downloadFunction template")
    public void checkDownloadFunctionTemplate() throws Exception {
        VelocityUtil velocity = new VelocityUtil("vm/script/datamanagement/downloadFunctions.vm", false);

        velocity.put("timeout", 10);
        velocity.put("minAvgDownloadThroughput", 10);
        velocity.put("bdiiTimeout", 10);
        velocity.put("srmTimeout", 10);
        velocity.put("failOverEnabled", false);
        velocity.put("failOverHost", "host");
        velocity.put("failOverPort", 10);
        velocity.put("failOverHome", "home");

        velocity.merge().toString();
    }

    @Test
    @DisplayName("Check addToCacheFunction template")
    public void checkAddToCacheFunctionTemplate() throws Exception {
        VelocityUtil velocity = new VelocityUtil("vm/script/datamanagement/addToCacheFunction.vm", false);

        velocity.put("cacheDir", "dir");
        velocity.put("cacheFile", "file");

        velocity.merge().toString();
    }

    @Test
    @DisplayName("Check addToFailOverFunction template")
    public void checkAddToFailOverFunctionTemplate() throws Exception {
        VelocityUtil velocity = new VelocityUtil("vm/script/datamanagement/addToFailOverFunction.vm", false);

        velocity.put("failOverHost", "host");
        velocity.put("failOverPort", 10);
        velocity.put("failOverHome", "home");

        velocity.merge().toString();
    }

    @Test
    @DisplayName("Check uploadFunction template")
    public void checkUploadFunctionTemplate() throws Exception {
        VelocityUtil velocity = new VelocityUtil("vm/script/datamanagement/uploadFunctions.vm", false);

        velocity.put("timeout", 10);
        velocity.put("minAvgDownloadThroughput", 10);
        velocity.put("bdiiTimeout", 10);
        velocity.put("srmTimeout", 10);
        velocity.put("failOverEnabled", false);

        velocity.merge().toString();
    }

    @Test
    @DisplayName("Check deleteFunctions template")
    public void checkDeleteFunctionsTemplate() throws Exception {
        VelocityUtil velocity = new VelocityUtil("vm/script/datamanagement/deleteFunctions.vm", false);

        velocity.merge().toString();
    }
}
