package no.ntnu.commands;

import java.util.List;
import no.ntnu.greenhouse.GreenhouseSimulator;
import no.ntnu.greenhouse.Sensor;
import no.ntnu.greenhouse.SensorActuatorNode;

/**
 * Command to request sensor data from a node.
 *
 * <p>When a control panel node wants to get the latest sensor data from a sensor node, it sends
 * this command to the sensor node.
 */
public class RequestSensorData extends Command {
  private final int nodeId;

  /**
   * Command to request sensor data from a node.
   *
   * @param nodeId The ID of the node to request sensor data from
   */
  public RequestSensorData(int nodeId) {
    this.nodeId = nodeId;
  }

  /**
   * Get the ID of the node to request sensor data from.
   *
   * @return The ID of the node.
   */
  public int getNodeId() {
    return nodeId;
  }

  @Override
  public String execute(GreenhouseSimulator greenhouse) {
    //throw new IllegalArgumentException("Not implemented"); // TODO: Implement
    StringBuilder sb;
    try {
      SensorActuatorNode node = greenhouse.getSensorNode(nodeId);
      if (node == null) {
        return "Node not found";
      }
      List<Sensor> sensors = node.getSensors();
      sb = new StringBuilder();
      for (Sensor sensor : sensors) {
        sb.append(sensor.getReading().getFormatted());
        sb.append(", ");
      }
    } catch (Exception e) {
      sb = new StringBuilder();
      sb.append("Error executing RequestSensorData: ");
      sb.append(e.getMessage());
    }
    return sb.toString();
  }
}
