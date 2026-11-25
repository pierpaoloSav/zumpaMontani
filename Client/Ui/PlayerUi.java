package Client.Ui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

public class PlayerUi extends JPanel {

    public String nickname;
    public boolean isDealer;
    public int lives;
    public boolean myTurn;

    private CardTexture cardTexture;

    private JLabel nicknameLabel;
    private JLabel dealerLabel;
    private JLabel livesLabel;
    private JLabel turnLabel;

    private JPanel tag;

    private static final int WIDTH = 140;
    private static final int HEIGHT = 132;

    public PlayerUi(
        String nickname,
        boolean isDealer,
        int lives,
        boolean myTurn
    ) {
        this(nickname, isDealer, lives, myTurn, "000");
    }

    public PlayerUi(
        String nickname,
        boolean isDealer,
        int lives,
        boolean myTurn,
        String cardCode
    ) {
        this.nickname = nickname;
        this.isDealer = isDealer;
        this.lives = lives;
        this.myTurn = myTurn;

        setLayout(null);
        setOpaque(false);

        // Card
        cardTexture = new CardTexture(cardCode);
        cardTexture.setBounds(40, 0, 60, 86);
        add(cardTexture);

        // Player tag
        tag = new JPanel(new BorderLayout(4, 3));

        tag.setBackground(new Color(35, 38, 48));

        tag.setBorder(new CompoundBorder(
            new LineBorder(new Color(75, 80, 95), 2, true),
            new EmptyBorder(4, 7, 4, 7)
        ));

        tag.setBounds(0, 88, WIDTH, 42);

        // Nickname
        nicknameLabel = new JLabel(nickname);
        nicknameLabel.setForeground(Color.WHITE);
        nicknameLabel.setFont(
            new Font("Arial", Font.BOLD, 13)
        );

        // Dealer
        dealerLabel = new JLabel();
        dealerLabel.setForeground(new Color(255, 200, 70));
        dealerLabel.setFont(
            new Font("Arial", Font.BOLD, 11)
        );

        // Lives
        livesLabel = new JLabel();
        livesLabel.setForeground(new Color(255, 75, 90));
        livesLabel.setFont(
            new Font("Arial", Font.BOLD, 12)
        );

        // Turn
        turnLabel = new JLabel();
        turnLabel.setForeground(new Color(100, 230, 150));
        turnLabel.setFont(
            new Font("Arial", Font.BOLD, 10)
        );

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        header.add(nicknameLabel, BorderLayout.WEST);
        header.add(dealerLabel, BorderLayout.EAST);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);

        footer.add(livesLabel, BorderLayout.WEST);
        footer.add(turnLabel, BorderLayout.EAST);

        tag.add(header, BorderLayout.NORTH);
        tag.add(footer, BorderLayout.SOUTH);

        add(tag);

        updateDisplay();
    }

    public void updateDisplay() {

        nicknameLabel.setText(nickname);
        dealerLabel.setText(isDealer ? "MAZZIERE" : "");

        StringBuilder hearts = new StringBuilder();

        for (int i = 0; i < lives; i++) {
            hearts.append("♥ ");
        }

        livesLabel.setText(hearts.toString());
        turnLabel.setText(myTurn ? "TUO TURNO" : "");

        tag.setBorder(new CompoundBorder(
            new LineBorder(
                myTurn
                    ? new Color(80, 230, 140)
                    : new Color(75, 80, 95),
                myTurn ? 2 : 1,
                true
            ),
            new EmptyBorder(4, 7, 4, 7)
        ));

        repaint();
    }

    public void setCardCode(String code) {
        cardTexture.setCardCode(code);
    }

    public String getCardCode() {
        return cardTexture.getCardCode();
    }

    public void setLives(int lives) {
        this.lives = Math.max(0, lives);
        updateDisplay();
    }

    public void setMyTurn(boolean myTurn) {
        this.myTurn = myTurn;
        updateDisplay();
    }

    public void setDealer(boolean isDealer) {
        this.isDealer = isDealer;
        updateDisplay();
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
        updateDisplay();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(WIDTH, HEIGHT);
    }
}
