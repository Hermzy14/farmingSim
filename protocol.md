# Candidate mapping
This is a list of the candidates with their github usernames and candidate numbers.
MilkDirt = 10204
Hermzy14 = 10205
mustafyu = 10202
Dennij = 10225

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
We have chosen to use TCP as our underlying transport protocol, and 9057 as our port number. We choose TCP instead of
UDP because of the superior reliability. We believe it is more important for this application to have reliable data
transfer instead of prioritizing speed and efficiency.

## The architecture
We have defined the sensor/actuator nodes as the clients and the control-panel nodes as the servers.
- The control-panel nodes will initiate communication to request sensor data or send control commands to sensor/actuator nodes.
- The sensor/actuator nodes will respond to server requests by providing sensor data or executing commands sent by the control-panel nodes.

The different nodes:
1. Control-Panel nodes.
2. Sensor/Actuator nodes.

The sensor/actuator nodes collect data from sensors like temperature or humidity sensors, 
and control actuators like fans and heaters. The control-panel nodes act as user interfaces 
for monitoring sensor data and controlling actuators. They can send control commands to specific 
sensor/actuator nodes. They will also receive and visualize data from the sensor nodes, like sensor 
readings and actuator statuses.

## The flow of information and events
We have chosen a pull-based approach, where control panels request sensor data from sensor/actuator nodes.
This is a simple approach that works well for less frequent updates of sensor data. 

## Connection and state
Our communication protocol is connection-oriented and stateful. This is because we want to keep track of the state of the
sensor/actuator nodes and the control-panel nodes. This is important for the control-panel nodes to know which sensor/actuator
nodes are available and what their current state is. It being a connection-oriented protocol will provide us with a 
possibility for good error handling and ensuring data integrity.

## Types, constants
We have set specific values for the different message types. These are used to identify the type of message that is being sent.
Message Type Values:
- REQUEST_SENSOR_DATA = 0x01
- REQUEST_ACTUATOR_STATUS = 0x02
- SEND_ACTUATOR_COMMAND = 0x03
- LIST SENSORS = list

Nodes will have a unique identifier, They will go from 1 and up. This will be used to identify the different nodes in the system.
sensor node 1 = 1
command node 2 = 2
....


## Message format
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
4. LIST SENSORS
   - Request: The control panel sends a REQUEST_LIST message to a sensor node.
   - Response: The sensor node replies with a LIST message which lists all the available sensor/actuator nodes.

Message Type Values:
- REQUEST_SENSOR_DATA = 0x01
- REQUEST_ACTUATOR_STATUS = 0x02
- SEND_ACTUATOR_COMMAND = 0x03
- LIST SENSORS = list

For marshalling we will use TLV (Type-Lenght-Value) format. TLV is felxible and extensible, which is especially useful 
for future protocol upgrades.

TLV structure:
- Message Type: 1 byte: Defines the message category.
- Lenght: 2 bytes: Lenght of the value field.
- Node ID: 4 bytes: Unique identifier for the sender/recipient node.
- Timestamp: 4 bytes: Unix timestamp.
- Value: Variable: The actual payload

### Error messages
1. **MessageFormatError**:
   - Caused by receiving a message in an unexpected format.
   - The sensor/actuator nodes should handle this by logging the error, and ignore the message if it cannot be parsed. 
   Then send an error response back to the control-panel node.
   - Control-panel node should notify the user about the error ("Invalid response from sensor node") then we ask user 
   if we should retry sending the message.

## An example scenario
1.  Sensor Node ID= 1 is started:
- It initializes its sensors(1 temperature sensor, 2 humidity sensors) and its actuator (a window)
- It establishes a TCP connection with the control panel and send a registration packet.
2. Sensor Node ID=2 is started:
- It initializes its sensors (1 temperature sensor) and actuators (2 fans and a heater). 
- It establishes a TCP connection with the control panel and sends a similar registration packet:
3.  Control Panel Node 1 is started:
- It begins listening for incoming TCP connections from sensor/actuator nodes.
- It stores the capabilities of connected nodes.
4. Control Panel Node 2 is started:
- It also begins listening for TCP connections.
- It mirrors the same functionality as the first control panel.
5. Sensor Node ID=3 is started:
- It initializes its sensors (2 temperature sensors) with no actuators.
- It establishes a connection and registers
6. Request sensor data:
- Command panel request data from sensor nodes.
- 10 seconds after initialization, all three sensor nodes broadcast their current sensor data to the control panels.
7.   User interaction with control panel 1:
- The user of Control Panel 1 presses a button to turn on the first fan of Sensor Node ID=2.
- Control Panel 1 sends a command to Sensor Node ID=2.
- Sensor Node ID=2 turns on the fan and sends a confirmation back to the control panel.
8. User Interaction with Control Panel 2:
- The user of Control Panel 2 presses a button to turn off all actuators.
- Control Panel 2 sends commands to all sensor nodes with actuators.
- Each node executes the commands (e.g., closing the window, turning off fans, and shutting down the heater) and sends 
confirmations to Control Panel 2.

## Reliability and security
### Reliability:
- We have error handling for the different types of errors that can occur in the system. 
- We will also implement a checksum to compare the received data with the expected data.
- We define a timeout for each packet, so the sender can retransmit.

### Security:
- Use encryption key-exchange to encrypt and decrypt messages. 
This will ensure that the messages are secure and cannot be tampered with.
To ensure that the messages are secure, we are using the AES encryption algorithm. 
This will encrypt the messages before they are sent and decrypt them when they are received.
We share public keys when we set up the connection, and then we use these keys to encrypt and decrypt the messages.

- Use a MAC based authentication. This makes ti a lot harder for an intruder to tamper with the system.
The MAC authentication in our project is configured as an if statement which cancels communication if,
the nodeId is not equal to the nodeIds we have configured. There is alot of exceptions which will in most cases,
stop this before we reach this layer, but it is an extra security measure.