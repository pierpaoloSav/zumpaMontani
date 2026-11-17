package Server;

import Model.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {
    // Connection variables
    private final int port;
    private ServerSocket server;

    // Threads
    private Acceptor acceptor;
    private final ArrayList<ServerThread> waitingPool;
    private final ArrayList<Room> rooms;

    public Server(int port) {
        this.port = port;

        this.waitingPool = new ArrayList<>();
        this.rooms = new ArrayList<>();
    }

    // Server handling
    public synchronized void start() throws Exception {
        // Start server
        this.server = new ServerSocket(this.port);
        System.out.println("Server started on port " + this.port);

        // Set a shutdown hook for proper shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            this.shutdown();
        }));

        // Open listening
        this.acceptor = new Acceptor(this);
        this.acceptor.setDaemon(true);
        this.acceptor.start();
    }

    public synchronized void shutdown() {
        // Close server
        this.acceptor.interrupt();
        try {
            this.server.close();
        } catch (IOException e) {
            System.err.println("Error shutting server: " + e.getMessage());
        }

        // Close waiting threads
        for (ServerThread client : this.waitingPool) {
            client.notifyClose();
            client.close();
        }

        // Close rooms
        for (Room room : rooms) {
            room.closeRoom();
        }
        rooms.clear();
    }

    // Client joining
    public void addClient(Socket client) throws Exception {
        // Create a new thread for the client
        ServerThread thread = new ServerThread(client, this);

        synchronized (this) {
            waitingPool.add(thread);
            thread.start();
        }
    }

    public synchronized void removeClient(ServerThread client)  {
        // Search for the client in the waiting pool and rooms and remove it
        boolean found = false;
        for (Room room : rooms) {
            try {
                room.removeClient(client);
                found = true;
                break;
            } catch (Exception e) {}
        }

        if (this.waitingPool.contains(client)) {
            this.waitingPool.remove(client);
        }
        else if (!found) {
            System.err.println("Thread not found");
        }
    }

    public void listenClient() throws Exception {
        // Waiting a client
        Socket client = server.accept();
        System.out.println("New TCP connection: " + client.getInetAddress().getHostAddress());

        // Add client
        addClient(client);
    }

    // Room handling
    public synchronized boolean createRoom(RoomData data) {
        // Name has to be unique
        for (Room r : rooms) {
            if (r.getRoomName().equals(data.getRoomName())) {
                return false;
            }
        }

        Room room = new Room(data);
        room.start();
        this.rooms.add(room);
        return true;
    }

    public synchronized boolean joinRoom(ServerThread client, String roomName, String roomPin, String nickname) {
        // Find by name
        for (Room room : rooms) {
            if (room.getRoomName().equals(roomName)) {
                try {
                    room.joinRoom(client, roomPin, nickname);
                    this.waitingPool.remove(client);
                    client.setRoom(room);

                    return true;
                } catch (Exception e) {
                    System.err.println("Failed to join room: " + e.getMessage());
                    return false;
                } 
            }
        }
        return false;
    }

    public static void main(String[] args) {
        try {
            Server server = new Server(6060);
            server.start();

            while (true) {}
        } catch (Exception e) {
            System.out.println("Fatal error: " + e.getMessage());
        }
        finally {    
            System.out.println("Server closed.");
        }
    }
}

