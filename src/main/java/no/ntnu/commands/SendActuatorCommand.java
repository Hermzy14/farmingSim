package no.ntnu.commands;

import no.ntnu.greenhouse.Actuator;
import no.ntnu.greenhouse.GreenhouseSimulator;
import no.ntnu.greenhouse.SensorActuatorNode;

/**
 * Command to send an actuator command to a node. This could be to turn on a light, open a window, etc.
 *
 * <p>When a control panel node wants to send an actuator command to a node, it sends this command to
 * the node.
 */
public class SendActuatorCommand extends Command {
  private final int nodeId;
  private final int actuatorId;

  /**
   * Command to send an actuator command to a node.
   *
   * @param nodeId     The ID of the node to send the actuator command to
   * @param actuatorId The ID of the actuator to send the command to
   */
  public SendActuatorCommand(int nodeId, int actuatorId) {
    this.nodeId = nodeId;
    this.actuatorId = actuatorId;
  }

  /**
   * Get the ID of the node to send the actuator command to.
   *
   * @return The ID of the node.
   */
  public int getNodeId() {
    return this.nodeId;
  }

  @Override
  public String execute(GreenhouseSimulator greenhouse) {
    SensorActuatorNode node = greenhouse.getSensorNode(nodeId);
    if (node == null) {
      return "Error: Node not found.";
    }
    if (nodeId != 1 && nodeId != 2 && nodeId != 3){
      return "Invalid MAC authentication.";
    }
    try {
      // Toggle the actuator
      node.toggleActuator(this.actuatorId);
      // Get the actuator and return a message
      Actuator actuator = node.getActuators().get(this.actuatorId);
      // Return a message indicating the actuator state
      return "Actuator " + this.actuatorId + " on node " + this.nodeId + " is now " +
          (actuator.isOn() ? "ON" : "off");
    } catch (IllegalArgumentException e) {
      return "Error: " + e.getMessage();
    }
  }
}
