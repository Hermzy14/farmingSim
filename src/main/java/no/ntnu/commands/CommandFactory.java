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

    switch (parts[0]) { //TODO: Expand to include all commands
      case "0x01":
        return new RequestSensorData(Integer.parseInt(parts[1]));
      case "0x02":
        return new RequestActuatorStatus(Integer.parseInt(parts[1]));
      case "0x03":
        return new SendActuatorCommand(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
      default:
        throw new MessageFormatException("Unknown command: " + parts[0]);
    }
  }
}