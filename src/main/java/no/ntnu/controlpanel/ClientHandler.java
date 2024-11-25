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
public class ClientHandler implements Runnable {
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
    String command;
    boolean shouldContinue;
    do {
      command = receiveClientCommand();
      shouldContinue = handleCommand(command);
    } while (shouldContinue);
  }


  private String receiveClientCommand() {
    String command = "";
    try {
      command = (String) this.objectReader.readObject();
    } catch (IOException | ClassNotFoundException e) {
      Logger.error("Failed to read command from client: " + e.getMessage());
    }
    return command;
  }

  private boolean handleCommand(String command) {
    boolean shouldContinue = true;
    CommandFactory factory = new CommandFactory();
    System.out.println("Command from the client: " + command);
    String response = null;

    if (command == null || command.isEmpty()) {
      shouldContinue = false;
    } else {
      try {
        Command cmd = factory.parseCommand(command);
        response = cmd.execute(client);
      } catch (Exception e) {
        response = "ERROR: " + e.getMessage();
      }
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
      this.clientSocket.close();
    } catch (IOException e) {
      Logger.error("Failed to close client socket: " + e.getMessage());
    }
  }
}
