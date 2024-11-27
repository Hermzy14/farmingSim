package no.ntnu.controlpanel;

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


  public ClientHandler(GreenhouseSimulator client, Socket clientSocket) {
    this.client = client;
    this.clientSocket = clientSocket;
    System.out.println("Greenhouse connected from " + clientSocket.getRemoteSocketAddress() +
        ", port: " + clientSocket.getPort());
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
    System.out.println("Exiting the handler of the greenhouse " +
        this.clientSocket.getRemoteSocketAddress());
  }

  private boolean establishStreams() {
    boolean success = false;
    try {
      this.objectReader = new ObjectInputStream(this.clientSocket.getInputStream());
      this.socketWriter = new PrintWriter(this.clientSocket.getOutputStream(), true);

      exchangeKeys();

      success = true;
    } catch (IOException e) {
      Logger.error("Failed to establish streams: " + e.getMessage());
    }
    return success;
  }


  private void handleClientRequest() {
    try {
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

      Object receivedObject = objectReader.readObject();
      if (receivedObject == null) {
        Logger.info("Received null object - potential connection closure");
        return null;
      }

      if (!(receivedObject instanceof String)) {
        Logger.error("Received non-string object: " + receivedObject.getClass());
        return null;
      }

      return (String) receivedObject;
    } catch (java.io.EOFException e) {
      // This typically means the connection was closed
      Logger.info("End of stream reached - connection likely closed");
      return null;
    } catch (IOException e) {
      Logger.error("IO Error while reading command: " + e.getMessage());
      return null;
    } catch (ClassNotFoundException e) {
      Logger.error("Deserialization error: class not found: " + e.getMessage());
      return null;
    }
  }

  private boolean handleCommand(String encryptedCommand) {
    // Check if the encrypted command is empty or null
    if (encryptedCommand == null || encryptedCommand.isEmpty()) {
      return false;
    }
    String command;
    try {
      command = EncryptionDecryption.decrypt(encryptedCommand, sharedSecret);
    } catch (Exception e) {
      Logger.error("Error decrypting command: " + e.getMessage());
      return false;
    }

    // Special handling for shutdown command
    if ("SHUTDOWN".equals(command)) {
      Logger.info("Received shutdown command from client");
      return false;  // This will break the communication loop
    }

    boolean shouldContinue = true;
    CommandFactory factory = new CommandFactory();
    Logger.info("Command from the client: " + command);
    String response = null;

    try {
      Command cmd = factory.parseCommand(command);
      response = cmd.execute(client);
    } catch (IllegalArgumentException e) {
      response = "ERROR: Invalid command format - " + e.getMessage();
    } catch (Exception e) {
      response = "ERROR: Command execution failed - " + e.getMessage();
      Logger.error("Command execution error: " + e.getMessage());
    }

    if (response != null) {
      sendToServer(response);
    }

    return shouldContinue;
  }

  private void sendToServer(String response) {
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

  public void exchangeKeys() {
    try {
      // Generate key pair
      KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("DH");
      keyPairGen.initialize(2048);
      KeyPair keyPair = keyPairGen.generateKeyPair();
      PrivateKey privateKey = keyPair.getPrivate();
      PublicKey publicKey = keyPair.getPublic();

      // Receive public key from client
      PublicKey clientPublicKey = (PublicKey) objectReader.readObject();

      // Send public key to client
      ObjectOutputStream objectWriter = new ObjectOutputStream(clientSocket.getOutputStream());
      objectWriter.writeObject(publicKey);
      objectWriter.flush();

      // Generate shared secret
      KeyAgreement keyAgree = KeyAgreement.getInstance("DH");
      keyAgree.init(privateKey);
      keyAgree.doPhase(clientPublicKey, true);
      byte[] sharedSecretBytes = keyAgree.generateSecret();

      // Derive AES key from shared secret
      sharedSecret = new SecretKeySpec(sharedSecretBytes, 0, 16, "AES");
    } catch (NoSuchAlgorithmException | InvalidKeyException | ClassNotFoundException | IOException e) {
      Logger.error("Failed to generate key pair: " + e.getMessage());
    }
  }
}
