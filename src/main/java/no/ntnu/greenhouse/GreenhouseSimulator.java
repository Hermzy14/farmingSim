package no.ntnu.greenhouse;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.net.Socket;
import no.ntnu.controlpanel.ClientHandler;
import no.ntnu.controlpanel.SensorActuatorNodeInfo;
import no.ntnu.listeners.greenhouse.NodeStateListener;
import no.ntnu.tools.Logger;

/**
 * Application entrypoint - a simulator for a greenhouse.
 */
public class GreenhouseSimulator {
  private final Map<Integer, SensorActuatorNode> nodes = new HashMap<>();

  private final List<PeriodicSwitch> periodicSwitches = new LinkedList<>();
  private final boolean fake;
  public static final int TCP_PORT = 9057;
  private ServerSocket serverSocket;
  private boolean running;
  private Socket clientSocket;

  /**
   * Create a greenhouse simulator.
   *
   * @param fake When true, simulate a fake periodic events instead of creating
   *             socket communication
   */
  public GreenhouseSimulator(boolean fake) {
    this.fake = fake;
  }

  /**
   * Initialise the greenhouse but don't start the simulation just yet.
   */
  public void initialize() {
    createNode(1, 2, 1, 0, 0);
    createNode(1, 0, 0, 2, 1);
    createNode(2, 0, 0, 0, 0);
    Logger.info("Greenhouse initialized");
  }

  private void createNode(int temperature, int humidity, int windows, int fans, int heaters) {
    SensorActuatorNode node = DeviceFactory.createNode(
        temperature, humidity, windows, fans, heaters);
    nodes.put(node.getId(), node);
  }

  /**
   * Start a simulation of a greenhouse - all the sensor and actuator nodes inside it.
   */
  public void start() {
    initiateCommunication();
    for (SensorActuatorNode node : nodes.values()) {
      node.start();
    }
    for (PeriodicSwitch periodicSwitch : periodicSwitches) {
      periodicSwitch.start();
    }

    Logger.info("Simulator started");
  }

  private void initiateCommunication() {
    if (fake) {
      initiateFakePeriodicSwitches();
    } else {
      initiateRealCommunication();
    }
  }

  /**
   * Start the real communication with the greenhouse.
   */
  private void initiateRealCommunication() {
    new Thread(() -> {
      if (openListeningSocket()) {
        this.running = true;
        while (this.running) {
          clientSocket = acceptNextClient();
          if (clientSocket != null) {
            Logger.info("Accepted new client connection: " + clientSocket.getInetAddress());
            ClientHandler clientHandler = new ClientHandler(this, clientSocket);
            clientHandler.start();
          }
        }
      }
      System.out.println("Greenhouse server turning off...");
    }).start();
  }

  /**
   * Open a listening TCP socket.
   *
   * @return {@code true} on success, {@code false} on error.
   */
  private boolean openListeningSocket() {
    boolean success = false;
    try {
      this.serverSocket = new ServerSocket(this.TCP_PORT);
      success = true;
    } catch (IOException e) {
      System.err.println("Could not open a listening socket on port " + TCP_PORT
          + ", reason: " + e.getMessage());
    }
    return success;
  }

  /**
   * Accepts the next client and returns the socket.
   *
   * @return the socket of the client.
   */
  private Socket acceptNextClient() {
    Socket clientSocket = null;
    try {
      clientSocket = this.serverSocket.accept();
    } catch (IOException e) {
      System.err.println("Could not accept the next client: " + e.getMessage());
    }
    return clientSocket;
  }

  private void initiateFakePeriodicSwitches() {
    periodicSwitches.add(new PeriodicSwitch("Window DJ", nodes.get(1), 2, 20000));
    periodicSwitches.add(new PeriodicSwitch("Heater DJ", nodes.get(2), 7, 8000));
  }

  /**
   * Stop the simulation of the greenhouse - all the nodes in it.
   */
  public void stop() {
    stopCommunication();
    for (SensorActuatorNode node : nodes.values()) {
      node.stop();
    }
  }

  private void stopCommunication() {
    if (fake) {
      for (PeriodicSwitch periodicSwitch : periodicSwitches) {
        periodicSwitch.stop();
      }
    } else {
      this.running = false;
      try {
        if (clientSocket != null) {
          clientSocket.close();
        }
        if (serverSocket != null) {
          serverSocket.close();
        }
      } catch (IOException e) {
        System.err.println("Error while closing the communication: " + e.getMessage());
      }
    }
  }

  /**
   * Add a listener for notification of node staring and stopping.
   *
   * @param listener The listener which will receive notifications
   */
  public void subscribeToLifecycleUpdates(NodeStateListener listener) {
    for (SensorActuatorNode node : nodes.values()) {
      node.addStateListener(listener);
    }
  }

  /**
   * Return a sensor/actuator node by its ID.
   *
   * @return The node with the given ID, or {@code null} if not found
   */
  public SensorActuatorNode getSensorNode(int nodeId) {
    if (!nodes.containsKey(nodeId)) {
      throw new IllegalArgumentException("Node with ID " + nodeId + " not found");
    }
    return nodes.get(nodeId);
  }
}
