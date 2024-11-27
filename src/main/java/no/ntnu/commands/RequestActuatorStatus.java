package no.ntnu.commands;

import no.ntnu.greenhouse.Actuator;
import no.ntnu.greenhouse.ActuatorCollection;
import no.ntnu.greenhouse.GreenhouseSimulator;
import no.ntnu.greenhouse.Sensor;
import no.ntnu.greenhouse.SensorActuatorNode;

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
    StringBuilder sb = new StringBuilder();
    try {
      SensorActuatorNode node = greenhouse.getSensorNode(nodeId);
      if (node == null) {
        return "Error: Node not found.";
      }
      if (nodeId != 1 && nodeId != 2 && nodeId != 3){
        return "Invalid MAC authentication.";
      }
      ActuatorCollection actuators = node.getActuators();
      sb.append("Actuator status from node ").append(nodeId).append(": ");
      if (actuators.size() == 0) {
        sb.append("No actuators found for node ").append(nodeId).append(".");
      } else {
        for (Actuator actuator : actuators) {
          sb.append("Actuator ")
              .append(actuator.getId())
              .append(": ")
              .append(actuator.isOn() ? "on" : "off")
              .append(", ");
        }
      }
    } catch (Exception e) {
      sb.append("Error executing RequestActuatorStatus: ").append(e.getMessage());
    }
    return sb.toString().trim();
  }
}
