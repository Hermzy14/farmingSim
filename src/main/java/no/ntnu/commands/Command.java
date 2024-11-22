package no.ntnu.commands;

import no.ntnu.greenhouse.GreenhouseSimulator;

/**
 * A command that can be sent to the server.
 * All commands must implement this interface.
 */
public abstract class Command {
  public String execute(GreenhouseSimulator greenhouse) {
    //TODO: Implement this method so that it can be used to execute the command on the given greenhouse
    return "Command executed on: " + greenhouse;
  }
}
