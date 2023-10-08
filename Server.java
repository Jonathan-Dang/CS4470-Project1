import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static ServerSocket serverSocket;
    private static List<Socket> clientSockets;
    private static boolean active = true;

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
                while (active) {
                    String userInput = scanner.nextLine().trim();
                    userCommand(userInput);
                }
            });
            userInputThread.start();

            // Setting up server and client sockets
            serverSocket = new ServerSocket(port);
            clientSockets = new ArrayList<>();
            System.out.println("\n  Server running on port " + port + "\n");

            // Executor service for handling client connections
            ExecutorService executorService = Executors.newCachedThreadPool();

            while (active) {
                Socket clientSocket = serverSocket.accept();
                clientSockets.add(clientSocket);
                System.out.println("\n  Peer " + clientSocket.getRemoteSocketAddress() + " connected.\n");

                // Handle client connection
                executorService.execute(() -> {
                    try {
                        handleClient(clientSocket);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
            serverSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static synchronized void handleClient(Socket clientSocket) throws IOException {
        try (InputStream input = clientSocket.getInputStream();
                OutputStream output = clientSocket.getOutputStream()) {

            byte[] buffer = new byte[1024];
            while (true) {
                int bytesRead = input.read(buffer);
                if (bytesRead == -1) {
                    // If peer disconnects
                    System.out.println("\n  Peer " + clientSocket.getRemoteSocketAddress() + " disconnected.\n");
                    break;
                }

                // If peer sends a message
                String message = new String(buffer, 0, bytesRead);
                System.out
                        .println("\n  Message received from " + clientSocket.getRemoteSocketAddress() + ": " + message
                                + "\n");
            }
        } finally {
            clientSocket.close();
        }
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

            case "myport":
                // Display the port number that the process runs on
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

            case "exit":
                System.out.println("Exiting");
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

            // Check if destination ip is a valid
            if (!isValid(destination)) {
                System.out.println("\n  Error: Invalid IP address.\n");
                return;
            }

            // Check if port number is in range
            if (port < 0 || port > 65353) {
                System.out.println("\n  Error: Invalid port number.\n");
                return;
            }

            // Check for self-connections
            String myIPAddress = getMyIPAddress();
            if (destination.equals(myIPAddress) && port == Server.serverSocket.getLocalPort()) {
                System.out.println("\n  Error: Cannot connect to your own IP address with the same port.\n");
                return;
            }

            // Check for duplicate connections
            if (isDuplicate(destination, port)) {
                System.out.println("\n  Error: Already connected to " + destination + " on port " + port + ".\n");
                return;
            }

            Socket socket = new Socket(destination, port);
            clientSockets.add(socket);
            System.out.println("\n  The connection to peer connected to " + destination + " on port " + port
                    + "is successfully established\n");
        } catch (IOException e) {
            System.out.println("\n  Connection to peer " + destination + " on port " + port + " failed.\n");
        }
    }

    private static boolean isValid(String ipAddress) {
        try {
            InetAddress.getByName(ipAddress);
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private static boolean isDuplicate(String destination, int port) {
        for (Socket socket : clientSockets) {
            String peerIPAddress = socket.getInetAddress().getHostAddress();
            int peerPort = socket.getPort();
            if (destination.equals(peerIPAddress) && port == peerPort) {
                return true;
            }
        }
        return false;
    }

    private static synchronized void terminateConnection() {
        // TODO: This function can be used individually but will be called in exit.
    }
}