package br.com.snakegame;

import javax.swing.*;

public class Main extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main());
    }

    private Main() {
        add(new GameScreen(new KeyReaderAdapter()));
        setTitle("Jogo da Cobrinha");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        pack();
        setVisible(true);
        setLocationRelativeTo(null);
    }
}