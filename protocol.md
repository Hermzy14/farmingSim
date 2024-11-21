# Communication protocol

This document describes the protocol used for communication between the different nodes of the
distributed application.

## Terminology

* Sensor - a device which senses the environment and describes it with a value (an integer value in
  the context of this project). Examples: temperature sensor, humidity sensor.
* Actuator - a device which can influence the environment. Examples: a fan, a window opener/closer,
  door opener/closer, heater.
* Sensor and actuator node - a computer which has direct access to a set of sensors, a set of
  actuators and is connected to the Internet.
* Control-panel node - a device connected to the Internet which visualizes status of sensor and
  actuator nodes and sends control commands to them.
* Graphical User Interface (GUI) - A graphical interface where users of the system can interact with
  it.

## The underlying transport protocol

TODO - what transport-layer protocol do you use? TCP? UDP? What port number(s)? Why did you 
choose this transport layer protocol?

We have chosen to use TCP as our underlying transport protocol, and 9057 as our port number. We choose TCP instead of
UDP because of the superior reliability. We believe it is more important for this application to have reliable data
transfer instead of prioritizing speed and efficiency.

## The architecture

TODO - show the general architecture of your network. Which part is a server? Who are clients? 
Do you have one or several servers? Perhaps include a picture here. 

We have defined the control-panel nodes as the clients and the sensor/actuator nodes as the servers.
- The control-panel nodes will initiate communication to request sensor data or send control commands to sensor/actuator nodes.
- The sensor/actuator nodes will respond to client requests by providing sensor data or executing commands sent by the control-panel nodes.

The different nodes:
1. Sensor/Actuator nodes.
2. Control-Panel nodes.

The sensor/actuator nodes collect data from sensors like temperature or humidity sensors, 
and control actuators like fans and heaters. The control-panel nodes act as user interfaces 
for monitoring sensor data and controlling actuators. They can send control commands to specific 
sensor/actuator nodes. They will also receive and visualize data from the sensor nodes, like sensor 
readings and actuator statuses.

## The flow of information and events

TODO - describe what each network node does and when. Some periodic events? Some reaction on 
incoming packets? Perhaps split into several subsections, where each subsection describes one 
node type (For example: one subsection for sensor/actuator nodes, one for control panel nodes).

We have chosen a pull-based approach, where control panels request sensor data from sensor/actuator nodes.
This is a simple approach that works well for less frequent updates of sensor data. 

## Connection and state

TODO - is your communication protocol connection-oriented or connection-less? Is it stateful or 
stateless? 

Our communication protocol is connection-oriented and stateful. This is because we want to keep track of the state of the
sensor/actuator nodes and the control-panel nodes. This is important for the control-panel nodes to know which sensor/actuator
nodes are available and what their current state is. It being a connection-oriented protocol will provide is with a 
possibility for good error handling and ensuring data integrity.

## Types, constants

TODO - Do you have some specific value types you use in several messages? They you can describe 
them here.

## Message format

TODO - describe the general format of all messages. Then describe specific format for each 
message type in your protocol.

We are going to have two main message categories: Sensor messages and Command messages.

1. SENSOR_DATA (pull sensor data)
   - Request: The control panel sends a REQUEST_SENSOR_DATA message to a sensor node.
   - Response: The sensor node replies with a SENSOR_DATA message.
2. ACTUATOR_STATUS (pull actuator state)
   - Request: The control panel sends a REQUEST_ACTUATOR_STATUS message.
   - Response: The actuator replies with an ACTUATOR_STATUS message. 
3. COMMAND_TO_ACTUATOR (push command, optional pull acknowledgment)
   - Push command: the control panel sends a COMMAND_TO_ACTUATOR message.
   - Pull Acknowledgment: The control panel sends a REQUEST_COMMAND_ACK message.
   - Response: the actuator replies with an ACK message indicating whether the command was received and executed.
4. BROADCAST_MESSAGE (pull for broadcast updates)
   - Request: The node sends a REQUEST_BROADCAST_UPDATE message with the topic ID.
   - Response: the control panel responds with the latest BORADCAST_MESSAGE.
5. ACK (acknowledgment on demand)
   - Request: The control panel sends a REQUEST_ACK message with a specific message ID.
   - Response: The recipient node replies with an ACK.

For marshalling we will use TLV (Type-Lenght-Value) format. TLV is felxible and extensible, which is especially useful 
for future protocol upgrades.

TLV structure:
- Message Type: 1 byte: Defines the message category.
- Lenght: 2 bytes: Lenght of the value field.
- Node ID: 4 bytes: Unique identifier for the sender/recipient node.
- Timestamp: 4 bytes: Unix timestamp.
- Value: Variable The actual payload

### Error messages

TODO - describe the possible error messages that nodes can send in your system.

1. **MessageFormatError**:
   - Caused by receiving a message in an unexpected format.
   - The sensor/actuator nodes should handle this by logging the error, and ignore the message if it cannot be parsed. 
   Then send an error response back to the control-panel node.
   - Control-panel node should notify the user about the error ("Invalid response from sensor node") then we ask user 
   if we should retry sending the message.
2. **ConnectionError**:
   - If a node receives a message with an invalid or expired session.
   - The control-panel node will attempt to reconnect automatically and retry the message transmission up to a specified 
   number of attempts. If we exceed the max retries, then we log the connection error and notify the application 
   layer and user.
   - The sensor/actuator nodes should also retry the message transmission up to the defined limit, as well as log the 
   failure and send an alert to the control-panel (if possible).
3. **UnexpectedError**:
   - For any other unexpected errors.
   - Should log the error details.
   - Handle the error "gracefully" so that nothing crashes.
   - Notify application layer about the error.
   - Notify user about error.

## An example scenario

TODO - describe a typical scenario. How would it look like from communication perspective? When 
are connections established? Which packets are sent? How do nodes react on the packets? An 
example scenario could be as follows:
1. A sensor node with ID=1 is started. It has a temperature sensor, two humidity sensors. It can
   also open a window.
2. A sensor node with ID=2 is started. It has a single temperature sensor and can control two fans
   and a heater.
3. A control panel node is started.
4. Another control panel node is started.
5. A sensor node with ID=3 is started. It has a two temperature sensors and no actuators.
6. After 5 seconds all three sensor/actuator nodes broadcast their sensor data.
7. The user of the first-control panel presses on the button "ON" for the first fan of
   sensor/actuator node with ID=2.
8. The user of the second control-panel node presses on the button "turn off all actuators".

## Reliability and security

TODO - describe the reliability and security mechanisms your solution supports.
### Reliability:
- We have error handling for the different types of errors that can occur in the system. 
- We will also implement a checksum to compare the received data with the expected data.
- We define a timeout for each packet, so the sender can retransmit.
### Security:
- Use encryption key-exchange to encrypt and decrypt messages.
- Use a MAC based authentication. This makes ti a lot harder for an intruder to tamper with the system.