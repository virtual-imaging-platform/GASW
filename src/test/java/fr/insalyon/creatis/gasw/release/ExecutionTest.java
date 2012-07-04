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
import fr.insalyon.creatis.gasw.release.Execution.JobType;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;
import org.junit.*;

/**
 *
 * @author Rafael Silva
 */
public class ExecutionTest {

    private EnvVariable boundedVariable1;
    private EnvVariable boundedVariable2;
    private EnvVariable boundedVariable3;
    private String normalTarget;
    private String mpiLamTarget;
    private String mpiMpichTarget;
    private String mpiMpich2Target;
    private URI normalBounded;
    private URI mpiLamBounded;
    private URI mpiMpichBounded;
    private URI mpiMpich2Bounded;
    private Execution normalExecution;
    private Execution mpiLamExecution;
    private Execution mpiMpichExecution;
    private Execution mpiMpich2Execution;

    public ExecutionTest() {

        boundedVariable1 = new EnvVariable(Category.INFRASTRUCTURE.name(), "name1", "value1");
        boundedVariable2 = new EnvVariable(Category.INFRASTRUCTURE.name(), "name2", "value2");
        boundedVariable3 = new EnvVariable(Category.INFRASTRUCTURE.name(), "name2", "value3");

        List<EnvVariable> variablesList = new ArrayList<EnvVariable>();
        variablesList.add(boundedVariable1);
        variablesList.add(boundedVariable2);
        variablesList.add(boundedVariable3);

        normalTarget = "normal.sh";
        mpiLamTarget = "mpiLam.sh";
        mpiMpichTarget = "mpiMpich.sh";
        mpiMpich2Target = "mpiMpich2.sh";

        normalBounded = URI.create("");

        normalExecution = new Execution(JobType.NORMAL.name(), normalTarget, variablesList, normalBounded);
        mpiLamExecution = new Execution(JobType.MPI_LAM.name(), mpiLamTarget, variablesList, mpiLamBounded);
        mpiMpichExecution = new Execution(JobType.MPI_MPICH.name(), mpiMpichTarget, new ArrayList<EnvVariable>(), mpiMpichBounded);
        mpiMpich2Execution = new Execution(JobType.MPI_MPICH2.name(), mpiMpich2Target, null, mpiMpich2Bounded);
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
     * Test of getType method, of class Execution.
     */
    @Test
    public void testGetType() {

        assertEquals(JobType.NORMAL, normalExecution.getType());
        assertEquals(JobType.MPI_LAM, mpiLamExecution.getType());
        assertEquals(JobType.MPI_MPICH, mpiMpichExecution.getType());
        assertEquals(JobType.MPI_MPICH2, mpiMpich2Execution.getType());
    }

    /**
     * Test of getBoundArtifact method, of class Execution.
     */
    @Test
    public void testGetBoundArtifact() {

        assertEquals(normalBounded, normalExecution.getBoundArtifact());
        assertEquals(mpiLamBounded, mpiLamExecution.getBoundArtifact());
        assertEquals(mpiMpichBounded, mpiMpichExecution.getBoundArtifact());
        assertEquals(mpiMpich2Bounded, mpiMpich2Execution.getBoundArtifact());
    }

    /**
     * Test of getBoundEnvironment method, of class Execution.
     */
    @Test
    public void testGetBoundEnvironment() {

        assertEquals(true, normalExecution.getBoundEnvironment().contains(boundedVariable1));
        assertEquals(true, normalExecution.getBoundEnvironment().contains(boundedVariable2));
        assertEquals(true, normalExecution.getBoundEnvironment().contains(boundedVariable3));

        assertEquals(true, mpiLamExecution.getBoundEnvironment().contains(boundedVariable1));
        assertEquals(true, mpiLamExecution.getBoundEnvironment().contains(boundedVariable1));
        assertEquals(true, mpiLamExecution.getBoundEnvironment().contains(boundedVariable1));

        assertEquals(0, mpiMpichExecution.getBoundEnvironment().size());

        try {
            mpiMpich2Execution.getBoundEnvironment().size();
            fail("Must call null pointer exception.");

        } catch (NullPointerException ex) {
            // expected result
        }
    }

    /**
     * Test of getTarget method, of class Execution.
     */
    @Test
    public void testGetTarget() {

        assertEquals(normalTarget, normalExecution.getTarget());
        assertEquals(mpiLamTarget, mpiLamExecution.getTarget());
        assertEquals(mpiMpichTarget, mpiMpichExecution.getTarget());
        assertEquals(mpiMpich2Target, mpiMpich2Execution.getTarget());
    }

    /**
     * Test of hasBoundEnvironment method, of class Execution.
     */
    @Test
    public void testHasBoundEnvironment() {

        assertEquals(true, normalExecution.hasBoundEnvironment());
        assertEquals(false, mpiMpichExecution.hasBoundEnvironment());
        assertEquals(false, mpiMpich2Execution.hasBoundEnvironment());
    }
}
