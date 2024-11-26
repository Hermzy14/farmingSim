package no.ntnu.commands;

import java.util.List;
import no.ntnu.greenhouse.GreenhouseSimulator;
import no.ntnu.greenhouse.Sensor;
import no.ntnu.greenhouse.SensorActuatorNode;
import no.ntnu.greenhouse.SensorReading;

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
    StringBuilder sb = new StringBuilder();
    try {
      SensorActuatorNode node = greenhouse.getSensorNode(nodeId);
      if (node == null) {
        return "Error: Node not found.";
      }
      sb.append("0x01 ").append(nodeId).append(" ");
      for (Sensor sensor : node.getSensors()) {
        SensorReading reading = sensor.getReading();
        sb.append(sensor.getType())
            .append(":")
            .append(reading != null ? reading.getUnit() : "unknown")
            .append(":")
            .append(reading != null ? reading.getFormatted() : "N/A")
            .append(" ");
      }
    } catch (Exception e) {
      sb.append("Error executing RequestSensorData: ").append(e.getMessage());
    }
    return sb.toString().trim(); // Ensure no trailing spaces
  }

}
