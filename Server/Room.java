package Server;

import Model.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
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
        // SYNCHRONIZED IN INDEX
        private final ArrayList<ServerThread> turn;
        private final ArrayList<String> turnNicks;
        private ServerThread dealer;
    private volatile boolean gameStarted = false;
    private final Deque<String> deck = new ArrayDeque<>();
    private GameData gameData;
    private HashSet<String> uncoveredCards;
    
    public Room(RoomData data) {
        this.pool = new ArrayList<>();
        this.nicknames = new ArrayList<>();
        this.admin = null;
        this.roomData = data;
        
        this.turn = new ArrayList<>();
        this.turnNicks = new ArrayList<>();
        this.dealer = null;
        this.gameData = new GameData();
        this.uncoveredCards = new HashSet<>();
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

    private synchronized void broadcastGameData() {
        GameData base = new GameData(this.gameData);
        for (int i = 0; i < base.size(); i++) {
            PlayerData p = base.getPlayer(i);
            if (uncoveredCards.contains(p.getCard())) continue;
            p.setCard("000");
        }

        for (ServerThread client : this.pool) {
            if (this.turn.contains(client)) {
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
            
            // Shuffle deck
            shuffleDeck();

            synchronized (this) {
                // Pick the dealer
                int d = turn.size()-1;
                if (d == -1) {
                    // If first round random pick from pool
                    Random rnd = new Random();
                    dealer = pool.get(rnd.nextInt(pool.size()));
                    d = pool.indexOf(dealer);
                }
                else {
                    // Else advance the turn
                    d++;
                    if (d >= turn.size()) d = 0;
                    dealer = turn.get(d);
                }
                //add dealer to data
                gameData.getPlayerByName(nicknames.get(d)).setDealer(true);
                
                // Make the turn based on dealer
                int j = d+1;
                turn.clear();
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

            // Update clients
            broadcastGameData();
            
            // Turn
            for (int i = 0; i < turnNicks.size(); i++) {
                String nick = turnNicks.get(i);
                ServerThread client = turn.get(i);
                PlayerData p = gameData.getPlayerByName(nick);

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

                    p.setChoice(choice);
                }

                // Execute change
                if (choice = true) {
                    if (client == dealer) {
                        //TODO scambia col mazzo
                    }
                    else {
                        PlayerData next = gameData.getPlayerByName(turnNicks.get(i+1));
                        // Tens block
                        if (next.getCardValue() == 10) {
                            // No code needed for the block

                            // Uncover
                            uncoveredCards.add(next.getCard());
                        }
                        if (next.getCardValue() == 9) { 
                            // Nines are jumped
                            int j = 2;
                            while (next.getCardValue() == 9) {
                                next = gameData.getPlayerByName(turnNicks.get(i+j));
                                
                                //TODO vedi se ha raggiunto il mazziere 
                            }
                        }
                        else {
                            // Change
                            String temp = next.getCard();
                            next.setCard(p.getCard());
                            p.setCard(temp);
                        }

                        //? fai saltare il turno a re e cavalli
                    }
                }

                // Update clients
                broadcastGameData();
            }

            // Calculate the leasts card values
            //TODO special cards
            ArrayList<String> leasts = new ArrayList<>();
            leasts.add(gameData.getPlayer(0).getNick());
            
            ArrayList<String> highests = new ArrayList<>();
            highests.add(gameData.getPlayer(0).getNick());
            
            int min = Integer.parseInt( gameData.getPlayer(0).getCard().substring(1) ); 
            int max = min;
            for (int i = 1; i < gameData.size(); i++) {
                PlayerData p = gameData.getPlayer(i);
                int v = gameData.getPlayer(i).getCardValue();

                if (v < min) {
                    leasts.clear();
                    leasts.add(p.getNick());

                    min = v;
                }
                else if (v == min) {
                    leasts.add(p.getNick());
                }

                if (v > max) {
                    highests.clear();
                    highests.add(p.getNick());

                    max = v;
                }
                else if (v == max) {
                    highests.add(p.getNick());
                }
            }

            // Find losers
            if (leasts.size() == 1) {
                // The player with the worst card
                PlayerData p = gameData.getPlayerByName(leasts.getLast());
                p.setLives(p.getLives()-1);
            }
            else if (leasts.size() == 2) {
                // Players with the highest card
                for (String nick : highests) {
                    PlayerData p = gameData.getPlayerByName(nick);
                    p.setLives(p.getLives()-1);
                }
            }
            else if (leasts.size() == 3) {
                // Only the dealer (which is always the last in turn)
                PlayerData p = gameData.getPlayerByName(turnNicks.getLast());
                p.setLives(p.getLives()-1);
            }
            else if (leasts.size() == 4) {
                // Everybody but players with the worst card
                for (String nick :turnNicks) {
                    if (leasts.contains(nick)) continue;

                    PlayerData p = gameData.getPlayerByName(nick);
                    p.setLives(p.getLives()-1);
                }
            }
            
            // Update clients
            broadcastGameData();

            while (true);
        }
    }
}
