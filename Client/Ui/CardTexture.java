package Client.Ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class CardTexture extends JPanel {

    private static BufferedImage spriteSheet;

    private String cardCode = "000";
    private BufferedImage cardImage;

    private static final double REFERENCE_WIDTH = 1024.0;
    private static final double REFERENCE_HEIGHT = 825.6;

    private static final int[] X = {
        15, 107, 200, 293, 385,
        478, 571, 664, 756, 849
    };

    private static final int[] Y = {
        9, 173, 337, 500
    };

    private static final int CARD_WIDTH = 72;
    private static final int CARD_HEIGHT = 145;

    private static final Rectangle BACK =
        new Rectangle(940, 664, 73, 143);

    private static final int CORNER_RADIUS = 9;

    static {
        try {
            URL resource =
                CardTexture.class.getResource("resources/cardsheet.png");

            if (resource != null) {
                spriteSheet = ImageIO.read(resource);
            } else {
                spriteSheet = ImageIO.read(
                    new File("resources/cardsheet.png")
                );
            }

            if (spriteSheet == null) {
                throw new IOException("Invalid image format");
            }

            System.out.println(
                "Spritesheet loaded: "
                + spriteSheet.getWidth() + "x"
                + spriteSheet.getHeight()
            );

        } catch (IOException e) {
            throw new RuntimeException(
                "Unable to load cardsheet.png", e
            );
        }
    }

    public CardTexture() {
        this("000");
    }

    public CardTexture(String cardCode) {
        setOpaque(false);
        setCardCode(cardCode);
    }

    private int scaleX(int value) {
        return (int) Math.round(
            value * spriteSheet.getWidth() / REFERENCE_WIDTH
        );
    }

    private int scaleY(int value) {
        return (int) Math.round(
            value * spriteSheet.getHeight() / REFERENCE_HEIGHT
        );
    }

    private Rectangle getCardBounds(String code) {

        if (code.equals("000")) {
            return new Rectangle(
                scaleX(BACK.x),
                scaleY(BACK.y),
                scaleX(BACK.width),
                scaleY(BACK.height)
            );
        }

        if (!code.matches("[BCDS](0[1-9]|10)")) {
            throw new IllegalArgumentException(
                "Invalid card code: " + code
            );
        }

        int row;

        switch (code.charAt(0)) {
            case 'B': row = 0; break;
            case 'C': row = 1; break;
            case 'D': row = 2; break;
            case 'S': row = 3; break;
            default:
                throw new IllegalArgumentException("Invalid suit");
        }

        int column =
            Integer.parseInt(code.substring(1)) - 1;

        return new Rectangle(
            scaleX(X[column]),
            scaleY(Y[row]),
            scaleX(CARD_WIDTH),
            scaleY(CARD_HEIGHT)
        );
    }

    public void setCardCode(String cardCode) {

        if (cardCode == null) {
            throw new IllegalArgumentException(
                "Card code cannot be null"
            );
        }

        cardCode = cardCode.toUpperCase();

        Rectangle bounds = getCardBounds(cardCode);

        if (bounds.x < 0 || bounds.y < 0 ||
            bounds.width <= 0 || bounds.height <= 0 ||
            bounds.x + bounds.width > spriteSheet.getWidth() ||
            bounds.y + bounds.height > spriteSheet.getHeight()) {

            throw new IllegalArgumentException(
                "Card outside spritesheet: " + cardCode
            );
        }

        this.cardCode = cardCode;

        cardImage = spriteSheet.getSubimage(
            bounds.x,
            bounds.y,
            bounds.width,
            bounds.height
        );

        repaint();
    }

    public String getCardCode() {
        return cardCode;
    }

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        if (cardImage == null) return;

        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );

        g2.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BICUBIC
        );

        int w = getWidth();
        int h = getHeight();

        if (w <= 0 || h <= 0) {
            g2.dispose();
            return;
        }

        int padding = 1;

        int availableWidth = w - 2 - padding * 2;
        int availableHeight = h - 2 - padding * 2;

        Shape oldClip = g2.getClip();

        g2.clip(new RoundRectangle2D.Double(
            padding,
            padding,
            availableWidth,
            availableHeight,
            CORNER_RADIUS - 2,
            CORNER_RADIUS - 2
        ));

        // Mantiene le proporzioni originali
        double scale = Math.min(
            availableWidth / (double) cardImage.getWidth(),
            availableHeight / (double) cardImage.getHeight()
        );

        int imageWidth = (int) Math.round(
            cardImage.getWidth() * scale
        );

        int imageHeight = (int) Math.round(
            cardImage.getHeight() * scale
        );

        int x = (w - imageWidth) / 2;
        int y = (h - imageHeight) / 2;

        g2.drawImage(
            cardImage,
            x, y,
            imageWidth, imageHeight,
            null
        );

        g2.setClip(oldClip);

        g2.dispose();
    }
}
