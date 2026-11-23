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
    private volatile boolean gameStarted = false;
    
    private RoomData roomData;
    private Object clientData = null;
    
    // Game variables
        // SYNCHRONIZED IN INDEX
        private final ArrayList<ServerThread> turn; 
        private final ArrayList<String> turnNicks; 
    private GameData gameData; 
    private HashSet<String> uncoveredCards; 
    
    // Deck
    private final Deque<String> deck = new ArrayDeque<>();
    private final String COVERED_CODE = "000";
    private final String SPECIAL_LOSE_CODE = "PRE";
    private final String SPECIAL_SAVE_CODE = "SCA";
    private final int BLOCK_VALUE = 10;
    private final int JUMP_VALUE = 9;

    public Room(RoomData data) {
        this.pool = new ArrayList<>();
        this.nicknames = new ArrayList<>();
        this.admin = null;
        this.roomData = data;

        this.turn = new ArrayList<>();
        this.turnNicks = new ArrayList<>();
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
            // Remove from pool
            this.nicknames.remove(pool.indexOf(client));
            this.pool.remove(client);

            // Remove from turn
            if (turn.contains(client)) {
                String nick = turnNicks.get(this.turn.indexOf(client));
                this.gameData.removePlayerByName(nick);

                this.turnNicks.remove(nick);
                this.turn.remove(client);
            }

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
        // Special cards
        cards.add(SPECIAL_LOSE_CODE);
        cards.add(SPECIAL_SAVE_CODE);
        // Shuffle
        Collections.shuffle(cards);
        // Put in queue
        deck.clear();
        deck.addAll(cards);
    }

    private synchronized void broadcastPublicGameData() { 
        for (ServerThread client : this.pool) {
            client.sendGameData(gameData);
        }
    }

    private synchronized void broadcastGameData() {
        GameData base = new GameData(this.gameData);
        for (int i = 0; i < base.size(); i++) {
            PlayerData p = base.getPlayer(i);
            if (uncoveredCards.contains(p.getCard())) continue;
            p.setCard(COVERED_CODE);
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
            
            // Prepare class variables
            shuffleDeck();
            uncoveredCards.clear();
            for (int i = 0; i < gameData.size(); i++) {
                gameData.getPlayer(i).setChoice(false);
            }

            synchronized (this) {
                String dealerNick = null;

                // Find the previous dealer
                for (int i = 0; i < gameData.size(); i++) {
                    if (gameData.getPlayer(i).isDealer()) {
                        //change and get nick
                        gameData.getPlayer(i).setDealer(false);
                        if (i+1 >= gameData.size()) {
                            gameData.getPlayer(0).setDealer(true);
                            dealerNick = gameData.getPlayer(0).getNick();
                        }
                        else {
                            gameData.getPlayer(i+1).setDealer(true);
                            dealerNick = gameData.getPlayer(i+1).getNick();
                        }
                        break;
                    }
                }
                // First round: select random
                if (dealerNick == null) {
                    Random rnd = new Random();
                    int n = rnd.nextInt(gameData.size());
                    gameData.getPlayer(n).setDealer(true);
                    dealerNick = gameData.getPlayer(n).getNick();
                }

                // Build the turn
                turn.clear();
                turnNicks.clear();
                int dealerIndex = nicknames.indexOf(dealerNick);

                // Make the turn based on dealer
                int j = dealerIndex + 1;
                for (int i = 0; i < gameData.size(); i++) {
                    if (j >= gameData.size()) j = 0;

                    String nickname = gameData.getPlayer(j).getNick();

                    turnNicks.add(nickname);
                    turn.add(pool.get(nicknames.indexOf(nickname)));

                    j++;
                }
            }

            // Deal cards
            for (int i = 0; i < turn.size(); i++) {
                gameData.getPlayerByName(turnNicks.get(i)).setCard(deck.peekFirst());
                deck.removeFirst();
            }
            
            // Turn
            for (int i = 0; i < turnNicks.size(); i++) {
                String nick = turnNicks.get(i);
                ServerThread client = turn.get(i);
                PlayerData p = gameData.getPlayerByName(nick);
                p.setMyTurn(true);

                // Update clients
                broadcastGameData();

                // Blocks and jumps dont choice
                if (p.getCardValue() == BLOCK_VALUE || p.getCardValue() == JUMP_VALUE) continue;

                // Wait the choice
                client.askPlay();
                boolean choice;
                synchronized (client) {
                    //TODO action timer, timeout kick
                    //TODO mentre è in wait deve essere cacciato lo stesso
                    //TODO deve scalare il dealer
                    //TODO chi ha perso viene cacciato

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
                if (choice) {
                    if (p.isDealer()) {
                        // Change with the deck
                        p.setCard(deck.removeFirst());
                    }
                    else {
                        int j = 1;
                        PlayerData next = null;

                        // Find the next player who doesnt have a jump card
                        while (j < turnNicks.size()) {
                            int nextIndex = (i + j) % turnNicks.size();

                            next = gameData.getPlayerByName(turnNicks.get(nextIndex));

                            if (next.getCardValue() != JUMP_VALUE) {
                                break;
                            }

                            // Uncover the jump card
                            uncoveredCards.add(next.getCard());

                            j++;
                        }

                        // If all remaining players have a jump card, change with the deck
                        if (j >= turnNicks.size()) {
                            p.setCard(deck.removeFirst());
                        }
                        else if (next.getCardValue() == BLOCK_VALUE) {
                            // Block card blocks the exchange
                            uncoveredCards.add(next.getCard());
                        }
                        else {
                            // Exchange cards
                            String temp = next.getCard();
                            next.setCard(p.getCard());
                            p.setCard(temp);
                        }
                    }
                }

                p.setMyTurn(false);
            }

            // Calculate the leasts card values
            ArrayList<String> leasts = new ArrayList<>();
            leasts.add(gameData.getPlayer(0).getNick());
            
            ArrayList<String> highests = new ArrayList<>();
            highests.add(gameData.getPlayer(0).getNick());

            int min = gameData.getPlayer(0).getCardValue();
            int max = min;
            for (int i = 1; i < gameData.size(); i++) {
                PlayerData p = gameData.getPlayer(i);
                int v = p.getCardValue();
                
                //skip special
                if (v == -1) continue;

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

            // Find SPECIAL_LOSE_CODE
            PlayerData specialLose = null;
            for (int i = 0; i < gameData.size(); i++) {
                PlayerData p = gameData.getPlayer(i);

                if (SPECIAL_LOSE_CODE.equals(p.getCard())) {
                    specialLose = p;
                    break;
                }
            }

            // Find losers
            if (specialLose != null) {
                specialLose.setLives(specialLose.getLives()-1);
            }
            else if (leasts.size() == 1) {
                // The player with the worst card
                PlayerData p = gameData.getPlayerByName(leasts.getLast());
                
                // Special immunity
                if (! (p.getCard().equals(SPECIAL_LOSE_CODE) || p.getCard().equals(SPECIAL_SAVE_CODE) )) {
                    p.setLives(p.getLives()-1);
                }
            }
            else if (leasts.size() == 2) {
                // Players with the highest card
                for (String nick : highests) {
                    PlayerData p = gameData.getPlayerByName(nick);
                    // Special immunity
                    if (! (p.getCard().equals(SPECIAL_LOSE_CODE) || p.getCard().equals(SPECIAL_SAVE_CODE) )) {
                        p.setLives(p.getLives()-1);
                    }
                }
            }
            else if (leasts.size() == 3) {
                // Only the dealer (which is always the last in turn)
                PlayerData p = gameData.getPlayerByName(turnNicks.getLast());
                // Special immunity
                if (! (p.getCard().equals(SPECIAL_LOSE_CODE) || p.getCard().equals(SPECIAL_SAVE_CODE) )) {
                    p.setLives(p.getLives()-1);
                }
            }
            else if (leasts.size() == 4) {
                // Everybody but players with the worst card
                for (String nick :turnNicks) {
                    if (leasts.contains(nick)) continue;

                    PlayerData p = gameData.getPlayerByName(nick);
                    // Special immunity
                    if (! (p.getCard().equals(SPECIAL_LOSE_CODE) || p.getCard().equals(SPECIAL_SAVE_CODE) )) {
                        p.setLives(p.getLives()-1);
                    }
                }
            }
            
            // Update clients
            broadcastPublicGameData();

            // Wait for new round
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                System.err.println("Error sleeping: " + e.getMessage());
            }
        }
    }
}
