package no.ntnu.controlpanel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import no.ntnu.tools.Logger;

/**
 * Handles communication with TCP clients.
 */
public class ClientHandler implements Runnable {
  private final Socket clientSocket;

  public ClientHandler(Socket clientSocket) {
    this.clientSocket = clientSocket;
  }

  /**
   * The main loop for handling client communication.
   * This method should be called when the thread is started.
   * It will read messages from the client and send responses.
   * The method will return when the client disconnects.
   */
  @Override
  public void run() {
    try (
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

      Logger.info("Client handler started for " + clientSocket.getInetAddress());

      String receivedMessage;
      while ((receivedMessage = in.readLine()) != null) {
        Logger.info("Received message: " + receivedMessage);

        // Parse and process the message
        String response = processMessage(receivedMessage);
        if (response != null) {
          out.println(response);
          Logger.info("Sent response: " + response);
        }
      }
    } catch (IOException e) {
      Logger.error("Error in communication with client: " + e.getMessage());
    } finally {
      try {
        clientSocket.close();
        Logger.info("Client connection closed: " + clientSocket.getInetAddress());
      } catch (IOException e) {
        Logger.error("Failed to close client socket: " + e.getMessage());
      }
    }
  }

  /**
   * Processes a single message from the client and generates a response.
   * This should be implemented based on your protocol.
   *
   * @param message The received message.
   * @return The response to the client, or null if no response is required.
   */
  private String processMessage(String message) {
    // Example: Basic protocol processing
    if (message.startsWith("SENSOR_DATA_REQUEST")) {
      return "SENSOR_DATA: {temperature:22, humidity:45}";
    } else if (message.startsWith("COMMAND_ACTUATOR")) {
      return "ACK: Command received";
    }
    return "ERROR: Unknown message type";
  }
}
