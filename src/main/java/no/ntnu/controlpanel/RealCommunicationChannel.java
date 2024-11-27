package no.ntnu.controlpanel;

import static no.ntnu.tools.Parser.parseIntegerOrError;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.zip.CRC32;
import java.util.LinkedList;
import java.util.List;
import no.ntnu.greenhouse.GreenhouseSimulator;
import no.ntnu.greenhouse.SensorReading;
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
  private Thread communicationThread;
  private boolean running;

  /**
   * Create a new real communication channel.
   *
   * @param logic The application logic of the control panel node.
   */
  public RealCommunicationChannel(ControlPanelLogic logic) {
    this.logic = logic;
  }

  public class ChecksumUtil {
    // Calculates a CRC32 checksum for the given data
    public static long calculateChecksum(String data) {
      CRC32 crc32 = new CRC32();
      crc32.update(data.getBytes()); //Add the data to the checksum calculator
      return crc32.getValue();
    }
    // Validate the checksum of the given data
    public static boolean validateChecksum(String data, long receivedChecksum) {
      return calculateChecksum(data) == receivedChecksum;
    }
  }

  @Override
  public void sendActuatorChange(int nodeId, int actuatorId, boolean isOn) {
    // Send the actuator change to the server
    String state = isOn ? "on" : "off";
    Logger.info("Sending command to greenhouse: turn " + state + " actuator"
        + "[" + actuatorId + "] on node " + nodeId);

    // Create the command packet
    String command = nodeId + " " + actuatorId + " " + (isOn ? "1" : "0");

    // Calculate the checksum to the command
      long checksum = ChecksumUtil.calculateChecksum(command);
      String packet = command + ";" + checksum;

    try {
      sendCommand(packet);
    } catch (IOException e) {
      Logger.error("Failed to send command with checksum: " + e.getMessage());
    }
  }

  @Override
  public boolean open() {
    int attempt = 1; // Current connection attempt
    int maxAttempts = 5; // Maximum number of connection attempts
    int delayBetweenAttempts = 2000; // Delay between connection attempts in milliseconds
    boolean success = false;

    while ((attempt <= maxAttempts) && !success) {
      try {
        this.socket = new Socket(HOST, GreenhouseSimulator.TCP_PORT);
        this.objectWriter = new ObjectOutputStream(this.socket.getOutputStream());
        this.objectWriter.flush();
        this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
        success = true;
      } catch (IOException e) {
        Logger.error("Connection attempt " + attempt + " failed: " + e.getMessage());

        try {
          Thread.sleep(delayBetweenAttempts);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          break;
        }
      }
      attempt++;
    }

    // Log an error if the connection was not established
    if (!success) {
      Logger.error("Failed to establish connection after " + maxAttempts + " attempts");
    }

    return success;
  }

  /**
   * Close the communication channel.
   */
  public void close() {
    try {
      // Send a shutdown signal if needed
      if (objectWriter != null) {
        objectWriter.writeObject("SHUTDOWN");
        objectWriter.flush();
      }
    } catch (IOException e) {
      Logger.error("Error sending shutdown signal: " + e.getMessage());
    } finally {
      try {
        if (socket != null && !socket.isClosed()) {
          socket.close();
        }
      } catch (IOException e) {
        Logger.error("Error closing socket: " + e.getMessage());
      }
    }
  }

  /**
   * Start continuous communication with the server.
   * This method will periodically send requests for sensor data to the server.
   */
  public void startContinuousCommunication() {
    this.running = true;
    this.communicationThread = new Thread(() -> {
      while (this.running) {
        try {
          // Periodically send requests for sensor data
          for (int nodeId : new int[] {1, 2, 3}) {
            sendCommand("0x01 " + nodeId);
            String response = receiveResponse();
            Logger.info("Continuous communication response: " + response);
          }

          // Wait a bit between cycles
          Thread.sleep(5000); // 5 seconds between cycles
        } catch (IOException | InterruptedException e) {
          Logger.error("Error in continuous communication: " + e.getMessage());
          this.running = false;
        }
      }
    });
    communicationThread.setDaemon(true);
    communicationThread.start();
  }

  /**
   * Stop the continuous communication with the server.
   */
  public void stopContinuousCommunication() {
    this.running = false;
    if (this.communicationThread != null) {
      this.communicationThread.interrupt();
    }
  }

  /**
   * Receive a response from the server.
   */
  public String receiveResponse() throws IOException {
    // Read a packet from the communication channel
    String packet = this.reader.readLine();
   if (packet == null || packet.contains(",")) {
     Logger.error("Invalid packet received: " + packet);
        return null;
   }
   try {
     String parts[] = packet.split(";");
     String command = parts[0];
     long receivedChecksum = Long.parseLong(parts[1]);

     return  ChecksumUtil.validateChecksum(command, receivedChecksum) ? command : null;
    } catch (Exception e) {
        Logger.error("Error parsing packet: " + e.getMessage());
        return null;
   }
  }

  /**
   * Send a command to the server.
   */
  public void sendCommand(String command) throws IOException {
    try {
      if (this.objectWriter == null) {
        Logger.error("Object writer is null, cannot send command");
        return;
      }
      this.objectWriter.writeObject(command);
      this.objectWriter.flush();
      Logger.info("Sent command: " + command);
    } catch (IOException e) {
      Logger.error("Error sending command: " + e.getMessage());
    }
  }
}
