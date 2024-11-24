package no.ntnu.run;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import no.ntnu.commands.Command;
import no.ntnu.commands.CommandAck;
import no.ntnu.commands.RequestSensorData;
import no.ntnu.controlpanel.CommunicationChannel;
import no.ntnu.controlpanel.ControlPanelLogic;
import no.ntnu.controlpanel.FakeCommunicationChannel;
import no.ntnu.controlpanel.RealCommunicationChannel;
import no.ntnu.greenhouse.GreenhouseSimulator;
import no.ntnu.gui.controlpanel.ControlPanelApplication;
import no.ntnu.tools.Logger;

import java.io.PrintWriter;
import java.net.Socket;

/**
 * Starter class for the control panel.
 * Note: we could launch the Application class directly, but then we would have issues with the
 * debugger (JavaFX modules not found)
 */
public class ControlPanelStarter {
  private final boolean fake;
  private static final String HOST = "localhost";
  private Socket socket;
  private BufferedReader reader;
  private ObjectOutputStream objectWriter;

  public ControlPanelStarter(boolean fake) {
    this.fake = fake;
  }

  /**
   * Entrypoint for the application.
   *
   * @param args Command line arguments, only the first one of them used: when it is "fake",
   *             emulate fake events, when it is either something else or not present,
   *             use real socket communication. Go to Run → Edit Configurations.
   *             Add "fake" to the Program Arguments field.
   *             Apply the changes.
   */
  public static void main(String[] args) {
    boolean fake = false;// make it true to test in fake mode
    if (args.length == 1 && "fake".equals(args[0])) {
      fake = true;
      Logger.info("Using FAKE events");
    }
    ControlPanelStarter starter = new ControlPanelStarter(fake);
    starter.start();
  }

  private void start() {
    ControlPanelLogic logic = new ControlPanelLogic();
    CommunicationChannel channel = initiateCommunication(logic, fake);
    ControlPanelApplication.startApp(logic, channel);
    // This code is reached only after the GUI-window is closed
    Logger.info("Exiting the control panel application");
    stopCommunication();
  }

  private CommunicationChannel initiateCommunication(ControlPanelLogic logic, boolean fake) {
    CommunicationChannel channel;
    if (fake) {
      channel = initiateFakeSpawner(logic);
    } else {
      channel = initiateSocketCommunication(logic);
    }
    return channel;
  }

  private CommunicationChannel initiateSocketCommunication(ControlPanelLogic logic) {
    // You communication class(es) may want to get reference to the logic and call necessary
    // logic methods when events happen (for example, when sensor data is received)
    CommunicationChannel channel = null;
    try {
      this.socket = new Socket(HOST, GreenhouseSimulator.TCP_PORT);
      this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
      this.objectWriter = new ObjectOutputStream(this.socket.getOutputStream());
      System.out.println("Connection established!");
      channel = new RealCommunicationChannel(logic);
      sendAndReceive("0x01 1"); //TODO - remove after testing, should be done by the GUI
    } catch (IOException e) {
      System.err.println("Error on connection: " + e.getMessage());
    }
    return channel;
  }

  /**
   * Sends and receives command.
   *
   * @param command command received.
   */
  public void sendAndReceive(String command) {
    if (sendToServer(command)) {
      String response = receiveResponse();
      if (response != null) {
        System.out.println("Client's response: " + response);
      }
    }
  }

  private boolean sendToServer(String command) {
    boolean success = false;
    try {
      this.objectWriter.writeObject(command);
      success = true;
    } catch (Exception e) {
      Logger.error("Error while sending the message: " + e.getMessage());
    }
    return success;
  }

  private String receiveResponse() {
    String response = null;
    try {
      response = this.reader.readLine();
    } catch (IOException e) {
      Logger.error("Error while receiving data from the server: " + e.getMessage());
    }
    return response;
  }

  private CommunicationChannel initiateFakeSpawner(ControlPanelLogic logic) {
    // Here we pretend that some events will be received with a given delay
    FakeCommunicationChannel spawner = new FakeCommunicationChannel(logic);
    logic.setCommunicationChannel(spawner);
    final int START_DELAY = 5;
    spawner.spawnNode("4;3_window", START_DELAY);
    spawner.spawnNode("1", START_DELAY + 1);
    spawner.spawnNode("1", START_DELAY + 2);
    spawner.advertiseSensorData("4;temperature=27.4 °C,temperature=26.8 °C,humidity=80 %",
        START_DELAY + 2);
    spawner.spawnNode("8;2_heater", START_DELAY + 3);
    spawner.advertiseActuatorState(4, 1, true, START_DELAY + 3);
    spawner.advertiseActuatorState(4, 1, false, START_DELAY + 4);
    spawner.advertiseActuatorState(4, 1, true, START_DELAY + 5);
    spawner.advertiseActuatorState(4, 2, true, START_DELAY + 5);
    spawner.advertiseActuatorState(4, 1, false, START_DELAY + 6);
    spawner.advertiseActuatorState(4, 2, false, START_DELAY + 6);
    spawner.advertiseActuatorState(4, 1, true, START_DELAY + 7);
    spawner.advertiseActuatorState(4, 2, true, START_DELAY + 8);
    spawner.advertiseSensorData("4;temperature=22.4 °C,temperature=26.0 °C,humidity=81 %",
        START_DELAY + 9);
    spawner.advertiseSensorData("1;humidity=80 %,humidity=82 %", START_DELAY + 10);
    spawner.advertiseRemovedNode(8, START_DELAY + 11);
    spawner.advertiseRemovedNode(8, START_DELAY + 12);
    spawner.advertiseSensorData("1;temperature=25.4 °C,temperature=27.0 °C,humidity=67 %",
        START_DELAY + 13);
    spawner.advertiseSensorData("4;temperature=25.4 °C,temperature=27.0 °C,humidity=82 %",
        START_DELAY + 14);
    spawner.advertiseSensorData("4;temperature=25.4 °C,temperature=27.0 °C,humidity=82 %",
        START_DELAY + 16);
    return spawner;
  }

  private void stopCommunication() {
    // TODO - here you stop the TCP/UDP socket communication
  }
}
