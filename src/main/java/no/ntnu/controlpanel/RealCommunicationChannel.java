package no.ntnu.controlpanel;

import no.ntnu.tools.Logger;

/**
 * A communication channel for disseminating control commands to the sensor nodes
 * (sending commands to the server) and receiving notifications about events.
 */
public class RealCommunicationChannel implements CommunicationChannel {
  private final ControlPanelLogic logic;

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
    // Open the communication channel
    return true;
  }
}
