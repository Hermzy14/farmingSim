package no.ntnu.commands;

/**
 * Status of an acknowledgment command.
 */
public enum ACKCommandStatus {
  SUCCESS,    // Command executed successfully
  PENDING,    // Command is still being processed
  FAILED      // Command failed to execute
}

