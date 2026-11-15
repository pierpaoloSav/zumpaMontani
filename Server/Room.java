package Server;

import Model.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

public class Room extends Thread {
    // Connection variables
    private final ArrayList<ServerThread> pool;
    private ServerThread admin;
    private RoomData roomData;
    private Object clientData = null;
    
    // Game variables
    private volatile boolean gameStarted = false;
    private ServerThread dealer;
    private final Deque<String> deck = new ArrayDeque<>();
    private GameData gameData;
    
    public Room(RoomData data) {
        this.pool = new ArrayList<>();
        this.admin = null;
        this.roomData = data;
        
        this.dealer = null;
        this.gameData = new GameData();
    }

    public synchronized void closeRoom() {
        for (ServerThread client : pool) {
            try {
                client.notifyClose();
                client.close();
            } catch (Exception e) {
                System.err.println("Error in closing a room: " + e.getMessage());
            }
        }
    }

    public synchronized void joinRoom(ServerThread client, String pin) throws Exception {
        if (this.roomData.getPin().equals(pin) && 
            this.roomData.getMaxPlayers() > this.pool.size()
        ) {
            this.pool.add(client);
            System.out.println("N client: " + this.pool.size());

            // Set admin 
            if (this.pool.size() == 1) admin = client;
        }
        else {
            throw new Exception("Client refused");
        } 
    }

    public synchronized void removeClient(ServerThread client) throws Exception {
        if (this.pool.contains(client)) {
            this.pool.remove(client);

            // Find new admin (take the first)
            if (client == this.admin) {
                this.admin = this.pool.get(0);
            }
        }
        else {
            throw new Exception("Thread not found");
        }

    }

    public synchronized boolean startGame(ServerThread askedBy) {
        if (!this.gameStarted && 
            askedBy == this.admin &&
            this.pool.size() >= this.roomData.getMinPlayers()
        ) {
            // Notify all the other clients
            for (ServerThread client : this.pool) {
                if (client != this.admin)
                    client.notifyGameStart();
            }

            System.out.println("GAME STARTING");
            this.gameStarted = true;   
        }

        return this.gameStarted;
    }

    public synchronized String getRoomName() {
        return roomData.getRoomName();
    }

    public synchronized void setClientData(Object data) {
        this.clientData = data;
    }

    // Game
    private void shuffleDeck() {
        // Init
        char[] seeds = {'B', 'C', 'S', 'D'};
        List<String> cards = new ArrayList<>(40);
        for (int i = 0; i < 40; i++) {
            //seed
            int s = i / 10;
            //n
            int n = (i - s*10) + 1;

            cards.add(seeds[s] + String.format("%02d", n));
        }
        // Shuffle
        Collections.shuffle(cards);
        // Put in queue
        deck.clear();
        deck.addAll(cards);
    }

    @Override 
    public void run() {
        // Wait
        while (!gameStarted) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                System.err.println("Error sleeping: " + e.getMessage());
            }
        }

        // Init gameData
        for (int i = 0; i < pool.size(); i++) {
            PlayerData temp = new PlayerData(roomData.getnLives());
            gameData.addPlayer(temp);
        }

        // Game loop
        while (true) {
            Random rnd = new Random();

            // Shuffle deck
            shuffleDeck();

            ArrayList<ServerThread> turn = new ArrayList<>();
            synchronized (this) {
                // Pick the dealer
                int d = pool.indexOf(dealer);
                if (d == -1) {
                    // If first round random pick
                    dealer = pool.get(rnd.nextInt(pool.size()));
                    d = pool.indexOf(dealer);
                }
                else {
                    // Else advance
                    d++;
                    if (d >= pool.size()) d = 0;
                    dealer = pool.get(d);
                }
                
                // Make the turn
                int j = d+1;
                for (int i = 0; i < pool.size(); i++) {
                    if (j >= pool.size()) j = 0;

                    turn.add(pool.get(j));
                    j++;
                }
            }

            // Deal cards
            //TODO fix gameData.getPlayer facendo un hashmap che li identifica con un nick
            //TODO strutturara come mandare il dato alla pool ad ogni cambiamento
            for (int i = 0; i < gameData.size(); i++) {
                gameData.getPlayer(i).setCard(deck.peekFirst());
                deck.removeFirst();
            }

            // Turns
            int i = 0;
            for (ServerThread client : turn) {
                // Cover cards
                GameData temp = new GameData(gameData);
                for (int j = 0; j < temp.size(); j++) {
                    if (i != j) temp.getPlayer(j).setCard("000");
                }
                client.sendGameData(temp); //? send vanno fatti in altro modo ovv

                client.askPlay();

                // Wait the choice
                boolean choice = false; //def choice is to stay
                synchronized (client) {
                    while (clientData == null) {
                        try {
                            client.wait();
                        } catch (InterruptedException e) {
                            System.err.println("Error reading data: " + e.getMessage());
                        }
                    }
                    choice = (Boolean)clientData;
                    gameData.getPlayer(turn.indexOf(client)).setChoice(choice);
                }
                
                client.sendGameData(temp); //?
                
                clientData = null;
                i++;
            }

            while (true);
        }
    }
}
