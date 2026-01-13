import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Random;
import javax.sound.sampled.*;
import javax.swing.*;

public class BlackJack {
    private class Card {
        String value;
        String type;

        Card(String value, String type) {
            this.value = value;
            this.type = type;
        }

        @Override
        public String toString() {
            return value + "-" + type;
        }

        public int getValue() {
            if ("AJQK".contains(value)) { // A, J, Q, K
                if (value.equals("A")) {
                    return 11;
                }
                return 10;
            }
            return Integer.parseInt(value); // 2-10
        }

        public boolean isAce() {
            return value.equals("A");
        }

        public String getImagePath() {
            return "./Card/" + toString() + ".png";
        }
    }

    private ArrayList<Card> deck;
    private Random random = new Random(); // Shuffle deck

    // Dealer
    private Card hiddenCard;
    private ArrayList<Card> dealerHand;
    private int dealerSum;
    private int dealerAceCount;

    // Player
    private ArrayList<Card> playerHand;
    private int playerSum;
    private int playerAceCount;

    // Window
    private int boardWidth = 600;
    private int boardHeight = boardWidth;
    private int cardWidth = 110; // Ratio should be 1/1.4
    private int cardHeight = 154;

    // Animation variables
    private int animationDelay = 10; // 10 ms delay for smoothness
    private int animationStep = 5;   // Movement step in pixels
    private Timer cardAnimationTimer;

    // Positions for animated card drawing
    private int cardX = 0;
    private int cardY = 0;
    private boolean isAnimating = false;

    // Sound Effects
    private Clip hitSoundClip;
    private Clip staySoundClip;
    private Clip currentPlayingSound; // Track currently playing sound

    private JFrame frame = new JFrame("Black Jack");
    private JPanel gamePanel = new JPanel() {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            try {
                Image backgroundImg = new ImageIcon(getClass().getResource("/Card/Back.jpg")).getImage();
                g.drawImage(backgroundImg, 0, 0, boardWidth, boardHeight, null);

                Image hiddenCardImg = new ImageIcon(getClass().getResource("/Card/BACK.png")).getImage();
                if (!stayButton.isEnabled()) {
                    hiddenCardImg = new ImageIcon(getClass().getResource(hiddenCard.getImagePath())).getImage();
                }
                g.drawImage(hiddenCardImg, 20, 20, cardWidth, cardHeight, null);

                // Draw dealer's hand
                for (int i = 0; i < dealerHand.size(); i++) {
                    Card card = dealerHand.get(i);
                    Image cardImg = new ImageIcon(getClass().getResource(card.getImagePath())).getImage();
                    g.drawImage(cardImg, cardWidth + 25 + (cardWidth + 5) * i, 20, cardWidth, cardHeight, null);
                }

                // Draw player's hand
                for (int i = 0; i < playerHand.size(); i++) {
                    Card card = playerHand.get(i);
                    Image cardImg = new ImageIcon(getClass().getResource(card.getImagePath())).getImage();
                    if (isAnimating && i == playerHand.size() - 1) {
                        g.drawImage(cardImg, cardX, cardY, cardWidth, cardHeight, null);
                    } else {
                        g.drawImage(cardImg, 20 + (cardWidth + 5) * i, 320, cardWidth, cardHeight, null);
                    }
                }

                if (!stayButton.isEnabled()) {
                    dealerSum = reduceDealerAce();
                    playerSum = reducePlayerAce();

                    String message = "";
                    if (playerSum > 21) {
                        message = "YOU LOSE!";
                    } else if (dealerSum > 21) {
                        message = "YOU WIN!";
                    } else if (playerSum == dealerSum) {
                        message = "TIE!";
                    } else if (playerSum > dealerSum) {
                        message = "YOU WIN!";
                    } else if (playerSum < dealerSum) {
                        message = "YOU LOSE!";
                    }

                    g.setFont(new Font("Arial", Font.PLAIN, 30));
                    g.setColor(Color.white);
                    g.drawString(message, 220, 250);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    private JPanel buttonPanel = new JPanel();
    private JPanel scorePanel = new JPanel();
    private JButton hitButton = new JButton("Hit");
    private JButton stayButton = new JButton("Stay");
    private JLabel dealerScoreLabel = new JLabel("Dealer: 0");
    private JLabel playerScoreLabel = new JLabel("Player: 0");

    public BlackJack() {
        loadSoundEffects(); // Load sound effects
        showWelcomeScreen(); // Show welcome screen before initializing game window
    }

    private void showWelcomeScreen() {
        JFrame welcomeFrame = new JFrame("Welcome");
        JPanel welcomePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                try {
                    // Set background color for the welcome screen
                    setBackground(new Color(173, 216, 230)); // Light blue background color
                    Image backgroundImg = new ImageIcon(getClass().getResource("/Card/welcome_bg.jpg")).getImage();
                    g.drawImage(backgroundImg, 0, 0, getWidth(), getHeight(), null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        welcomePanel.setLayout(new BorderLayout());

        JLabel welcomeLabel = new JLabel("Welcome to Blackjack", JLabel.CENTER);
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 24));
        welcomeLabel.setForeground(Color.WHITE); // Set text color to ensure it is visible on the background
        welcomePanel.add(welcomeLabel, BorderLayout.CENTER);

        JButton startButton = new JButton("Start Game");
        startButton.setFont(new Font("Arial", Font.PLAIN, 18));
        welcomePanel.add(startButton, BorderLayout.SOUTH);

        startButton.addActionListener(e -> {
            welcomeFrame.dispose(); // Close the welcome window
            startGame(); // Start the main game
            setupMainGameWindow(); // Set up and show the main game window
        });

        welcomeFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        welcomeFrame.setSize(600, 400); // Increase the size of the welcome window
        welcomeFrame.setLocationRelativeTo(null);
        welcomeFrame.add(welcomePanel);
        welcomeFrame.setVisible(true);
    }

    private void setupMainGameWindow() {
        frame.setVisible(true);
        frame.setSize(boardWidth, boardHeight);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        gamePanel.setLayout(new BorderLayout());
        gamePanel.setBackground(new Color(139, 137, 137)); // Dark green background
        frame.add(gamePanel, BorderLayout.CENTER);

        scorePanel.setLayout(new GridLayout(1, 2));
        scorePanel.setBackground(new Color(255, 204, 51)); // Light yellow background
        scorePanel.add(dealerScoreLabel);
        scorePanel.add(playerScoreLabel);
        gamePanel.add(scorePanel, BorderLayout.NORTH);

        hitButton.setFocusable(false);
        buttonPanel.add(hitButton);
        stayButton.setFocusable(false);
        buttonPanel.add(stayButton);
        buttonPanel.setBackground(new Color(64, 64, 64)); // Dark gray background
        gamePanel.add(buttonPanel, BorderLayout.SOUTH);

        hitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                stopSound(currentPlayingSound); // Stop currently playing sound
                playSound(hitSoundClip); // Play hit sound
                currentPlayingSound = hitSoundClip; // Track the currently playing sound
                Card card = deck.remove(deck.size() - 1);
                playerSum += card.getValue();
                playerAceCount += card.isAce() ? 1 : 0;
                playerHand.add(card);

                startCardAnimation(); // Start animation when the player hits
            }
        });

        stayButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                stopSound(currentPlayingSound); // Stop currently playing sound
                playSound(staySoundClip); // Play stay sound
                currentPlayingSound = staySoundClip; // Track the currently playing sound
                hitButton.setEnabled(false);
                stayButton.setEnabled(false);

                new Thread(() -> {
                    while (dealerSum < 17) {
                        try {
                            Thread.sleep(1000); // 1-second delay between each dealer card draw
                        } catch (InterruptedException interruptedException) {
                            interruptedException.printStackTrace();
                        }

                        Card card = deck.remove(deck.size() - 1);
                        dealerSum += card.getValue();
                        dealerAceCount += card.isAce() ? 1 : 0;
                        dealerHand.add(card);

                        // Update scores and repaint the game panel after each card is drawn
                        updateScores();
                        gamePanel.repaint();
                    }

                    gamePanel.repaint();
                }).start();
            }
        });

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopSoundEffects();    // Stop sound effects if necessary
                System.exit(0);
            }
        });

        gamePanel.repaint();
    }

    private void startGame() {
        // Deck
        buildDeck();
        shuffleDeck();

        // Dealer
        dealerHand = new ArrayList<>();
        dealerSum = 0;
        dealerAceCount = 0;

        hiddenCard = deck.remove(deck.size() - 1); // Remove card at last index
        dealerSum += hiddenCard.getValue();
        dealerAceCount += hiddenCard.isAce() ? 1 : 0;

        Card card = deck.remove(deck.size() - 1);
        dealerSum += card.getValue();
        dealerAceCount += card.isAce() ? 1 : 0;
        dealerHand.add(card);

        // Player
        playerHand = new ArrayList<>();
        playerSum = 0;
        playerAceCount = 0;

        for (int i = 0; i < 2; i++) {
            card = deck.remove(deck.size() - 1);
            playerSum += card.getValue();
            playerAceCount += card.isAce() ? 1 : 0;
            playerHand.add(card);
        }

        updateScores();
    }

    private void buildDeck() {
        deck = new ArrayList<>();
        String[] values = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};
        String[] types = {"C", "D", "H", "S"};

        for (String type : types) {
            for (String value : values) {
                Card card = new Card(value, type);
                deck.add(card);
            }
        }
    }

    private void shuffleDeck() {
        for (int i = 0; i < deck.size(); i++) {
            int j = random.nextInt(deck.size());
            Card currCard = deck.get(i);
            Card randomCard = deck.get(j);
            deck.set(i, randomCard);
            deck.set(j, currCard);
        }
    }

    private int reducePlayerAce() {
        while (playerSum > 21 && playerAceCount > 0) {
            playerSum -= 10;
            playerAceCount -= 1;
        }
        return playerSum;
    }

    private int reduceDealerAce() {
        while (dealerSum > 21 && dealerAceCount > 0) {
            dealerSum -= 10;
            dealerAceCount -= 1;
        }
        return dealerSum;
    }

    private void updateScores() {
        dealerScoreLabel.setText("Dealer: " + dealerSum);
        playerScoreLabel.setText("Player: " + playerSum);
    }

    private void startCardAnimation() {
        isAnimating = true;

        // Starting point for card animation (deck area)
        cardX = boardWidth / 2 - cardWidth / 2;
        cardY = 0;

        cardAnimationTimer = new Timer(animationDelay, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Move the card down toward the player's hand
                cardY += animationStep;
                if (cardY >= 320) {
                    // Stop animation once the card reaches the player's hand
                    cardAnimationTimer.stop();
                    isAnimating = false;
                }
                gamePanel.repaint();
            }
        });
        cardAnimationTimer.start();
    }

    private void loadSoundEffects() {
        try {
            // Load the hit sound
            File hitSoundFile = new File(getClass().getResource("/Card/hit.wav").toURI());
            AudioInputStream hitAudioStream = AudioSystem.getAudioInputStream(hitSoundFile);
            AudioFormat hitFormat = hitAudioStream.getFormat();
            DataLine.Info hitInfo = new DataLine.Info(Clip.class, hitFormat);
            hitSoundClip = (Clip) AudioSystem.getLine(hitInfo);
            hitSoundClip.open(hitAudioStream);

            // Load the stay sound
            File staySoundFile = new File(getClass().getResource("/Card/stay.wav").toURI());
            AudioInputStream stayAudioStream = AudioSystem.getAudioInputStream(staySoundFile);
            AudioFormat stayFormat = stayAudioStream.getFormat();
            DataLine.Info stayInfo = new DataLine.Info(Clip.class, stayFormat);
            staySoundClip = (Clip) AudioSystem.getLine(stayInfo);
            staySoundClip.open(stayAudioStream);

        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void playSound(Clip clip) {
        if (clip != null) {
            clip.setFramePosition(0); // Rewind to the beginning
            clip.start();
        }
    }

    private void stopSound(Clip clip) {
        if (clip != null && clip.isRunning()) {
            clip.stop();
        }
    }

    private void stopSoundEffects() {
        stopSound(hitSoundClip);
        stopSound(staySoundClip);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BlackJack());
    }
}