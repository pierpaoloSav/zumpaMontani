package Model;

import java.io.Serializable;

public class RoomData implements Serializable {
    private String roomName;
    private String pin;
    private int nLives;
    private int maxPlayers;
    private int minPlayers;
    private int actionTimer;
    private int timeoutKick;

    public RoomData() {}

    //TODO put the limits

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }       

    public void setnLives(int nLives) {
        this.nLives = nLives;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public void setMinPlayers(int minPlayers) {
        this.minPlayers = minPlayers;
    }

    public void setActionTimer(int actionTimer) {
        this.actionTimer = actionTimer;
    }

    public void setTimeoutKick(int timeoutKick) {
        this.timeoutKick = timeoutKick;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getPin() {
        return pin;
    }

    public int getnLives() {
        return nLives;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }   

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getActionTimer() {
        return actionTimer;
    }

    public int getTimeoutKick() {
        return timeoutKick;
    }
}
