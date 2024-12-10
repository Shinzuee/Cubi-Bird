import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;

public class CubibirdGame extends JPanel implements KeyListener, ActionListener {
    private static final int GROUND_HEIGHT = 70;
    private static final int HELICOPTER_SIZE = 70;
    private static final int OBSTACLE_WIDTH = 80; // Adjusted width
    private static final int OBSTACLE_GAP = 270;

    private int width;
    private int height;

    private int helicopterX;
    private int helicopterY;
    private int velocityY = 0;
    private int velocityX = 0;
    private int score = 0;
    private int highestScore = 0;
    private boolean gameOver = false;
    private int ticks = 0;
    private int obstacleSpeed = 5;
    private int countdownTimer = 5;
    private boolean gameStarted = false;
    private boolean showInstructions = true;
    private Timer countdown;
    private List<Obstacle> obstacles;
    private List<HighScore> highScores;
    private JButton startButton;
    private Image backgroundImageDay;
    private Image backgroundImageNight;
    private Image helicopterImage;
    private Image obstacleImage;
    private String playerName;

    public CubibirdGame() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        width = screenSize.width;
        height = screenSize.height;

        JFrame frame = new JFrame("Cubibird Game");
        frame.setSize(width, height);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setLayout(null);

        this.setBounds(0, 0, width, height);
        frame.add(this);

        // Load images
        try {
            backgroundImageDay = ImageIO.read(getClass().getResource("flappy.jpg"));
            backgroundImageNight = ImageIO.read(getClass().getResource("bbbg.jpg")); // Assuming you have a night background image
            helicopterImage = ImageIO.read(getClass().getResource("cool.png"));
            obstacleImage = ImageIO.read(getClass().getResource("STID.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Initialize start button
        startButton = new JButton("RESTART");
        startButton.setFont(new Font("product sans", Font.BOLD, 18));
        startButton.setBounds((width - 150) / 2, height / 2 + 60, 150, 50);
        startButton.setBackground(Color.DARK_GRAY);
        startButton.setForeground(Color.WHITE);
        startButton.setFocusPainted(false);

        // Create a border with rounded corners
        Border lineBorder = BorderFactory.createLineBorder(Color.WHITE, 3, true);
        startButton.setBorder(lineBorder);

        startButton.addActionListener(this);
        startButton.setFocusable(false);
        startButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                startButton.setBackground(Color.GREEN);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                startButton.setBackground(Color.DARK_GRAY);
            }
        });
        add(startButton);
        startButton.setVisible(false);

        this.addKeyListener(this);
        this.setFocusable(true);

        frame.setVisible(true);

        // Initialize countdown timer
        countdown = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (countdownTimer > 0) {
                    countdownTimer--;
                    repaint();
                } else {
                    countdown.stop();
                    startGameLoop();
                }
            }
        });

        obstacles = new ArrayList<>();
        highScores = new ArrayList<>();
        startNewGame();
    }

    private void startGameLoop() {
        gameStarted = true;
        showInstructions = false;
        Thread gameLoop = new Thread(() -> {
            while (gameStarted) {
                update();
                repaint();
                try {
                    TimeUnit.MILLISECONDS.sleep(23);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        gameLoop.start();
    }

    private void update() {
        if (!gameOver) {
            helicopterY += velocityY;
            if (helicopterY > height - HELICOPTER_SIZE - GROUND_HEIGHT) {
                helicopterY = height - HELICOPTER_SIZE - GROUND_HEIGHT;
                velocityY = 0;
                gameOver = true;
            } else if (helicopterY < GROUND_HEIGHT) {
                helicopterY = GROUND_HEIGHT;
                velocityY = 0;
                gameOver = true;
            } else {
                velocityY += 1;
            }

            helicopterX += velocityX;
            if (helicopterX < 0) {
                helicopterX = 0;
            } else if (helicopterX > width - HELICOPTER_SIZE) {
                helicopterX = width - HELICOPTER_SIZE;
            }

            synchronized (obstacles) {
                for (int i = 0; i < obstacles.size(); i++) {
                    Obstacle obstacle = obstacles.get(i);
                    obstacle.x -= obstacleSpeed;

                    if (obstacle.x + OBSTACLE_WIDTH < 0) {
                        obstacles.remove(obstacle);
                        score++;
                        i--;
                    }
                }

                if (ticks % 60 == 0) {
                    int height = (int) (Math.random() * (this.height - 2 * GROUND_HEIGHT - OBSTACLE_GAP));
                    int topHeight = height + GROUND_HEIGHT;
                    int bottomY = height + OBSTACLE_GAP + GROUND_HEIGHT;

                    obstacles.add(new Obstacle(width, GROUND_HEIGHT, OBSTACLE_WIDTH, topHeight));
                    obstacles.add(new Obstacle(width, bottomY, OBSTACLE_WIDTH, this.height - bottomY - GROUND_HEIGHT));
                }
            }

            Rectangle helicopterBounds = new Rectangle(helicopterX + 12, helicopterY + 12, HELICOPTER_SIZE -55, HELICOPTER_SIZE - 55); // Adjusting the helicopter hitbox
            synchronized (obstacles) {
                for (Obstacle obstacle : obstacles) {
                    Rectangle obstacleBounds = new Rectangle(obstacle.x, obstacle.y, OBSTACLE_WIDTH, obstacle.height); // Adjusting obstacle hitbox
                    if (helicopterBounds.intersects(obstacleBounds)) {
                        gameOver = true;
                        if (score > highestScore) {
                            highestScore = score;
                        }
                        updateHighScores();
                        showLeaderboard();
                    }
                }
            }

            ticks++;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (score < 20) {
            g.drawImage(backgroundImageDay, 0, 0, getWidth(), getHeight(), this);
        } else {
            g.drawImage(backgroundImageNight, 0, 0, getWidth(), getHeight(), this);
        }
        g.setColor(Color.GRAY);
        g.fillRect(0, height - GROUND_HEIGHT, width, GROUND_HEIGHT);
        g.fillRect(0, 0, width, GROUND_HEIGHT);
        g.drawImage(helicopterImage, helicopterX, helicopterY, HELICOPTER_SIZE, HELICOPTER_SIZE, this);
        synchronized (obstacles) {
            for (Obstacle obstacle : obstacles) {
                g.drawImage(obstacleImage, obstacle.x, obstacle.y, OBSTACLE_WIDTH, obstacle.height, this); // Adjusting obstacle width
            }
        }
        g.setColor(Color.BLACK);
        g.setFont(new Font("product sans", Font.BOLD, 20));
        g.drawString("Score: " + score, 20, 30);
        String highestScoreText = "Highest Score: " + highestScore;
        int highestScoreWidth = g.getFontMetrics().stringWidth(highestScoreText);
        g.drawString(highestScoreText, (width - highestScoreWidth) / 2, 30);

        if (showInstructions) {
            Font instructionsFont = new Font("product sans", Font.BOLD, 30);
            g.setFont(instructionsFont);
            String instructions = "Press SPACE to Start\nUse SPACE to Control the Bird\nAvoid Obstacles!";
            int instructionsWidth = g.getFontMetrics().stringWidth(instructions);
            int instructionsHeight = g.getFontMetrics().getHeight();
            int instructionsX = (width - instructionsWidth) / 2;
            int instructionsY = (height - instructionsHeight) / 2;
            g.drawString("Press SPACE to Start", instructionsX + 100, instructionsY - 50);
            g.drawString("Use SPACE to Control the Bird", instructionsX + 300, instructionsY - 10);
            g.drawString("Avoid Obstacles!", instructionsX + 420, instructionsY + 40);
        }

        if (countdownTimer > 0 && !gameStarted) {
            Font countdownFont = new Font("product sans", Font.BOLD, 100);
            g.setFont(countdownFont);
            String countdownText = String.valueOf(countdownTimer);
            int countdownWidth = g.getFontMetrics().stringWidth(countdownText);
            g.drawString(countdownText, (width - countdownWidth) / 2, height / 2);
        }

        if (gameOver) {
            Font gameOverFont = new Font("Comic Sans MS", Font.BOLD, 50);
            g.setColor(Color.red);
            g.setFont(gameOverFont);
            String gameOverText = "Game Over!";
            int gameOverWidth = g.getFontMetrics().stringWidth(gameOverText);
            g.drawString(gameOverText, (width - gameOverWidth) / 2, height / 2 - 50);

            startButton.setVisible(true);
        }
    }

    private void startNewGame() {
        helicopterX = 100;
        helicopterY = height / 2;
        velocityY = 0;
        velocityX = 0;
        score = 0;
        gameOver = false;
        ticks = 0;
        obstacles.clear();
        countdownTimer = 3;
        gameStarted = false;
        showInstructions = true;
        startButton.setVisible(false);
        playerName = JOptionPane.showInputDialog(this, "Enter your name:", "Player");

        countdown.start();
    }

    private void updateHighScores() {
        highScores.add(new HighScore(playerName, score));
        highScores.sort(Comparator.comparingInt(HighScore::getScore).reversed());
        if (highScores.size() > 5) {
            highScores.remove(highScores.size() - 1);
        }
    }

    private void showLeaderboard() {
        JFrame leaderboardFrame = new JFrame("Leaderboard");
        leaderboardFrame.setSize(400, 300);
        leaderboardFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        String[] columnNames = {"Rank", "Name", "Score"};
        Object[][] data = new Object[highScores.size()][3];

        for (int i = 0; i < highScores.size(); i++) {
            HighScore highScore = highScores.get(i);
            data[i][0] = (i + 1);
            data[i][1] = highScore.name;
            data[i][2] = highScore.score;
        }

        JTable table = new JTable(new DefaultTableModel(data, columnNames));
        JScrollPane scrollPane = new JScrollPane(table);
        leaderboardFrame.add(scrollPane);

        leaderboardFrame.setVisible(true);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            if (!gameStarted) {
                countdown.start();
            }
            if (gameOver) {
                startNewGame();
            } else {
                velocityY = -10;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            velocityY = 0;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == startButton) {
            startNewGame();
        }
    }

    public static void main(String[] args) {
        new CubibirdGame();
    }

    private static class Obstacle extends Rectangle {
        public int height;

        public Obstacle(int x, int y, int width, int height) {
            super(x, y, width, height);
            this.height = height;
        }
    }

    private static class HighScore {
        String name;
        int score;

        public HighScore(String name, int score) {
            this.name = name;
            this.score = score;
        }

        public int getScore() {
            return score;
        }
    }
}
