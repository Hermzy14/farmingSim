package no.ntnu.exceptions;

/**
 * Exception thrown when there is a connection error.
 */
public class ConnectionErrorException extends Exception {
  /**
   * Constructor for the ConnectionErrorException class.
   *
   * @param message The message to be displayed when the exception is thrown.
   */
  public ConnectionErrorException(String message) {
    super(message);
  }
}
