import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    static ServerSocket serverSocket;
    public static void main(String[] args) {
        // The number of command arguments only can be one
        if (args.length != 1) {
            System.out.println("Usage: java -cp bin Server <port>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        boolean active = true;

        try {
            // Scanner for reading user commands
            Thread userInputThread = new Thread(() -> {
                Scanner scanner = new Scanner(System.in);
                while (true) {
                    String userInput = scanner.nextLine().trim();
                    // Process user command
                    userCommand(userInput);
                }
            });
            userInputThread.start();

            // Setting up server and client sockets
            Server.serverSocket = new ServerSocket(port);
            System.out.println("Server running on port " + port);
            List<Socket> clientSockets = new ArrayList<>();

            // Executor service for handling client connections
            ExecutorService executorService = Executors.newCachedThreadPool();

            while (active) {
                Socket clientSocket = serverSocket.accept();
                clientSockets.add(clientSocket);
                System.out.println("Peer " + clientSocket.getRemoteSocketAddress() + " connected.");

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

    private static void handleClient(Socket clientSocket) throws IOException {
        try (InputStream input = clientSocket.getInputStream();
                OutputStream output = clientSocket.getOutputStream()) {

            byte[] buffer = new byte[1024];
            while (true) {
                int bytesRead = input.read(buffer);
                if (bytesRead == -1) {
                    // If peer disconnects
                    System.out.println("Peer " + clientSocket.getRemoteSocketAddress() + " disconnected.");
                    break;
                }

                // If peer sends a message
                String message = new String(buffer, 0, bytesRead);
                System.out.println("Message received from " + clientSocket.getRemoteSocketAddress() + ": " + message);
            }
        } finally {
            clientSocket.close();
        }
    }

    private static void userCommand(String command) {
        String[] parts = command.split(" ");
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "help":
                // Display the command manual
                System.out.println("testing");
                break;

            case "myip":
                // Display the IP address of the peer
                try(final DatagramSocket socket = new DatagramSocket())
                {
                    //Use Datagram socket to attempt to bing 8.8.8.8 which will fail.
                    //Port 10002 is used as one of 3 ports to do this type of connection.
                    //Then we can gather our prefered IP, which is our IPV4 address by default.
                    //Keep in mind that there are two prefered IPs, but usually connections will
                    //use iPv4 addresses unless specified otherwise.
                    socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
                    String ip = socket.getLocalAddress().getHostAddress();
                    //Should only output iPv4 Address
                    System.out.println(ip);
                }
                catch(Exception error)
                {
                    error.printStackTrace();
                }
                break;

            case "myport":
                // Display the port number that the process runs on 
                System.out.println("My Port: " + Server.serverSocket.getLocalPort());
                break;

            case "connect":
                // Implement connect command
                System.out.println("testing");
                break;

            case "exit":
                System.out.println("Exiting");
                break;

            default:
                System.out.println("Unknown command: " + cmd);
                break;
        }
    }
}