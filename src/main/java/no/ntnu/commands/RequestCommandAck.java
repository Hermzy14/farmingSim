package no.ntnu.commands;

import no.ntnu.greenhouse.GreenhouseSimulator;

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

  @Override
  public String execute(GreenhouseSimulator greenhouse) {
    // 1. Check if the command ID is valid
    ACKCommandStatus status = greenhouse.getCommandStatus(commandId);
    if (status == null) {
      return "ERROR: Invalid command ID";
    }

    // 2. Return the acknowledgment based on the command's status
    switch (status) {
      case SUCCESS:
        return "ACK: Command executed successfully";
      case PENDING:
        return "ACK: Command execution is pending";
      case FAILED:
        return "ACK: Command execution failed";
      default:
        return "ACK: Unknown status";
    }
  }
}
