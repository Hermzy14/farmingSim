package no.ntnu.commands;

/**
 * Command to acknowledge a command. This is sent from a node to the control panel node to acknowledge
 * that a command has been received and processed.
 */
public class CommandAck implements Command {
  private final int commandId;

  /**
   * Command to acknowledge a command.
   *
   * @param commandId The ID of the command to acknowledge
   */
  public CommandAck(int commandId) {
    this.commandId = commandId;
  }

  /**
   * Get the ID of the command to acknowledge.
   *
   * @return The ID of the command.
   */
  public int getCommandId() {
    return commandId;
  }
}
