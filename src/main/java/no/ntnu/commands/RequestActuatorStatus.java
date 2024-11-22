package no.ntnu.commands;

/**
 * Command to request the status of an actuator from a node.
 *
 * <p>When a control panel node wants to get the latest status of an actuator from a node, it sends
 * this command to the node.
 */
public class RequestActuatorStatus implements Command {
  private final int nodeId;

  /**
   * Command to request the status of an actuator from a node.
   *
   * @param nodeId The ID of the node to request the actuator status from
   */
  public RequestActuatorStatus(int nodeId) {
    this.nodeId = nodeId;
  }

  /**
   * Get the ID of the node to request the actuator status from.
   *
   * @return The ID of the node.
   */
  public int getNodeId() {
    return nodeId;
  }
}