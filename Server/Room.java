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
        // SYNCHRONIZED IN INDEX
        private final ArrayList<ServerThread> pool;
        private final ArrayList<String> nicknames;
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
        this.nicknames = new ArrayList<>();
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

    public synchronized void joinRoom(ServerThread client, String pin, String nickname) throws Exception {
        if (this.roomData.getPin().equals(pin) && 
            this.roomData.getMaxPlayers() > this.pool.size()
        ) {
            this.pool.add(client);

            // Unique nickname 
            if (this.nicknames.contains(nickname)) {
                int i = 1;
                while (this.nicknames.contains(nickname+String.valueOf(i))) {
                    i++;
                }
                nickname += String.valueOf(i);
            }
            this.nicknames.add(nickname); 

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
            this.nicknames.remove(pool.indexOf(client));
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

    private synchronized void broadcastGameData(ArrayList<ServerThread> turn) {
        GameData base = new GameData(this.gameData);
        for (int i = 0; i < base.size(); i++) {
            base.getPlayer(i).setCard("000");
        }

        for (ServerThread client : this.pool) {
            if (turn.contains(client)) {
                //get info
                String name = this.nicknames.get(this.pool.indexOf(client));
                GameData temp = new GameData(base);
                String card = gameData.getPlayerByName(name).getCard();

                //set the card
                temp.getPlayerByName(name).setCard(card);

                //send
                client.sendGameData(temp);
            }
            else {
                client.sendGameData(base);
            }
        }
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
        synchronized (this) {
            for (String nickname : nicknames) {
                PlayerData temp = new PlayerData(nickname, roomData.getnLives());
                gameData.addPlayer(temp);
            }
        }

        // Game loop
        while (true) {
            Random rnd = new Random();

            // Shuffle deck
            shuffleDeck();

            ArrayList<ServerThread> turn = new ArrayList<>();
            ArrayList<String> turnNicks = new ArrayList<>();
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
                //add dealer to data
                gameData.getPlayerByName(nicknames.get(d)).setDealer(true);
                
                // Make the turn
                int j = d+1;
                for (int i = 0; i < pool.size(); i++) {
                    if (j >= pool.size()) j = 0;

                    turn.add(pool.get(j));
                    turnNicks.add(nicknames.get(j));
                    j++;
                }
            }

            // Deal cards
            for (int i = 0; i < turn.size(); i++) {
                gameData.getPlayerByName(turnNicks.get(i)).setCard(deck.peekFirst());
                deck.removeFirst();
            }

            broadcastGameData(turn);
            
            // Turns
            int n = 0;
            for (ServerThread client : turn) {
                // Wait the choice
                client.askPlay();
                boolean choice;
                synchronized (client) {
                    while (clientData == null) {
                        try {
                            client.wait();
                        } catch (InterruptedException e) {
                            System.err.println("Error reading data: " + e.getMessage());
                        }
                    }
                    choice = (Boolean)clientData;
                    clientData = null;

                    gameData.getPlayerByName(turnNicks.get(n)).setChoice(choice);
                }

                broadcastGameData(turn);
                
                n++;
            }

            // Calculate the leasts card values
            //TODO special cards
            ArrayList<String> leasts = new ArrayList<>();
            int min = Integer.parseInt( gameData.getPlayer(0).getCard().substring(1) ); 
            leasts.add(gameData.getPlayer(0).getNick());
            for (int i = 1; i < gameData.size(); i++) {
                PlayerData p = gameData.getPlayer(i);
                int v = Integer.parseInt( gameData.getPlayer(i).getCard().substring(1) );
                if (v < min) {
                    leasts.clear();
                    leasts.add(p.getNick());

                    min = v;
                }
                else if (v == min) {
                    leasts.add(p.getNick());
                }
            }

            // Find losers
            if (leasts.size() == 1) {
                PlayerData p = gameData.getPlayerByName(leasts.getLast());
                p.setLives(p.getLives()-1);
            }
            //TODO other cases

            while (true);
        }
    }
}
