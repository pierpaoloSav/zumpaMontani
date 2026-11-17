package Model;

import java.io.Serializable;
import java.util.ArrayList;

public class GameData implements Serializable {
    private final ArrayList<PlayerData> players;

    public GameData(ArrayList<PlayerData> players) {
        this.players = players;
    }

    // GameData
    public GameData(GameData other) {
        this.players = new ArrayList<>(other.players.size());
        for (PlayerData p : other.players) {
            this.players.add(new PlayerData(p));
        }
    }
    
    public GameData() {
        this.players = new ArrayList<>();
    }

    public void addPlayer(PlayerData player) {
        players.add(player);
    }

    public PlayerData removePlayer(int index) {
        return players.remove(index);
    }

    public PlayerData getPlayer(int index) {
        return players.get(index);
    }

    public PlayerData getPlayerByName(String name) {
        for (PlayerData p : players) {
            if (p.getNick().equals(name)) {
                return p;
            }
        }
        return null;
    }

    public int size() {
        return players.size();
    }

    // GameData
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("GameData{players=[\n");
        for (int i = 0; i < players.size(); i++) {
            sb.append("  ").append(i).append(": ").append(players.get(i)).append("\n");
        }
        return sb.append("]}").toString();
    }
}