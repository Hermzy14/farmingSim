package no.ntnu.exceptions;

/**
 * Exception thrown when an unexpected error occurs.
 */
public class UnexpectedErrorException extends Exception {
  /**
   * Constructor for the UnexpectedErrorException class.
   *
   * @param message The message to be displayed when the exception is thrown.
   */
  public UnexpectedErrorException(String message) {
    super(message);
  }
}
