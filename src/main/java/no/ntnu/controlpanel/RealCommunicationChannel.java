package no.ntnu.controlpanel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
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
import no.ntnu.tools.ChecksumHandler;
import no.ntnu.tools.EncryptionDecryption;
import no.ntnu.tools.Logger;

/**
 * A communication channel for disseminating control commands to the sensor nodes
 * (sending commands to the server) and receiving notifications about events.
 */
public class RealCommunicationChannel implements CommunicationChannel {
  private Socket socket;
  private BufferedReader reader;
  private ObjectOutputStream objectWriter;
  private static final String HOST = "localhost";
  private Thread communicationThread;
  private boolean running;
  private SecretKey sharedSecret;
  private final ChecksumHandler checksumHandler = new ChecksumHandler();

  /**
   * Create a new real communication channel.
   */
  public RealCommunicationChannel() {
  }

  @Override
  public void sendActuatorChange(int nodeId, int actuatorId, boolean isOn) {
    // Send the actuator change to the server
    String state = isOn ? "on" : "off";
    Logger.info("Sending command to greenhouse: turn " + state + " actuator"
        + "[" + actuatorId + "] on node " + nodeId);
  }

  /**
   * Open the communication channel.
   *
   * <p>If connection could not be established, we attempt again for a maximum amount of
   * 5 times, with 5 seconds between each attempt.</p>
   *
   * @return {@code true} if the connection was established, {@code false} otherwise.
   */
  @Override
  public boolean open() {
    int attempt = 1; // Current connection attempt
    int maxAttempts = 5; // Maximum number of connection attempts
    int delayBetweenAttempts = 5000; // Delay between connection attempts in milliseconds
    boolean success = false;
    // Try to establish a connection
    while ((attempt <= maxAttempts) && !success) {
      try {
        this.socket = new Socket(HOST, GreenhouseSimulator.TCP_PORT);
        this.objectWriter = new ObjectOutputStream(this.socket.getOutputStream());
        this.objectWriter.flush();
        this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
        // Perform key exchange
        exchangeKeys();
        // If we reach this point, the connection is successfully established
        Logger.success("Connection established!");
        success = true;
      } catch (IOException e) {
        Logger.error("Connection attempt " + attempt + " failed: " + e.getMessage());
        // Wait before next attempt
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
   * Closes the communication channel.
   */
  public void close() {
    try {
      // Send a shutdown signal if needed
      if (objectWriter != null) {
        sendCommand("SHUTDOWN");
      }
      // Close the socket
      if (socket != null && !socket.isClosed()) {
        socket.close();
      }
    } catch (IOException e) {
      Logger.error("Error closing socket: " + e.getMessage());
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
          sendHeartbeatRequest();
        } catch (IOException e) {
          handleHeartbeatConnectionError(e);
        } catch (InterruptedException e) {
          this.running = false;
        } catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException
                 | BadPaddingException | InvalidKeyException e) {
          Logger.error("Error while decrypting: " + e.getMessage());
        }
      }
    });
    communicationThread.setDaemon(true);
    communicationThread.start();
  }

  private void sendHeartbeatRequest()
      throws IOException, InterruptedException, NoSuchPaddingException, IllegalBlockSizeException,
      NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
    // Periodically send requests for sensor data
    for (int nodeId : new int[] {1, 2, 3}) {
      sendCommand("0x01 " + nodeId);
      String response = receiveResponse();
      Logger.info("Heartbeat response: " + response + "\n");
    }
    Thread.sleep(60000); // 1 minute between cycles
  }

  private void handleHeartbeatConnectionError(IOException e) {
    Logger.error("Error in heartbeat: " + e.getMessage());
    Logger.info("Trying to reconnect...");
    if (!open()) {
      Logger.error("Reconnection failed, stopping heartbeat");
      this.running = false;
    }
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
   *
   * @param command The command to send.
   */
  public void sendCommand(String command) {
    try {
      if (this.objectWriter == null) {
        Logger.error("Object writer is null, cannot send command");
        return;
      }
      // Encrypt command and calculate checksum
      String encryptedCommand = EncryptionDecryption.encrypt(command, this.sharedSecret);
      String checksum = this.checksumHandler.calculateChecksum(encryptedCommand);
      // Send encrypted command and checksum
      this.objectWriter.writeObject(encryptedCommand + ":" + checksum);
      this.objectWriter.flush();
      // Log the sent command
      Logger.info("Sent command: " + command);
    } catch (IOException e) {
      Logger.error("Error sending command: " + e.getMessage());
    } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
             | IllegalBlockSizeException | BadPaddingException e) {
      Logger.error("Error encrypting command: " + e.getMessage());
    }
  }

  /**
   * Receive a response from the server.
   *
   * @return The response from the server.
   */
  public String receiveResponse() throws IOException, NoSuchPaddingException,
      IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException,
      InvalidKeyException {
    // Read encrypted response
    String encryptedResponse = this.reader.readLine();
    // If the response is null, return null
    if (encryptedResponse == null) {
      return null;
    }
    // Return the decrypted response
    return EncryptionDecryption.decrypt(encryptedResponse, this.sharedSecret);
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
      this.objectWriter.writeObject(publicKey);
      this.objectWriter.flush();

      // Receive public key from server
      ObjectInputStream objectReader = new ObjectInputStream(socket.getInputStream());
      PublicKey serverPublicKey = (PublicKey) objectReader.readObject();

      // Generate shared secret
      KeyAgreement keyAgree = KeyAgreement.getInstance("DH");
      keyAgree.init(privateKey);
      keyAgree.doPhase(serverPublicKey, true);
      byte[] sharedSecretBytes = keyAgree.generateSecret();

      // Derive AES key from shared secret
      this.sharedSecret = new SecretKeySpec(sharedSecretBytes, 0, 16, "AES");
    } catch (NoSuchAlgorithmException | InvalidKeyException | IOException e) {
      Logger.error("Key exchange failed: " + e.getMessage());
    } catch (ClassNotFoundException e) {
      Logger.error("Error reading public key: " + e.getMessage());
    }
  }

}
