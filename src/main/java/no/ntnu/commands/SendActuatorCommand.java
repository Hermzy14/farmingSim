package no.ntnu.commands;

/**
 * Command to send an actuator command to a node. This could be to turn on a light, open a window, etc.
 *
 * <p>When a control panel node wants to send an actuator command to a node, it sends this command to
 * the node.
 */
public class SendActuatorCommand extends Command {
  private final int nodeId;
  private final int command;

  /**
   * Command to send an actuator command to a node.
   *
   * @param nodeId  The ID of the node to send the actuator command to
   * @param command The actuator command to send
   */
  public SendActuatorCommand(int nodeId, int command) {
    this.nodeId = nodeId;
    this.command = command;
  }

  /**
   * Get the ID of the node to send the actuator command to.
   *
   * @return The ID of the node.
   */
  public int getNodeId() {
    return this.nodeId;
  }

  /**
   * Get the actuator command to send.
   *
   * @return The actuator command.
   */
  public int getCommand() {
    return this.command;
  }
}
