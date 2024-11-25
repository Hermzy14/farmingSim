package no.ntnu.commands;

import no.ntnu.greenhouse.GreenhouseSimulator;

/**
 * Command to request the status of an actuator from a node.
 *
 * <p>When a control panel node wants to get the latest status of an actuator from a node, it sends
 * this command to the node.
 */
public class RequestActuatorStatus extends Command {
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

    @Override
    public String execute(GreenhouseSimulator greenhouse) {
        throw new IllegalArgumentException("Not implemented"); // TODO: Implement
    }
}
