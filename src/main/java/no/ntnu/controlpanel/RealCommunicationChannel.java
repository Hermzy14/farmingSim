package no.ntnu.controlpanel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.zip.CRC32;
import java.util.LinkedList;
import java.util.List;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import no.ntnu.greenhouse.GreenhouseSimulator;
import no.ntnu.tools.EncryptionDecryption;
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
  private SecretKey sharedSecret;

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
    String state = isOn ? "on" : "off";
    Logger.info("Sending command to greenhouse: turn " + state + " actuator"
        + "[" + actuatorId + "] on node " + nodeId);

    // Create the command packet
    String command = nodeId + " " + actuatorId + " " + (isOn ? "1" : "0");

    // Calculate the checksum to the command
      long checksum = ChecksumUtil.calculateChecksum(command);
      String packet = command + ";" + checksum;

    sendCommand(packet);
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

        exchangeKeys();

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
  public void startHeartbeat() {
    this.running = true;
    this.communicationThread = new Thread(() -> {
      while (this.running) {
        try {
          // Periodically send requests for sensor data
          for (int nodeId : new int[] {1, 2, 3}) {
            sendCommand("0x01 " + nodeId);
            String response = receiveResponse();
            Logger.info("Heartbeat response: " + response + "\n");
          }
          Thread.sleep(60000); // 1 minute between cycles
        } catch (IOException e) {
          Logger.error("Error in heartbeat: " + e.getMessage());
          this.running = false;
        } catch (InterruptedException e) {
          this.running = false;
        }
      }
    });
    communicationThread.setDaemon(true);
    communicationThread.start();
  }

  /**
   * Toggles the heartbeat on and off.
   * If the heartbeat is on, it will be turned off, and vice versa.
   *
   * @return {@code true} if the heartbeat is now on, {@code false} if it is off.
   */
  public boolean toggleHeartbeat() {
    boolean heartbeat = false;
    if (this.running) {
      stopHeartbeat();
    } else {
      startHeartbeat();
      heartbeat = true;
    }
    return heartbeat;
  }

  private void stopHeartbeat() {
    this.running = false;
    if (this.communicationThread != null) {
      this.communicationThread.interrupt();
    }
  }

  /**
   * Send a command to the server.
   */
  public void sendCommand(String command) {
    try {
      if (this.objectWriter == null) {
        Logger.error("Object writer is null, cannot send command");
        return;
      }

      // Generate checksum for the command
      long checksum = ChecksumUtil.calculateChecksum(command);
      String packetWithChecksum = command + ";" + checksum;

      String encryptedCommand = EncryptionDecryption.encrypt(command, sharedSecret);
      this.objectWriter.writeObject(encryptedCommand);
      this.objectWriter.flush();
      Logger.info("Sent command: " + packetWithChecksum);
      Logger.info("Sent command: " + command);
    } catch (IOException e) {
      Logger.error("Error sending command: " + e.getMessage());
    } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
             IllegalBlockSizeException | BadPaddingException e) {
      Logger.error("Error encrypting command: " + e.getMessage());
    }
  }

  /**
   * Receive a response from the server.
   *
   * @return The response from the server.
   */
  public String receiveResponse() throws IOException {
    // Read a packet from the communication channel
    String packet = this.reader.readLine();
    if (packet == null) {
      Logger.error("Invalid packet received: null");
      return null;
    }


    // Handle plain-text responses (e.g., actuator status or sensor data without a checksum)
    if (!packet.contains(";")) {
      // Log warning for responses without a checksum
      Logger.info("Response missing checksum, assuming plain text: " + packet);
      return packet;  // Return plain-text responses as-is
    }

    // Handle command;checksum format
    String[] parts = packet.split(";", 2);
    if (parts.length != 2) {
      Logger.error("Malformed packet structure: " + packet);
      return null;
    }

    try {
      long receivedChecksum = Long.parseLong(parts[1]);
      if (ChecksumUtil.validateChecksum(parts[0], receivedChecksum)) {
        return parts[0];
      } else {
        Logger.error("Checksum validation failed for packet: " + packet);
      }
    } catch (NumberFormatException e) {
      Logger.error("Invalid checksum in packet: " + packet);
    }

    return null;
  }

  /**
   * Perform a key exchange with the server to establish a shared secret key.
   */
  private void exchangeKeys() {
    try {
      // Generate key pair
      KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("DH");
      keyPairGen.initialize(2048);
      KeyPair keyPair = keyPairGen.generateKeyPair();
      PrivateKey privateKey = keyPair.getPrivate();
      PublicKey publicKey = keyPair.getPublic();

      // Send public key to server
      objectWriter.writeObject(publicKey);
      objectWriter.flush();

      // Receive public key from server
      ObjectInputStream objectReader = new ObjectInputStream(socket.getInputStream());
      PublicKey serverPublicKey = (PublicKey) objectReader.readObject();

      // Generate shared secret
      KeyAgreement keyAgree = KeyAgreement.getInstance("DH");
      keyAgree.init(privateKey);
      keyAgree.doPhase(serverPublicKey, true);
      byte[] sharedSecretBytes = keyAgree.generateSecret();

      // Derive AES key from shared secret
      sharedSecret = new SecretKeySpec(sharedSecretBytes, 0, 16, "AES");
    } catch (NoSuchAlgorithmException | InvalidKeyException | IOException e) {
      Logger.error("Key exchange failed: " + e.getMessage());
    } catch (ClassNotFoundException e) {
      Logger.error("Error reading public key: " + e.getMessage());
    }
  }

}
