package no.ntnu.commands;

/**
 * Command to send the status of an actuator from a node.
 *
 * <p>When a node has new actuator status, it sends this command to the control panel node.
 * The control panel node will then update the GUI with the new actuator status.
 */
public class ActuatorStatus extends Command {
  private final int nodeId;
  private final boolean status;

  /**
   * Command to send the status of an actuator from a node.
   *
   * @param nodeId The ID of the node that sent the actuator status
   * @param status The status of the actuator. Either {@code true} (on) or {@code false} (off).
   */
  public ActuatorStatus(int nodeId, boolean status) {
    this.nodeId = nodeId;
    this.status = status;
  }

  /**
   * Get the ID of the node that sent the actuator status.
   *
   * @return The ID of the node.
   */
  public int getNodeId() {
    return nodeId;
  }

  /**
   * Get the status of the actuator.
   *
   * @return The status of the actuator.
   */
  public boolean getStatus() {
    return status;
  }
}
