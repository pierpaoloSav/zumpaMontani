package Client.Ui;

//! UI DRAFT
//TODO project and connect to client
//TODO implement and do a controller for Ui input

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import Model.*;

public class GameUi extends JPanel {

    private static final int MAX_PLAYERS = 30;
    private static final int MARGIN = 25;

    private final List<PlayerUi> players = new ArrayList<>();

    private final Ellipse2D.Double tableEllipse =
        new Ellipse2D.Double();

    private final Color tableColor =
        new Color(35, 100, 75);

    private final Color borderColor =
        new Color(210, 180, 110);

    public GameUi() {
        setLayout(null);
        setBackground(new Color(22, 24, 32));
        setOpaque(true);
    }

    public void setGameData(GameData gameData) {
        if (gameData == null) {
            throw new IllegalArgumentException(
                "GameData cannot be null"
            );
        }

        clearPlayers();

        if (gameData.size() > MAX_PLAYERS) {
            throw new IllegalArgumentException(
                "Maximum 30 players allowed"
            );
        }

        for (int i = 0; i < gameData.size(); i++) {
            PlayerData data = gameData.getPlayer(i);

            PlayerUi player = new PlayerUi(
                data.getNick(),
                data.isDealer(),
                data.getLives(),
                data.isMyTurn(),
                data.getCard()
            );

            //player.setChoice(data.isChoice());

            addPlayer(player);
        }

        revalidate();
        repaint();
    }

    public void addPlayer(PlayerUi player) {

        if (player == null) {
            throw new IllegalArgumentException(
                "Player cannot be null"
            );
        }

        if (players.size() >= MAX_PLAYERS) {
            throw new IllegalStateException(
                "Maximum 30 players reached"
            );
        }

        if (players.contains(player)) {
            return;
        }

        players.add(player);
        add(player);

        revalidate();
        repaint();
    }

    public void removePlayer(PlayerUi player) {

        if (players.remove(player)) {
            remove(player);
            revalidate();
            repaint();
        }
    }

    public void clearPlayers() {
        players.clear();
        removeAll();

        revalidate();
        repaint();
    }

    public List<PlayerUi> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public int getPlayerCount() {
        return players.size();
    }

    public void setPlayerCard(int index, String cardCode) {
        players.get(index).setCardCode(cardCode);
        repaint();
    }

    public void setPlayerTurn(int index) {
        for (int i = 0; i < players.size(); i++) {
            players.get(i).setMyTurn(i == index);
        }
    }

    @Override
    public void doLayout() {

        int width = getWidth();
        int height = getHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        double centerX = width / 2.0;
        double centerY = height / 2.0;

        // Find the largest PlayerUi dimensions
        int maxWidth = 0;
        int maxHeight = 0;

        for (PlayerUi player : players) {
            Dimension size = player.getPreferredSize();

            maxWidth = Math.max(maxWidth, size.width);
            maxHeight = Math.max(maxHeight, size.height);
        }

        // Calculate safe ellipse radii
        double radiusX = Math.max(
            0,
            width / 2.0 - maxWidth / 2.0 - MARGIN
        );

        double radiusY = Math.max(
            0,
            height / 2.0 - maxHeight / 2.0 - MARGIN
        );

        tableEllipse.setFrame(
            centerX - radiusX,
            centerY - radiusY,
            radiusX * 2,
            radiusY * 2
        );

        int count = players.size();

        if (count == 0) {
            return;
        }

        double startAngle = -Math.PI / 2.0;
        double angleStep = 2.0 * Math.PI / count;

        for (int i = 0; i < count; i++) {

            double angle = startAngle + i * angleStep;

            // Position on the ellipse
            double pointX =
                centerX + radiusX * Math.cos(angle);

            double pointY =
                centerY + radiusY * Math.sin(angle);

            PlayerUi player = players.get(i);

            Dimension size = player.getPreferredSize();

            // Center the complete PlayerUi on the ellipse
            int x = (int) Math.round(pointX - size.width / 2.0);
            int y = (int) Math.round(pointY - size.height / 2.0);

            player.setBounds(
                x,
                y,
                size.width,
                size.height
            );
        }
    }

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );

        // Table
        g2.setColor(tableColor);
        g2.fill(tableEllipse);

        // Outer border
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(4f));
        g2.draw(tableEllipse);

        // Inner border
        double inset = 12;

        Ellipse2D.Double inner = new Ellipse2D.Double(
            tableEllipse.x + inset,
            tableEllipse.y + inset,
            Math.max(0, tableEllipse.width - inset * 2),
            Math.max(0, tableEllipse.height - inset * 2)
        );

        g2.setColor(new Color(255, 255, 255, 40));
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(inner);

        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(1100, 800);
    }
}
