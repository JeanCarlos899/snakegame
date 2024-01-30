package br.com.snakegame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;
import java.util.concurrent.Semaphore;

public class GameScreen extends JPanel implements ActionListener {
    // Configurações do jogo
    private static final int SCREEN_WIDTH = 1300;
    private static final int SCREEN_HEIGHT = 750;
    private static final int BLOCK_SIZE = 20;
    private static final int UNITS = SCREEN_WIDTH * SCREEN_HEIGHT / (BLOCK_SIZE * BLOCK_SIZE);
    private static final int INTERVAL = 200;
    private static final String FONT_NAME = "Arial";

    // Corpo da cobra
    private final int[] axisX = new int[UNITS];
    private final int[] axisY = new int[UNITS];
    private int snakeLength = 2;

    // Blocos
    private int blocksEaten;
    private int blockX;
    private int blockY;

    // Estado do jogo
    private boolean isRunning = false;
    private Timer timer;
    private Random random;
    private KeyReaderAdapter keyReaderAdapter;

    // Direção da cobra
    private char direction = 'R';

    // Semáforo para controlar o acesso à área crítica (matriz de coordenadas da
    // cobra)
    private Semaphore semaphore = new Semaphore(1);

    public GameScreen(KeyReaderAdapter keyReaderAdapter) {
        this.keyReaderAdapter = keyReaderAdapter;
        random = new Random();
        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        setBackground(Color.WHITE);
        setFocusable(true);
        addKeyListener(keyReaderAdapter);
        startGame();

        // Esta thread é responsável por atualizar continuamente a tela do jogo
        new Thread(this::updateScreen).start();
    }

    /**
     * Método responsável por atualizar continuamente a tela do jogo em uma thread
     * separada.
     * Este método é executado em um loop infinito e é responsável por redesenhar a
     * tela do jogo
     * em intervalos regulares, garantindo uma experiência de jogo suave e
     * responsiva.
     * Ele adquire o semáforo para acessar a área crítica (matriz de coordenadas da
     * cobra),
     * redesenha a tela imediatamente e, em seguida, libera o semáforo antes de
     * aguardar um curto
     * intervalo de tempo. Isso permite que outras operações tenham a oportunidade
     * de serem executadas
     * durante esse intervalo, melhorando o desempenho e a responsividade do jogo.
     */
    private void updateScreen() {
        while (true) {
            try {
                // Bloqueia o acesso à área crítica
                semaphore.acquire();
                // Redesenha a tela imediatamente
                repaint();
                // Libera o acesso à área crítica
                semaphore.release();
                // Aguarda um curto intervalo de tempo
                Thread.sleep(5); 
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void startGame() {
        createBlock();
        isRunning = true;
        timer = new Timer(INTERVAL, this);
        timer.start();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawScreen(g);
    }

    public void drawScreen(Graphics g) {
        try {
            // Bloqueia o acesso à área crítica
            semaphore.acquire();
            if (isRunning) {
                setBackground(Color.BLACK);
                super.paintComponent(g);
                g.setColor(Color.BLUE);
                g.fillOval(blockX, blockY, BLOCK_SIZE, BLOCK_SIZE);

                for (int i = 0; i < snakeLength; i++) {
                    if (i == 0) {
                        g.setColor(Color.red);
                        g.fillRect(axisX[0], axisY[0], BLOCK_SIZE, BLOCK_SIZE);
                    } else {
                        g.setColor(Color.red);
                        g.fillRect(axisX[i], axisY[i], BLOCK_SIZE, BLOCK_SIZE);
                    }
                }
                g.setColor(Color.white);
                g.setFont(new Font(FONT_NAME, Font.BOLD, 20));
                g.drawString("PONTOS: " + blocksEaten,
                        10, g.getFont().getSize()); // Alinhado à esquerda
            } else {
                gameOver(g);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // Libera o acesso à área crítica
            semaphore.release();
        }
    }

    private void createBlock() {
        blockX = random.nextInt(SCREEN_WIDTH / BLOCK_SIZE) * BLOCK_SIZE;
        blockY = random.nextInt(SCREEN_HEIGHT / BLOCK_SIZE) * BLOCK_SIZE;
    }

    public void gameOver(Graphics g) {
        g.setColor(Color.red);
        g.setFont(new Font(FONT_NAME, Font.BOLD, 20));
        FontMetrics scoreFontMetrics = getFontMetrics(g.getFont());
        g.drawString("PONTOS: " + blocksEaten,
                (SCREEN_WIDTH - scoreFontMetrics.stringWidth("PONTOS: " + blocksEaten)) / 2, g.getFont().getSize());
        g.setColor(Color.red);
        g.setFont(new Font(FONT_NAME, Font.BOLD, 40));
        FontMetrics endGameFontMetrics = getFontMetrics(g.getFont());
        g.drawString("Fim de jogo", (SCREEN_WIDTH - endGameFontMetrics.stringWidth("Fim de jogo")) / 2,
                SCREEN_HEIGHT / 2);
    }

    /**
     * Este método é invocado quando uma ação ocorre, neste caso, ele lida com a
     * lógica do jogo.
     * Ele atualiza a direção da cobra com base na entrada do KeyReaderAdapter,
     * move a cobra, verifica se ela alcança um bloco e valida se atinge quaisquer
     * limites.
     *
     * @param e O objeto ActionEvent que representa a ação que ocorreu.
     */
    public void actionPerformed(ActionEvent e) {
        if (isRunning) {
            // Esta thread é responsável por executar a lógica do jogo
            new Thread(() -> {
                try {
                    // Bloqueia o acesso à área crítica
                    semaphore.acquire();
                    // Atualiza a direção a partir do KeyReaderAdapter
                    direction = keyReaderAdapter.getDirection();
                    // Move a cobra
                    move();
                    // Verifica se a cobra alcança um bloco
                    reachBlock();
                    // Valida se a cobra atinge quaisquer limites
                    validateBoundaries(); 
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                } finally {
                    // Libera o acesso à área crítica
                    semaphore.release(); 
                }
            }).start();
        }
    }

    private void move() {
        for (int i = snakeLength; i > 0; i--) {
            axisX[i] = axisX[i - 1];
            axisY[i] = axisY[i - 1];
        }

        switch (direction) {
            case 'U':
                axisY[0] = axisY[0] - BLOCK_SIZE;
                break;
            case 'D':
                axisY[0] = axisY[0] + BLOCK_SIZE;
                break;
            case 'L':
                axisX[0] = axisX[0] - BLOCK_SIZE;
                break;
            case 'R':
                axisX[0] = axisX[0] + BLOCK_SIZE;
                break;
            default:
                break;
        }
    }

    private void reachBlock() {
        if (axisX[0] == blockX && axisY[0] == blockY) {
            snakeLength++;
            blocksEaten++;

            // Ajusta o intervalo do timer

            // Aumenta a velocidade do jogo sempre que um bloco é alcançado
            int newInterval = INTERVAL - (blocksEaten * 10);

            // Limita a velocidade máxima do jogo
            timer.setDelay(Math.max(newInterval, 50));

            // Cria um novo bloco
            createBlock();
        }
    }

    private void validateBoundaries() {
        // Caso a cobra toque em si mesma, o jogo termina
        for (int i = snakeLength; i > 0; i--) {
            if (axisX[0] == axisX[i] && axisY[0] == axisY[i]) {
                isRunning = false;
                break;
            }
        }

        // Caso a cobra toque em qualquer uma das paredes, o jogo termina
        if (axisX[0] < 0 || axisX[0] > SCREEN_WIDTH) {
            isRunning = false;
        }

        if (axisY[0] < 0 || axisY[0] > SCREEN_HEIGHT) {
            isRunning = false;
        }

        // Caso o jogo tenha terminado, para o timer
        if (!isRunning) {
            timer.stop();
        }
    }
}
