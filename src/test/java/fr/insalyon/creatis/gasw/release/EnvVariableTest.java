/* Copyright CNRS-CREATIS
 *
 * Rafael Silva
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
package fr.insalyon.creatis.gasw.release;

import fr.insalyon.creatis.gasw.release.EnvVariable.Category;
import static org.junit.Assert.assertEquals;
import org.junit.*;

/**
 *
 * @author Rafael Silva
 */
public class EnvVariableTest {

    private EnvVariable systemVariable;
    private EnvVariable infrastructureVariable;
    private String variableName;
    private String variableValue;

    public EnvVariableTest() {

        variableName = "VariableName";
        variableValue = "VariableValue";

        systemVariable = new EnvVariable(Category.SYSTEM.name(),
                variableName, variableValue);
        infrastructureVariable = new EnvVariable(Category.INFRASTRUCTURE.name(),
                variableName, variableValue);
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getName method, of class EnvVariable.
     */
    @Test
    public void testGetName() {

        assertEquals(variableName, systemVariable.getName());
        assertEquals(variableName, infrastructureVariable.getName());
    }

    /**
     * Test of getValue method, of class EnvVariable.
     */
    @Test
    public void testGetValue() {

        assertEquals(variableValue, systemVariable.getValue());
        assertEquals(variableValue, infrastructureVariable.getValue());
    }

    /**
     * Test of getCategory method, of class EnvVariable.
     */
    @Test
    public void testGetCategory() {
        
        assertEquals(Category.SYSTEM, systemVariable.getCategory());
        assertEquals(Category.INFRASTRUCTURE, infrastructureVariable.getCategory());
    }
}
