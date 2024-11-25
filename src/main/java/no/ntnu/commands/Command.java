package no.ntnu.commands;

import no.ntnu.greenhouse.GreenhouseSimulator;

/**
 * A command that can be sent to the server.
 * All commands must implement this interface.
 */
public abstract class Command {
  /**
   * Execute the command.
   *
   * @param greenhouse The greenhouse simulator to execute the command on.
   * @return A string response to the command.
   */
  public abstract String execute(GreenhouseSimulator greenhouse);
}
