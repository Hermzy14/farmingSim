package no.ntnu.exceptions;

/**
 * Exception thrown when a message is not formatted as expected.
 */
public class MessageFormatException extends Exception {
  /**
   * Constructor for the MessageFormatException class.
   *
   * @param message The message to be displayed when the exception is thrown.
   */
  public MessageFormatException(String message) {
    super(message);
  }
}
