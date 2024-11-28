package no.ntnu.controlpanel;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import no.ntnu.commands.Command;
import no.ntnu.commands.CommandFactory;
import no.ntnu.greenhouse.GreenhouseSimulator;
import no.ntnu.tools.ChecksumHandler;
import no.ntnu.tools.EncryptionDecryption;
import no.ntnu.tools.Logger;

/**
 * Handles communication with TCP clients.
 */
public class ClientHandler extends Thread {
  private final GreenhouseSimulator client;
  private final Socket clientSocket;
  private ObjectInputStream objectReader;
  private PrintWriter socketWriter;
  private SecretKey sharedSecret;
  private final ChecksumHandler checksumHandler = new ChecksumHandler();

  /**
   * Create a new client handler.
   *
   * @param client       The greenhouse simulator
   * @param clientSocket The client socket
   */
  public ClientHandler(GreenhouseSimulator client, Socket clientSocket) {
    this.client = client;
    this.clientSocket = clientSocket;
    Logger.info("Greenhouse connected from " + clientSocket.getRemoteSocketAddress()
        + ", port: " + clientSocket.getPort());
  }

  /**
   * Runs the client handler.
   */
  @Override
  public void run() {
    if (establishStreams()) {
      handleClientRequest();
      closeSocket();
    }
    Logger.info("Exiting the handler of the greenhouse "
        + this.clientSocket.getRemoteSocketAddress());
  }

  private boolean establishStreams() {
    boolean success = false;
    try {
      // Initialize the streams
      this.objectReader = new ObjectInputStream(this.clientSocket.getInputStream());
      this.socketWriter = new PrintWriter(this.clientSocket.getOutputStream(), true);
      // Perform key exchange
      exchangeKeys();
      // If we reach this point, the streams are successfully established
      success = true;
    } catch (IOException e) {
      Logger.error("Failed to establish streams: " + e.getMessage());
    }
    return success;
  }

  private void handleClientRequest() {
    try {
      // While the thread is not interrupted, keep reading commands from the client
      while (!Thread.currentThread().isInterrupted()) {
        String command = receiveClientCommand();
        if (command == null) {
          Logger.info("Client connection closed");
          break;
        }
        boolean shouldContinue = handleCommand(command);
        if (!shouldContinue) {
          break;
        }
      }
    } catch (Exception e) {
      Logger.error("Error in client request handling: " + e.getMessage());
    } finally {
      closeSocket();
    }
  }

  private String receiveClientCommand() {
    try {
      // Check if the stream is available before reading
      if (objectReader == null || clientSocket.isClosed() || clientSocket.isInputShutdown()) {
        Logger.info("Socket is closed or input stream is shutdown");
        return null;
      }
      // Read the object from the stream
      Object receivedObject = objectReader.readObject();
      if (receivedObject == null) {
        Logger.info("Received null object - potential connection closure");
        return null;
      }
      // Check if the object is a string
      if (!(receivedObject instanceof String)) {
        Logger.error("Received non-string object: " + receivedObject.getClass());
        return null;
      }
      // Check if the string is in the correct format
      String receivedData = (String) receivedObject;
      // Split into two parts: the command and the checksum
      String[] parts = receivedData.split(":", 2);
      if (parts.length != 2) {
        Logger.error("Invalid command format: " + receivedData);
        return null;
      }
      // Validate the checksum and return the command
      return validateChecksum(parts);
    } catch (EOFException e) {
      // This typically means the connection was closed
      Logger.info("End of stream reached - connection likely closed");
      return null;
    } catch (IOException e) {
      Logger.error("IO Error while reading command: " + e.getMessage());
      return null;
    } catch (ClassNotFoundException e) {
      Logger.error("Deserialization error: class not found: " + e.getMessage());
      return null;
    } catch (NoSuchAlgorithmException e) {
      Logger.error("Checksum calculation error: " + e.getMessage());
      return null;
    }
  }

  private String validateChecksum(String[] parts) throws NoSuchAlgorithmException {
    String encryptedCommand = parts[0];
    String receivedChecksum = parts[1];
    String calculatedChecksum = checksumHandler.calculateChecksum(encryptedCommand);
    if (!receivedChecksum.equals(calculatedChecksum)) {
      Logger.error("Checksum mismatch: " + receivedChecksum + " != " + calculatedChecksum);
      return null;
    }
    return encryptedCommand;
  }

  private boolean handleCommand(String encryptedCommand) {
    boolean shouldContinue = true;
    // Check if the encrypted command is empty or null
    if (encryptedCommand == null || encryptedCommand.isEmpty()) {
      shouldContinue = false;
    }
    // Decrypt the command
    String command = decryptCommand(encryptedCommand);
    if (command == null) {
      shouldContinue = false;
    }
    // Special handling for shutdown command
    if (command.equals("SHUTDOWN")) {
      Logger.info("Received shutdown command from client");
      shouldContinue = false;
    }
    // Execute the command
    executeCommand(command);
    // If we reach this point, the command was successfully executed and we should continue
    return shouldContinue;
  }

  private void executeCommand(String command) {
    CommandFactory factory = new CommandFactory();
    Logger.info("Command from the client: " + command);
    String response = null;
    String encryptedResponse = null;
    // Execute the command
    try {
      Command cmd = factory.parseCommand(command);
      response = cmd.execute(client);
    } catch (IllegalArgumentException e) {
      response = "ERROR: Invalid command format - " + e.getMessage();
    } catch (Exception e) {
      response = "Command execution error: " + e.getMessage();
    }
    // Encrypt the response
    try {
      encryptedResponse = EncryptionDecryption.encrypt(response, sharedSecret);
    } catch (Exception e) {
      Logger.error("Error encrypting response: " + e.getMessage());
    }
    // Send the encrypted response to the client
    if (encryptedResponse != null) {
      sendToClient(encryptedResponse);
    }
  }

  private String decryptCommand(String encryptedCommand) {
    String command;
    try {
      command = EncryptionDecryption.decrypt(encryptedCommand, sharedSecret);
    } catch (Exception e) {
      Logger.error("Error decrypting command: " + e.getMessage());
      return null;
    }
    return command;
  }

  private void sendToClient(String response) {
    try {
      this.socketWriter.println(response);
    } catch (Exception e) {
      Logger.error("Failed to send response to client: " + e.getMessage());
    }
  }

  private void closeSocket() {
    try {
      if (clientSocket != null && !clientSocket.isClosed()) {
        Logger.info("Closing socket for " + clientSocket.getRemoteSocketAddress());
        if (objectReader != null) {
          objectReader.close();
        }
        if (socketWriter != null) {
          socketWriter.close();
        }
        clientSocket.close();
      }
    } catch (IOException e) {
      Logger.error("Error closing socket: " + e.getMessage());
    }
  }

  /**
   * Perform key exchange with the client.
   * This method was created with help from GitHub Copilot.
   */
  private void exchangeKeys() {
    try {
      // Generate key pair
      KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("DH");
      keyPairGen.initialize(2048);
      KeyPair keyPair = keyPairGen.generateKeyPair();
      PrivateKey privateKey = keyPair.getPrivate();
      PublicKey publicKey = keyPair.getPublic();

      // Send public key to client
      ObjectOutputStream objectWriter = new ObjectOutputStream(clientSocket.getOutputStream());
      objectWriter.writeObject(publicKey);
      objectWriter.flush();

      // Receive client's public key
      PublicKey clientPublicKey = (PublicKey) objectReader.readObject();

      // Generate shared secret
      KeyAgreement keyAgree = KeyAgreement.getInstance("DH");
      keyAgree.init(privateKey);
      keyAgree.doPhase(clientPublicKey, true);
      byte[] sharedSecretBytes = keyAgree.generateSecret();

      // Derive AES key from shared secret
      sharedSecret = new SecretKeySpec(sharedSecretBytes, 0, 16, "AES");
    } catch (NoSuchAlgorithmException | InvalidKeyException | ClassNotFoundException
             | IOException e) {
      Logger.error("Failed to generate key pair: " + e.getMessage());
    }
  }
}
