# A Chat Application for Remote Message Exchange

A comprehensive peer-to-peer chat application built for real-time message exchange between peers across different systems using TCP sockets.

## Features
- Uses Java's `Socket` and `ServerSocket` classes to establish reliable TCP connections.
- Facilitates both client and server functionalities in a unified manner allowing each peer to behave as both a client and a server.
- Provides a robust set of commands for users to control and interact with the application.
- Validates connection details to avoid common issues such as self-connection, duplicate connections, and invalid IP addresses.
- Offers a seamless way to retrieve the IP address of the user's machine.
- Uses Datagram sockets to accurately fetch the user's IP address, typically preferring IPv4.

## Prerequisites
- **System Requirements**: Any operating system capable of running Java applications.
- **Software Requirements**: Latest version of Java. Verify the installation with:
    ```bash
    java -version
    ```

## Installation and Building the Program
1. Clone the repository:
    ```bash
    git clone https://github.com/Jonathan-Dang/CS4470-Project1.git
    ```
2. Navigate to the project directory:
    ```bash
    cd [File Directory Name]
    ```
3. Compile the Java file:
    ```bash
    javac chat.java
    ```

## Running the Application
To initiate the Chat Application peer:
```bash
java Server <port>
```
Replace <port> with the port number you want the Chat Application to listen on. The IP will be automatically set as the IP of your machine.

## Usage
Here are the commands available within the application:
- `help` - Display information about the available commands.
- `myip` - Show the public IP address of the user's machine.
- `myport` - Display the port on which the application is listening for incoming connections.
- `connect` <destination> <port> - Establish a TCP connection to a given IP address and port.
- `list` - List all active connections with their details.
- `terminate` <connection_id> - Disconnect from a specific connection using its ID.
- `send` <connection_id> <message> - Transmit a message to a connected peer using its connection ID.
- `exit` - Shutdown the application and terminate all active connections.

Additional Information
- The system primarily focuses on handling IPv4 addresses.
- The system maintains separate mappings for client and server ports (clientPortsMap and serverPortsMap), though their distinction is not entirely clear based on the provided snippets. More details about these maps can be found in the codebase.

## Contributing
Steven Wang
Jonathan Dang <CIN: 402381587>
- Both authors contributed equally to all aspects of the project, including development, testing, and documentation.

## License
This project is licensed under the [MIT License](LICENSE).