package fr.insalyon.creatis.gasw.output;

/**
 *
 * @author tram
 */
public enum GaswExitCode {

    SUCCESS (0),           // successfully executed
    ERROR_READ_GRID (1),   // error during download file from grid using lcg-cp
    ERROR_WRITE_GRID (2),  // error during write file to grid using lcg-cr
    ERROR_WRITE_LOCAL (7), // error during create directory and write file locally, used in PBS
    ERROR_GET_STD (8),     // error during download stderr/out of the application from grid
    EXECUTION_FAILED (6),  // execution failed
    EXECUTION_CANCELED (9),// execution canceled
    EXECUTION_STALLED (10),// execution stalled used in DIRAC execution 
    UNDEFINED (-1);        // default exit code 
    
    private int exitCode;

    private GaswExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    public int getExitCode() {
        return this.exitCode;
    }
}
