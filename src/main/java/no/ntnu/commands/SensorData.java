package no.ntnu.commands;

/**
 * Command to send sensor data to a control panel node.
 *
 * <p>When a sensor node has new sensor data, it sends this command to the control panel node.
 * The control panel node will then update the GUI with the new sensor data.
 */
public class SensorData extends Command {
  private final int nodeId;
  private final int temperature;
  private final int humidity;

  /**
   * Command to send sensor data to a control panel node.
   *
   * @param nodeId      The ID of the node that sent the sensor data
   * @param temperature The temperature reading
   * @param humidity    The humidity reading
   */
  public SensorData(int nodeId, int temperature, int humidity) {
    this.nodeId = nodeId;
    this.temperature = temperature;
    this.humidity = humidity;
  }

  /**
   * Get the ID of the node that sent the sensor data.
   *
   * @return The ID of the node.
   */
  public int getNodeId() {
    return nodeId;
  }

  /**
   * Get the temperature reading.
   *
   * @return The temperature reading.
   */
  public int getTemperature() {
    return temperature;
  }

  /**
   * Get the humidity reading.
   *
   * @return The humidity reading.
   */
  public int getHumidity() {
    return humidity;
  }
}
