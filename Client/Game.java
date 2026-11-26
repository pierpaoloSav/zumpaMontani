package Client;

import javax.swing.*;
import java.awt.*;

import Client.Ui.GameUi;
import Model.GameData;

//TODO ui input with a controller

public class Game {
    public static void main(String[] args) {
        Client comm = new Client("localhost", 6060);

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Zumpa Motani");
            GameUi game = new GameUi();

            // Config window
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(game);
            frame.setSize(1600, 1000);
            frame.setMinimumSize(new Dimension(1200, 800));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // Load data to UI
            //? eliminare il while true e mettere che aspetta un cambiamento
            Thread gameUpdateThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {

                    GameData data = comm.getGameData();

                    if (data != null) {
                        SwingUtilities.invokeLater(() -> {
                            game.setGameData(data);
                        });
                    }

                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });

            gameUpdateThread.start();
        });

        // Run communication
        comm.run();
    }
}
