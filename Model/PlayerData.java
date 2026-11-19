package Model;

import java.io.Serializable;

public class PlayerData implements Serializable {
    private final String nick;
    private int lives;
    private String card;
    private boolean choice;
    private boolean dealer;
    private boolean myTurn;

    public PlayerData(String nick, int lives) {
        this.nick = nick;
        this.lives = lives;
        this.card = "000";
        this.choice = false;
        this.dealer = false;
        this.myTurn = false;
    }

    public PlayerData(PlayerData other) {
        this.nick = other.nick;
        this.lives = other.lives;
        this.card = other.card;
        this.choice = other.choice;
        this.dealer = other.dealer;
        this.myTurn = other.myTurn;
    }

    public void setCard(String card) {
        this.card = card;
    }

    public void setChoice(boolean choice) {
        this.choice = choice;
    }

    public void setDealer(boolean dealer) {
        this.dealer = dealer;
    }

    public void setLives(int lives) {
        this.lives = lives;
    }

    public void setMyTurn(boolean myTurn) {
        this.myTurn = myTurn;
    }

    public String getNick() {
        return nick;
    }

    public String getCard() {
        return card;
    }

    public int getCardValue() {
        return Integer.parseInt( card.substring(1) );
    }

    public int getLives() {
        return lives;
    }

    public boolean isChoice() {
        return choice;
    }

    public boolean isDealer() {
        return dealer;
    }

    public boolean isMyTurn() {
        return myTurn;
    }

    // PlayerData
    @Override
    public String toString() {
        return "PlayerData{nickname=" + nick +
            ", lives=" + lives +
            ", card=" + card +
            ", choice=" + choice +
            ", dealer=" + dealer +
            ", myTurn=" + myTurn + "}";
    }
}
