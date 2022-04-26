package cz.muni.fi.pv168.project.data;

/**
 * Class for exception that is thrown when data access fails.
 *
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
