package no.ntnu.controlpanel;

import static no.ntnu.tools.Parser.parseIntegerOrError;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.Socket;
import no.ntnu.greenhouse.Actuator;
import no.ntnu.greenhouse.GreenhouseSimulator;
import no.ntnu.tools.Logger;

/**
 * A communication channel for disseminating control commands to the sensor nodes
 * (sending commands to the server) and receiving notifications about events.
 */
public class RealCommunicationChannel implements CommunicationChannel {
  private final ControlPanelLogic logic;
  private Socket socket;
  private BufferedReader reader;
  private ObjectOutputStream objectWriter;
  private static final String HOST = "localhost";

  /**
   * Create a new real communication channel.
   *
   * @param logic The application logic of the control panel node.
   */
  public RealCommunicationChannel(ControlPanelLogic logic) {
    this.logic = logic;
  }

  @Override
  public void sendActuatorChange(int nodeId, int actuatorId, boolean isOn) {
    // Send the actuator change to the server
    String state = isOn ? "on" : "off";
    Logger.info("Sending command to greenhouse: turn " + state + " actuator"
        + "[" + actuatorId + "] on node " + nodeId);
  }

  @Override
  public boolean open() {
    boolean success = false;
    try {
      this.socket = new Socket(HOST, GreenhouseSimulator.TCP_PORT);
      this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
      this.objectWriter = new ObjectOutputStream(this.socket.getOutputStream());
      Logger.info("Connection established!");
      success = true;
    } catch (IOException e) {
      Logger.error("Error on connection: " + e.getMessage());
    }
    return success;
  }

  /**
   * Close the communication channel.
   */
  public void close() {
    try {
      if (reader != null) {
        reader.close();
      }
      if (objectWriter != null) {
        objectWriter.close();
      }
      if (socket != null && !socket.isClosed()) {
        socket.close();
      }
      Logger.info("Connection closed successfully.");
    } catch (IOException e) {
      Logger.error("Error while closing the connection: " + e.getMessage());
    }
  }

  /**
   * Receive a response from the server.
   */
  public String receiveResponse() throws IOException {
    return this.reader.readLine();
  }

  /**
   * Send a command to the server.
   */
  public void sendCommand(String command) throws IOException {
    this.objectWriter.writeObject(command);
    this.objectWriter.flush();
  }
}
