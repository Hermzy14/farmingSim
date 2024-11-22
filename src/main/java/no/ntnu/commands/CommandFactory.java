package no.ntnu.commands;

import no.ntnu.exceptions.MessageFormatException;

/**
 * Factory class for parsing commands.
 */
public class CommandFactory {
  /**
   * Parse a command from a message.
   *
   * @param message The message to parse
   * @return The parsed command
   * @throws MessageFormatException If the message is not a valid command
   */
  public Command parseCommand(String message) throws MessageFormatException {
    String[] parts = message.split(" ");
    if (parts.length < 1) {
      throw new MessageFormatException("Message is empty");
    }

    switch (parts[0]) {
      case "REQUEST_ACTUATOR_STATUS":
        return new RequestActuatorStatus(Integer.parseInt(parts[1])); // Parse the node ID
      case "REQUEST_SENSOR_DATA":
        return new RequestSensorData(Integer.parseInt(parts[1])); // Parse the node ID
      case "REQUEST_COMMAND_ACK":
        return new RequestCommandAck(Integer.parseInt(parts[1])); // Parse the command ID
      case "COMMAND_ACK":
        return new CommandAck(Integer.parseInt(parts[1])); // Parse the command ID
      case "ACTUATOR_STATUS":
        boolean status = parts[2].equals("ON"); // Parse the status, which is either "ON" or "OFF"
        return new ActuatorStatus(Integer.parseInt(parts[1]),
            status); // Parse the node ID and status
      case "SENSOR_DATA":
        return new SensorData(Integer.parseInt(parts[1]),
            Integer.parseInt(parts[2]),
            Integer.parseInt(parts[3])); // Parse the node ID and sensor data
      case "SEND_ACTUATOR_COMMAND":
        return new SendActuatorCommand(Integer.parseInt(parts[1]),
            Integer.parseInt(parts[2])); // Parse the node ID and command
      default:
        throw new MessageFormatException("Unknown command: " + parts[0]);
    }
  }
}