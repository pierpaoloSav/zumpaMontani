package Server;

import Model.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class ServerThread extends Thread {
    // Connection variables
    private final Server handler;
    private final Socket client;
    private Room room;
    private boolean connected = true;

    // Streams
    private final ObjectOutputStream output;
    private final ObjectInputStream input;

    public ServerThread(Socket client, Server handler) throws Exception {
        this.handler = handler;
        this.client = client;
        this.room = null;

        // Streams
        this.output = new ObjectOutputStream(client.getOutputStream());
        this.input = new ObjectInputStream(client.getInputStream());
    }
    
    public boolean isConnected() {
        return connected;
    }

    public synchronized void sendObject(Object obj) {
        try {
            output.writeObject(obj);
            output.reset();
            output.flush();
        } catch (IOException e) {
            System.err.println("Error in the outout:"  + e.getMessage());
        }
    }

    public void notifyClose() {
        // Notify the shutdown to the client (as protocol says)
        this.sendObject("0");    
    }

    public void close() {
        // Close
        try {
            client.close();
            connected = false;
        } catch (IOException e) {
            System.err.println("Error closing client comm:" + e.getMessage());
        }
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    // Game
    public void notifyGameStart() {
        this.sendObject("S1");    
    }

    public void askPlay() {
        this.sendObject("P");
    }

    public void sendGameData(GameData data) {
        this.sendObject("PD");
        this.sendObject(data);
    }

    @Override 
    public void run() {
        try {
            while (true) {
                Object in = (Object)input.readObject();

                if (in instanceof String s) {
                    if (s.equals("R01")) {
                        RoomData roomData = (RoomData) input.readObject();

                        // Create room
                        if (handler.createRoom(roomData)) { 
                            // Join it
                            if (handler.joinRoom(this, roomData.getRoomName(), roomData.getPin(), "ADMIN")) { //?temp admin nickname
                                this.sendObject("R011");
                            }
                            else {
                                this.sendObject("R010");
                            }
                        }
                        else {
                            this.sendObject("R010");
                        }
                    }
                    if (s.equals("R02")) {
                        ArrayList<String> namePin = (ArrayList<String>) input.readObject();
                        
                        // Join room
                        if (handler.joinRoom(this, namePin.get(0), namePin.get(1), namePin.get(2))) {
                            this.sendObject("R021");
                        }
                        else {
                            this.sendObject("R020");
                        }
                    }

                    if (s.equals("S")) {
                        // Start game
                        if (this.room.startGame(this)) {
                            this.sendObject("S1");
                        }
                        else {
                            this.sendObject("S0");
                        }
                    }

                    if (s.equals("P0")) {
                        this.room.setClientData(Boolean.FALSE);
                        synchronized (this) {
                            this.notifyAll();
                        }
                    }
                    if (s.equals("P1")) {
                        this.room.setClientData(Boolean.TRUE);
                        synchronized (this) {
                            this.notifyAll();
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Client disconnected: " + client.getInetAddress().getHostAddress());
        }
        finally {
            handler.removeClient(this);
            this.close();
        }
    }
}
