package no.ntnu.commands;

import no.ntnu.exceptions.MessageFormatException;
import no.ntnu.greenhouse.GreenhouseSimulator;

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
    throw new IllegalArgumentException("Not implemented"); // TODO: Implement
  }
}
