package no.ntnu.controlpanel;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.Socket;
import no.ntnu.commands.Command;
import no.ntnu.commands.CommandFactory;
import no.ntnu.greenhouse.GreenhouseSimulator;
import no.ntnu.tools.Logger;

/**
 * Handles communication with TCP clients.
 */
public class ClientHandler extends Thread {
  private final GreenhouseSimulator client;
  private final Socket clientSocket;
  private ObjectInputStream objectReader;
  private PrintWriter socketWriter;


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
      success = true;
    } catch (IOException e) {
      Logger.error("Failed to establish streams: " + e.getMessage());
    }
    return success;
  }


  private void handleClientRequest() {
//    String command;
//    boolean shouldContinue;
//    do {
//      command = receiveClientCommand();
//      shouldContinue = handleCommand(command);
//    } while (shouldContinue);
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
//    String command = null;
//    try {
//      command = (String) this.objectReader.readObject();
//    } catch (IOException e) {
//      Logger.error("Connection error while reading command: " + e.getMessage());
//    } catch (ClassNotFoundException e) {
//      Logger.error("Deserialization error: " + e.getMessage());
//    }
//    return command;
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

  private boolean handleCommand(String command) {
    // Check if the command is empty
    if (command == null || command.isEmpty()) {
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
        if (objectReader != null) objectReader.close();
        if (socketWriter != null) socketWriter.close();
        clientSocket.close();
      }
    } catch (IOException e) {
      Logger.error("Error closing socket: " + e.getMessage());
    }
  }
}
