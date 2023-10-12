import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Server {
    private static ServerSocket serverSocket;
    private static ConcurrentMap<Socket, Integer> clientPortsMap = new ConcurrentHashMap<>();
    private static ConcurrentMap<Socket, Integer> serverPortsMap = new ConcurrentHashMap<>();
    private static boolean active = true;
    private static int serverPort;

    public static void main(String[] args) {
        // The number of command arguments only can be one
        if (args.length != 1) {
            System.out.println("\n  Usage: java -cp bin Server <port>\n");
            System.exit(1);
        }
        int port = Integer.parseInt(args[0]);

        try {

            // Scanner for reading user commands
            Thread userInputThread = new Thread(() -> {
                Scanner scanner = new Scanner(System.in);
                while (Server.active) {
                    String userInput = scanner.nextLine().trim();
                    userCommand(userInput);
                }
                scanner.close();
            });
            userInputThread.start();

            // Setting up server and client sockets
            serverSocket = new ServerSocket(port);
            serverPort = serverSocket.getLocalPort();
            System.out.println("\n  Server listen on port " + serverPort + "\n");

            while (Server.active) {
                try {
                    Socket clientSocket = serverSocket.accept();

                    System.out.println("\n  Peer " + clientSocket.getInetAddress().getHostAddress() + " connected.\n");
                    handleClient(clientSocket);


                } catch (SocketException e) {
                    if (!Server.active) {
                        break;
                    } else {
                        e.printStackTrace();
                    }
                }
            }

            System.out.println("Exiting from Server Socket");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static synchronized void handleClient(Socket clientSocket) throws IOException {

        Thread clientThread = new Thread(() -> {
            try (InputStream input = clientSocket.getInputStream();OutputStream output = clientSocket.getOutputStream()) {

                byte[] buffer = new byte[1024];

                // Reading initial message for client's listening port
                int bytesRead = input.read(buffer);
                String initialMessage = new String(buffer, 0, bytesRead);
                int clientListeningPort = Integer.parseInt(initialMessage.trim());

                // Store clientListeningPort associated with clientSocket
                clientPortsMap.put(clientSocket, clientListeningPort);
                Integer clientPort = clientPortsMap.get(clientSocket);

                //Use an unsafe connection method since we know that the initial connection is safe
                if(!isDuplicateServer(clientSocket.getInetAddress().getHostAddress(), clientListeningPort))
                    unsafeConnect(clientSocket.getInetAddress().getHostAddress(), clientListeningPort);

                while (true) {
                    try {
                        bytesRead = input.read(buffer);
                        // if peer exit without terminating the connection
                        if (bytesRead == -1) {
                            System.out
                                    .println("\n  Peer " + clientSocket.getInetAddress().getHostAddress() + " "
                                            + clientPort + " disconnected.\n");
                            clientSocket.close();
                            clientPortsMap.remove(clientSocket);
                            break;
                        }

                        // If peer sends a message
                        String message = new String(buffer, 0, bytesRead);
                        if (message.equals("~~disconnect")) {
                            System.out
                                    .println("\n  Peer " + clientSocket.getInetAddress().getHostAddress() + " "
                                            + clientPort + " disconnected. [DISCONNECT]\n");
                            sendMessage(clientSocket, "Hello!");
                            clientSocket.close();
                            clientPortsMap.remove(clientSocket);
                            break;
                        } else
                            System.out.println(
                                    "\n  Message received from " + clientSocket.getInetAddress().getHostAddress() + " "
                                            + clientPort + ": " + message + "\n");
                    } catch (SocketException e) {
                        // if peer close the program without sending any request
                        System.out.println(
                                "\n  Peer " + clientSocket.getInetAddress().getHostAddress() + " "
                                        + clientPort + " disconnected abruptly.\n");
                        clientSocket.close();
                        clientPortsMap.remove(clientSocket);
                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        clientThread.start();
    }

    private static synchronized void userCommand(String command) {
        String[] parts = command.split(" ");
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "help":
                // Display the command manual
                displayHelpMessage();
                break;

            case "myip":
                String ipAddress = getMyIPAddress();
                if (ipAddress != null) {
                    System.out.println("\n  The IP address is " + ipAddress + "\n");
                } else {
                    System.out.println("\n  Error: Unable to retrieve the IP address.\n");
                }
                break;

            case "list":
                int id = 1;
                System.out.println("\n ID: IP Address       Port No.");
                // display connected clients
                System.out.println("[DEBUG]: Clients");
                for (Socket s : clientPortsMap.keySet()) {
                    displayConnectionDetails(s, id++);
                }
                System.out.println("[DEBUG]: Servers");

                // display connected servers
                for (Socket s : serverPortsMap.keySet()) {
                    displayConnectionDetails(s, id++);
                }
                System.out.println();
                break;

            case "myport":
                // Display the listen port number
                System.out.println("\n  The program runs on port number " + Server.serverSocket.getLocalPort() + "\n");
                break;

            case "connect":
                // Implement connect command
                if (parts.length == 3) {
                    String destination = parts[1];
                    int destinationPort = Integer.parseInt(parts[2]);
                    connectToDestination(destination, destinationPort);
                } else {
                    System.out.println("\n  Usage: connect <destination> <port>\n");
                }
                break;

            case "terminate":
                if (parts.length == 2) {
                    // Step 1: Send message to disconnect from other party
                    // Step 2: Make other socket remove our socket from list
                    // Step 3: Disconnect from socket at local end
                    // Step 4: Clear associated socket
                    Socket socketToTerminate[] = getSocketFromId(parts[1]);
                    if (socketToTerminate != null) {
                        terminateConnection(socketToTerminate);
                    }
                } else {
                    System.out.println("\n Usage: terminate <id>\n");
                }
                break;

            case "send":
                // Implement connect command
                if (parts.length == 3) {
                    Socket receiver[] = getSocketFromId(parts[1]);
                    if (receiver != null) {
                        String message = parts[2];
                        sendMessage(receiver[1], message);
                    }
                } else {
                    System.out.println("\n  Usage: send <id> <message>\n");
                }
                break;

            case "exit":
                // terminate connecion for each client and server
                //TODO:
                /*Set<Socket> clientSockets = new HashSet<>(clientPortsMap.keySet());
                for (Socket socket : clientSockets) {
                    terminateConnection(socket);
                }
                Set<Socket> serverSockets = new HashSet<>(serverPortsMap.keySet());
                for (Socket socket : serverSockets) {
                    terminateConnection(socket);
                }*/

                Server.active = false;
                try {
                    Server.serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            default:
                System.out.println("\n  Unknown command: " + cmd + "\n");
                break;
        }
    }

    private static void displayHelpMessage() {
        System.out.println("-------------------------------------------------------------");
        System.out.println("help                           - Display information about available commands.");
        System.out.println("myip                           - Display your public IP address.");
        System.out.println(
                "myport                         - Display the port on which you are listening for incoming connections.");
        System.out.println("connect <destination> <port>   - Establish a new connection to a peer.");
        System.out.println("list                           - List all connected peers.");
        System.out.println("terminate <connection_id>      - Terminate a specific connection.");
        System.out.println("send <connection_id> <message> - Send a message to a connected peer.");
        System.out.println("exit                           - Close all connections and exit the program.");
        System.out.println("-------------------------------------------------------------");
    }

    private static String getMyIPAddress() {
        // Display the IP address of the peer
        try (final DatagramSocket socket = new DatagramSocket()) {
            // Use Datagram socket to attempt to bing 8.8.8.8 which will fail.
            // Port 10002 is used as one of 3 ports to do this type of connection.
            // Then we can gather our prefered IP, which is our IPV4 address by default.
            // Keep in mind that there are two prefered IPs, but usually connections will
            // use iPv4 addresses unless specified otherwise.
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            String ip = socket.getLocalAddress().getHostAddress();
            // Should only output iPv4 Address
            return ip;
        } catch (Exception error) {
            return null;
        }
    }

    private static void connectToDestination(String destination, int port) {
        try {

            // Validate connection details
            String validationError = isValidConnection(destination, port);
            if (validationError != null) {
                System.out.println(validationError);
                return;
            }

            Socket socket = new Socket(destination, port);
            serverPortsMap.put(socket, port);

            // send the server's listening port as the first message
            OutputStream outs = socket.getOutputStream();
            String listeningPortMessage = String.valueOf(serverSocket.getLocalPort());
            outs.write(listeningPortMessage.getBytes());
            outs.flush();

            System.out.println("\n  The connection to peer connected to " + destination + " on port " + port
                    + " is successfully established\n");
        } catch (IOException e) {
            System.out.println("\n  Connection to peer " + destination + " on port " + port + " failed.\n");
        }
    }

    private static void unsafeConnect(String destination, int port){
        try {
            Socket socket = new Socket(destination, port);
            serverPortsMap.put(socket, port);

            // send the server's listening port as the first message
            OutputStream outs = socket.getOutputStream();
            String listeningPortMessage = String.valueOf(serverSocket.getLocalPort());
            outs.write(listeningPortMessage.getBytes());
            outs.flush();

            System.out.println("\n  The connection to peer connected to " + destination + " on port " + port
                    + " is successfully established\n");
        } catch (IOException e) {
            System.out.println("\n  Connection to peer " + destination + " on port " + port + " failed.\n");
        }
    }

    private static String isValidConnection(String destination, int port) {
        // Check if destination IP is valid
        if (!isIpValid(destination)) {
            return "\n  Error: Invalid IP address." + destination + "\n";
        }

        // Check if port number is in range
        if (port < 0 || port > 65353) {
            return "\n  Error: Invalid port number.\n";
        }

        // Check for self-connections
        String myIPAddress = getMyIPAddress();
        if (destination.equals(myIPAddress) && port == Server.serverSocket.getLocalPort()) {
            return "\n  Error: Cannot connect to your own IP address with the same port.\n";
        }

        // Check for duplicate connections
        if (isDuplicate(destination, port)) {
            return "\n  Error: Already connected to " + destination + " on port " + port + ".\n";
        }

        // No errors found
        return null;
    }

    private static boolean isIpValid(String ipAddress) {
        try {
            InetAddress.getByName(ipAddress);
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private static boolean isDuplicate(String destination, int port) {
        for (Map.Entry<Socket, Integer> entry : clientPortsMap.entrySet()) {
            Socket socket = entry.getKey();
            Integer storedPort = entry.getValue();

            String peerIPAddress = socket.getInetAddress().getHostAddress();
            if (destination.equals(peerIPAddress) && port == storedPort) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDuplicateServer(String destination, int port) {
        for (Map.Entry<Socket, Integer> entry : serverPortsMap.entrySet()) {
            Socket socket = entry.getKey();
            Integer storedPort = entry.getValue();

            String peerIPAddress = socket.getInetAddress().getHostAddress();
            if (destination.equals(peerIPAddress) && port == storedPort) {
                return true;
            }
        }
        return false;
    }

    private static synchronized void terminateConnection(Socket[] socketToTerminate) {
        // if socket exist in client list or server list
        if (socketToTerminate == null || (!clientPortsMap.containsKey(socketToTerminate[0])
                && !serverPortsMap.containsKey(socketToTerminate[1]))) {
            System.out.println("\n  Error: Invalid Socket.\n");
            return;
        }

        String disconnectCmd = "~~disconnect";
        sendMessage(socketToTerminate[1], disconnectCmd);

        try {
            socketToTerminate[0].close();
            socketToTerminate[1].close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            clientPortsMap.remove(socketToTerminate[0]);
            serverPortsMap.remove(socketToTerminate[1]);
        }
    }

    private static void sendMessage(Socket receiver, String message) {
        // if receiver exist in client list or server list
        if (receiver == null || (!clientPortsMap.containsKey(receiver)
                && !serverPortsMap.containsKey(receiver))) {
            System.out.println("\n  Error: Invalid Socket.\n");
            return;
        }
        try {
            OutputStream outs = receiver.getOutputStream();
            outs.write(message.getBytes());
            outs.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Socket[] getSocketFromId(String idString) {
        int id;
        try {
            id = Integer.parseInt(idString);
        } catch (NumberFormatException e) {
            System.out.println("\n Error: Invalid Socket ID \n");
            return null;
        }

        List<Socket> clientSocketList = new ArrayList<>(clientPortsMap.keySet());
        List<Socket> serverSocketList = new ArrayList<>(serverPortsMap.keySet());

        Socket ret[] = new Socket[2];

        if (id - 1 < 0 || id - clientSocketList.size() - 1 >= serverSocketList.size()) {
            System.out.println("\n Error: Invalid Socket ID \n");
            return null;
        } else if (id <= clientSocketList.size()) {
            ret[0] = clientSocketList.get(id - 1);
            ret[1] = serverSocketList.get(id - 1);
        }

        return ret;
    }

    private static void displayConnectionDetails(Socket s, int id) {
        String clientIpAddress = s.getInetAddress().getHostAddress();
        int clientListeningPort = clientPortsMap.getOrDefault(s, serverPortsMap.getOrDefault(s, -1));
        System.out.printf(" %d: %s       %d%n", id, clientIpAddress, clientListeningPort);
    }
}