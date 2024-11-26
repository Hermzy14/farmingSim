package no.ntnu.run;

import java.io.IOException;
import java.util.Scanner;
import no.ntnu.commands.CommandFactory;
import no.ntnu.controlpanel.ControlPanelLogic;
import no.ntnu.controlpanel.RealCommunicationChannel;
import no.ntnu.tools.Logger;

/**
 * Class for starting the command line control panel.
 * <p>From here the user should be able to:</p>
 * <ul>
 *   <li>Start the control panel</li>
 *   <li>Stop the control panel</li>
 *   <li>Send commands to the greenhouse</li>
 *   <li>Display information about sensors</li>
 *   <li>Display information about actuators</li>
 * </ul>
 */
public class CommandLineControlPanel {
  private RealCommunicationChannel communicationChannel;
  private ControlPanelLogic controlPanelLogic;
  private boolean running;

  /**
   * Main method for starting the command line control panel.
   *
   * @param args Command line arguments, only the first one of them used: when it is "fake",
   *             emulate fake events, when it is either something else or not present,
   *             use real socket communication.
   */
  public static void main(String[] args) {
    CommandLineControlPanel controlPanel = new CommandLineControlPanel();
    if (controlPanel.init()) { // Initialize the control panel
      Logger.success("Control panel initialized successfully!");
      controlPanel.run(); // Run the control panel if initialization was successful
    }
  }

  /**
   * Initialize the control panel.
   */
  public boolean init() {
    this.controlPanelLogic = new ControlPanelLogic();
    this.communicationChannel = new RealCommunicationChannel(controlPanelLogic);
    return this.communicationChannel.open();
  }

  /**
   * Run the control panel.
   */
  public void run() {
    Logger.info("Running the control panel...");
    Logger.info("\nList of available commands:");
    printCommands();

    this.running = true;
    Scanner scanner = new Scanner(System.in);
    while (this.running) {
      Logger.info("\nEnter a command: ");
      String command = scanner.nextLine().toLowerCase();
      sendReceive(command);
    }
  }

  private void printCommands() {
    // Print the available commands
    // %-15s means a string with a width of 15 characters, left-aligned
    // %-40s means a string with a width of 40 characters, left-aligned
    System.out.printf(
        "--------------------------------------------------------------------------------%n");
    System.out.printf("| %-15s | %-40s | %-15s |%n", "COMMAND", "DESCRIPTION", "EXAMPLE USE");
    System.out.printf(
        "--------------------------------------------------------------------------------%n");

    System.out.printf("| %-15s | %-40s | %-15s |%n",
        "0x01 [nodeId]", "Request sensor data from a node", "0x01 1");
    //TODO: Add more commands here
    System.out.printf("| %-15s | %-40s | %-15s |%n",
        "help", "Prints the available commands", "help");
    System.out.printf("| %-15s | %-40s | %-15s |%n", "exit", "Exits the control panel", "exit");

    System.out.printf(
        "--------------------------------------------------------------------------------%n");
  }

  private void sendReceive(String command) {
    if (command.equals("help")) {
      printCommands();
    } else if (command.equals("exit")) {
      this.running = false;
      try {
        this.communicationChannel.close();
      } catch (Exception e) {
        Logger.error("Error on closing the communication channel: " + e.getMessage());
      }
    } else {
      try {
        this.communicationChannel.sendCommand(command);
        String response = this.communicationChannel.receiveResponse();
        if (response != null) {
          Logger.info("Response: " + response);
        }
      } catch (IOException e) {
        Logger.error("Error on sending/receiving command: " + e.getMessage());
      }
    }
  }
}
