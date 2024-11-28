package no.ntnu.commands;

import java.util.ArrayList;
import java.util.List;
import no.ntnu.greenhouse.Actuator;
import no.ntnu.greenhouse.GreenhouseSimulator;
import no.ntnu.greenhouse.Sensor;
import no.ntnu.greenhouse.SensorActuatorNode;

/**
 * Command for listing all sensors in the greenhouse.
 */
public class ListSensors extends Command {
  @Override
  public String execute(GreenhouseSimulator greenhouse) {
    // Get all sensors in the greenhouse
    ArrayList<SensorActuatorNode> sensors = greenhouse.getSensors();
    StringBuilder response = new StringBuilder();
    response.append("Sensors:\n");
    int i = 1;
    // For each sensor, list the sensor types and actuators
    for (SensorActuatorNode node : sensors) {
      List<String> sensorTypes = new ArrayList<>();
      for (Sensor sensor : node.getSensors()) {
        sensorTypes.add(sensor.getType());
      }
      List<String> actuators = new ArrayList<>();
      for (Actuator actuator : node.getActuators()) {
        actuators.add(actuator.getType());
      }
      response.append(i).append(". Node with nodeId = ").append(node.getId())
          .append(" has sensor types: ").append(sensorTypes)
          .append(", and has these actuators: ").append(actuators).append("\n");
      i++;
    }
    return response.toString().trim();
  }
}
