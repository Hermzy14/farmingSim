package no.ntnu.commands;

/**
 * Command to request an acknowledgment for a command.
 *
 * <p>When a control panel node wants to request an acknowledgment for a command, it sends this
 * command to the node.
 */
public class RequestCommandAck extends Command {
  private final int commandId;

  /**
   * Command to request an acknowledgment for a command.
   *
   * @param commandId The ID of the command to request an acknowledgment for
   */
  public RequestCommandAck(int commandId) {
    this.commandId = commandId;
  }

  /**
   * Get the ID of the command to request an acknowledgment for.
   *
   * @return The ID of the command.
   */
  public int getCommandId() {
    return commandId;
  }
}
