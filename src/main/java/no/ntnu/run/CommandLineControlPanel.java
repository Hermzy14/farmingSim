package no.ntnu.run;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import no.ntnu.controlpanel.RealCommunicationChannel;
import no.ntnu.tools.Logger;

/**
 * Class for starting the command line control panel.
 *
 * <p>From here the user should be able to:</p>
 * <ul>
 *   <li>Start the control panel</li>
 *   <li>Stop the control panel</li>
 *   <li>Send commands to the greenhouse</li>
 *   <li>Display information about sensors</li>
 *   <li>Display information about actuators</li>
 *   <li>Turn an actuator on or off</li>
 *   <li>Turn heartbeat on or off</li>
 *   <li>Display the available commands</li>
 *   <li>Exit the control panel</li>
 * </ul>
 */
public class CommandLineControlPanel {
  private RealCommunicationChannel communicationChannel;
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
   *
   * @return {@code true} if the control panel was initialized successfully,
   * {@code false} otherwise.
   */
  public boolean init() {
    this.communicationChannel = new RealCommunicationChannel();
    return this.communicationChannel.open();
  }

  /**
   * Run the control panel.
   */
  public void run() {
    // Start heartbeat
    Logger.info("Starting heartbeat...");
    Logger.info("This will send a request sensor data command to the server every minute.");
    this.communicationChannel.startHeartbeat();

    Logger.info("Running the control panel...");
    Logger.info("\nAvailable commands:");
    printCommands();

    // Start the control panel
    this.running = true;
    Scanner scanner = new Scanner(System.in);
    while (this.running) {
      // Add a small delay before the next command so the user can read the output without
      // "Enter a command:" being printed immediately
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        Logger.error("Error in control panel: " + e.getMessage());
      }
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
        "-----------------------------------------------------------------------------------------"
            + "------%n");
    System.out.printf("| %-30s | %-40s | %-15s |%n", "COMMAND", "DESCRIPTION", "EXAMPLE USE");
    System.out.printf(
        "-----------------------------------------------------------------------------------------"
            + "------%n");

    System.out.printf("| %-30s | %-40s | %-15s |%n",
        "0x01 [nodeId]", "Request sensor data from a node", "0x01 1");
    System.out.printf("| %-30s | %-40s | %-15s |%n",
        "0x02 [nodeId]", "Request actuator data from a node", "0x02 1");
    System.out.printf("| %-30s | %-40s | %-15s |%n",
        "0x03 [nodeId] [actuatorId]", "Turn an actuator on a node on or off", "0x03 1 2");
    System.out.printf("| %-30s | %-40s | %-15s |%n",
        "list", "Lists all sensor/actuator nodes", "list");
    System.out.printf("| %-30s | %-40s | %-15s |%n", "toggle", "Toggles the heartbeat", "toggle");
    System.out.printf("| %-30s | %-40s | %-15s |%n",
        "help", "Prints the available commands", "help");
    System.out.printf("| %-30s | %-40s | %-15s |%n", "exit", "Exits the control panel", "exit");

    System.out.printf(
        "-----------------------------------------------------------------------------------------"
            + "------%n");
  }

  private void sendReceive(String command) {
    if (command.equals("help")) {
      printCommands();
    } else if (command.equals("toggle")) {
      handleToggleHeartbeat();
    } else if (command.equals("exit")) {
      handleExitCommand();
    } else {
      handleCommunicationCommand(command);
    }
  }

  private void handleCommunicationCommand(String command) {
    try {
      this.communicationChannel.sendCommand(command);
      String response = this.communicationChannel.receiveResponse();
      if (response != null) {
        Logger.info("Response: " + response);
      }
    } catch (IOException e) {
      Logger.error("Error on sending/receiving command: " + e.getMessage());
      Logger.info("Trying to reconnect...");
      if (!this.communicationChannel.open()) {
        Logger.error("Reconnection failed, stopping the control panel");
        this.running = false;
      }
    } catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException
             | BadPaddingException | InvalidKeyException e) {
      Logger.error("Error on decrypting command: " + e.getMessage());
      this.running = false;
    }
  }

  private void handleExitCommand() {
    this.running = false;
    try {
      this.communicationChannel.close();
    } catch (Exception e) {
      Logger.error("Error on closing the communication channel: " + e.getMessage());
    }
  }

  private void handleToggleHeartbeat() {
    if (this.communicationChannel.toggleHeartbeat()) {
      Logger.info("Heartbeat toggled on.");
    } else {
      Logger.info("Heartbeat toggled off.");
    }
  }
}
