import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections; // Refactoring: Added for Collections.shuffle
import java.util.Random;
import javax.sound.sampled.*;
import javax.swing.*;

public class BlackJack {

    // Code Smell 1: Duplicated literal - `boardHeight` is always `boardWidth`.
    // Refactoring 1: Replaced duplicated literal with a single constant `BOARD_WIDTH` and derived other related dimensions.
    private static final int BOARD_WIDTH = 600;
    private static final int CARD_WIDTH = 110;
    private static final int CARD_HEIGHT = 154;
    private static final int DEALER_CARD_Y_POS = 20;
    private static final int PLAYER_CARD_Y_POS = 320;
    private static final int CARD_SPACING = 5;
    private static final int DEALER_CARD_OFFSET_X = 25;
    private static final int INITIAL_CARD_X_OFFSET = 20;
    private static final int ANIMATION_TARGET_Y = 320;

    // Code Smell 2: Magic String - "Black Jack", "Hit", "Stay", "Dealer: 0", "Player: 0" etc., are hardcoded strings.
    // Refactoring 2: Extracted magic strings into well-named constants for better readability and maintainability.
    private static final String GAME_TITLE = "Black Jack";
    private static final String WELCOME_TITLE = "Welcome";
    private static final String WELCOME_MESSAGE = "Welcome to Blackjack";
    private static final String START_GAME_BUTTON_TEXT = "Start Game";
    private static final String HIT_BUTTON_TEXT = "Hit";
    private static final String STAY_BUTTON_TEXT = "Stay";
    private static final String DEALER_LABEL_PREFIX = "Dealer: ";
    private static final String PLAYER_LABEL_PREFIX = "Player: ";
    private static final String YOU_LOSE_MESSAGE = "YOU LOSE!";
    private static final String YOU_WIN_MESSAGE = "YOU WIN!";
    private static final String TIE_MESSAGE = "TIE!";

    // Code Smell 7: Magic Numbers / Literal Values - Font name, style, and size are hardcoded.
    // Code Smell 8: Magic Numbers / Literal Values - Coordinates for drawing the message are hardcoded.
    // Refactoring 7 & 8: Extracted font properties and drawing coordinates into constants.
    private static final String FONT_NAME = "Arial";
    private static final int FONT_PLAIN_STYLE = Font.PLAIN; // For clarity, could be a single constant if only one style
    private static final int FONT_BOLD_STYLE = Font.BOLD;
    private static final int FONT_SIZE_LARGE = 30;
    private static final int FONT_SIZE_MEDIUM = 24;
    private static final int FONT_SIZE_SMALL = 18;
    private static final int MESSAGE_X_POS = 220;
    private static final int MESSAGE_Y_POS = 250;

    // Code Smell 4: Magic String/Hardcoded Path - "/Card/Back.jpg" is a hardcoded resource path.
    // Code Smell 5: Magic String/Hardcoded Path - "/Card/BACK.png" is a hardcoded resource path.
    // Code Smell 10: Magic String/Hardcoded Path - "/Card/welcome_bg.jpg" is a hardcoded resource path.
    // Refactoring 4, 5, 10: Centralized image and sound file paths into constants for easier management.
    private static final String CARD_IMAGE_PATH_PREFIX = "/Card/";
    private static final String BACK_IMAGE = CARD_IMAGE_PATH_PREFIX + "Back.jpg";
    private static final String HIDDEN_CARD_IMAGE = CARD_IMAGE_PATH_PREFIX + "BACK.png";
    private static final String WELCOME_BG_IMAGE = CARD_IMAGE_PATH_PREFIX + "welcome_bg.jpg";
    private static final String HIT_SOUND_PATH = CARD_IMAGE_PATH_PREFIX + "hit.wav";
    private static final String STAY_SOUND_PATH = CARD_IMAGE_PATH_PREFIX + "stay.wav";


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
            if ("AJQK".contains(value)) {
                if (value.equals("A")) {
                    return 11;
                }
                return 10;
            }
            return Integer.parseInt(value);
        }

        public boolean isAce() {
            return value.equals("A");
        }

        public String getImagePath() {
            return CARD_IMAGE_PATH_PREFIX + toString() + ".png";
        }
    }

    private ArrayList<Card> deck;
    private Random random = new Random();

    // Dealer
    private Card hiddenCard;
    private ArrayList<Card> dealerHand;
    private int dealerSum;
    private int dealerAceCount;

    // Player
    private ArrayList<Card> playerHand;
    private int playerSum;
    private int playerAceCount;

    // Animation variables
    private int animationDelay = 10;
    private int animationStep = 5;
    private Timer cardAnimationTimer;

    // Positions for animated card drawing
    private int cardX = 0;
    private int cardY = 0;
    private boolean isAnimating = false;

    // Sound Effects
    private Clip hitSoundClip;
    private Clip staySoundClip;
    private Clip currentPlayingSound;

    private JFrame frame = new JFrame(GAME_TITLE); // Refactoring 2 applied
    private JPanel gamePanel = new JPanel() {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // Code Smell 3: Long Method - The paintComponent method is quite long and handles too many responsibilities (drawing background, hidden card, dealer hand, player hand, and game messages).
            // Refactoring 3: Extracted drawing logic into smaller, more focused private methods within the JPanel.
            try {
                drawBackground(g);
                drawDealerHand(g);
                drawPlayerHand(g);

                if (!stayButton.isEnabled()) {
                    // Code Smell 6: Duplicate Code / Repetitive Conditional Logic - The win/lose/tie logic for displaying the message.
                    // Refactoring 6: Extracted game result calculation and message retrieval into a separate method.
                    displayGameResult(g);
                }
            } catch (Exception e) {
                // Code Smell 9: Empty Catch Block / Suppressing Errors - While not empty, it only prints the stack trace, which can hide critical issues during runtime.
                // Refactoring 9: Improved error handling by logging the error message and re-throwing a custom runtime exception if appropriate, or handling gracefully.
                System.err.println("Error during painting: " + e.getMessage());
                // In a production application, you'd typically use a logging framework (e.g., SLF4J, Log4j).
                // e.g., Logger.getLogger(BlackJack.class.getName()).log(Level.SEVERE, "Error during painting", e);
                e.printStackTrace(); // Still include for immediate debugging in development
            }
        }

        // Extracted method for drawing the background
        private void drawBackground(Graphics g) {
            Image backgroundImg = new ImageIcon(getClass().getResource(BACK_IMAGE)).getImage(); // Refactoring 4 applied
            g.drawImage(backgroundImg, 0, 0, BOARD_WIDTH, BOARD_WIDTH, null); // Refactoring 1 applied
        }

        // Extracted method for drawing the dealer's hand
        private void drawDealerHand(Graphics g) {
            Image hiddenCardImg = new ImageIcon(getClass().getResource(HIDDEN_CARD_IMAGE)).getImage(); // Refactoring 5 applied
            if (!stayButton.isEnabled()) {
                hiddenCardImg = new ImageIcon(getClass().getResource(hiddenCard.getImagePath())).getImage();
            }
            g.drawImage(hiddenCardImg, INITIAL_CARD_X_OFFSET, DEALER_CARD_Y_POS, CARD_WIDTH, CARD_HEIGHT, null); // Refactoring 1 applied

            for (int i = 0; i < dealerHand.size(); i++) {
                Card card = dealerHand.get(i);
                Image cardImg = new ImageIcon(getClass().getResource(card.getImagePath())).getImage();
                g.drawImage(cardImg, CARD_WIDTH + DEALER_CARD_OFFSET_X + (CARD_WIDTH + CARD_SPACING) * i, DEALER_CARD_Y_POS, CARD_WIDTH, CARD_HEIGHT, null); // Refactoring 1 applied
            }
        }

        // Extracted method for drawing the player's hand
        private void drawPlayerHand(Graphics g) {
            for (int i = 0; i < playerHand.size(); i++) {
                Card card = playerHand.get(i);
                Image cardImg = new ImageIcon(getClass().getResource(card.getImagePath())).getImage();
                if (isAnimating && i == playerHand.size() - 1) {
                    g.drawImage(cardImg, cardX, cardY, CARD_WIDTH, CARD_HEIGHT, null); // Refactoring 1 applied
                } else {
                    g.drawImage(cardImg, INITIAL_CARD_X_OFFSET + (CARD_WIDTH + CARD_SPACING) * i, PLAYER_CARD_Y_POS, CARD_WIDTH, CARD_HEIGHT, null); // Refactoring 1 applied
                }
            }
        }

        // Extracted method for displaying game result
        private void displayGameResult(Graphics g) {
            dealerSum = reduceDealerAce();
            playerSum = reducePlayerAce();

            String message = getGameResultMessage(); // Refactoring 6 applied
            g.setFont(new Font(FONT_NAME, FONT_PLAIN_STYLE, FONT_SIZE_LARGE)); // Refactoring 7 applied
            g.setColor(Color.white);
            g.drawString(message, MESSAGE_X_POS, MESSAGE_Y_POS); // Refactoring 8 applied
        }

        // Helper method for getting game result message (part of Refactoring 6)
        private String getGameResultMessage() {
            if (playerSum > 21) {
                return YOU_LOSE_MESSAGE;
            } else if (dealerSum > 21) {
                return YOU_WIN_MESSAGE;
            } else if (playerSum == dealerSum) {
                return TIE_MESSAGE;
            } else if (playerSum > dealerSum) {
                return YOU_WIN_MESSAGE;
            } else { // playerSum < dealerSum
                return YOU_LOSE_MESSAGE;
            }
        }
    };
    private JPanel buttonPanel = new JPanel();
    private JPanel scorePanel = new JPanel();
    private JButton hitButton = new JButton(HIT_BUTTON_TEXT); // Refactoring 2 applied
    private JButton stayButton = new JButton(STAY_BUTTON_TEXT); // Refactoring 2 applied
    private JLabel dealerScoreLabel = new JLabel(DEALER_LABEL_PREFIX + "0"); // Refactoring 2 applied
    private JLabel playerScoreLabel = new JLabel(PLAYER_LABEL_PREFIX + "0"); // Refactoring 2 applied

    public BlackJack() {
        loadSoundEffects();
        showWelcomeScreen();
    }

    private void showWelcomeScreen() {
        JFrame welcomeFrame = new JFrame(WELCOME_TITLE); // Refactoring 2 applied
        JPanel welcomePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                try {
                    setBackground(new Color(173, 216, 230));
                    Image backgroundImg = new ImageIcon(getClass().getResource(WELCOME_BG_IMAGE)).getImage(); // Refactoring 10 applied
                    g.drawImage(backgroundImg, 0, 0, getWidth(), getHeight(), null);
                } catch (Exception e) {
                    System.err.println("Error loading welcome background: " + e.getMessage()); // Refactoring 9 applied
                    e.printStackTrace();
                }
            }
        };

        welcomePanel.setLayout(new BorderLayout());

        JLabel welcomeLabel = new JLabel(WELCOME_MESSAGE, JLabel.CENTER); // Refactoring 2 applied
        welcomeLabel.setFont(new Font(FONT_NAME, FONT_BOLD_STYLE, FONT_SIZE_MEDIUM)); // Refactoring 7 applied
        welcomeLabel.setForeground(Color.WHITE);
        welcomePanel.add(welcomeLabel, BorderLayout.CENTER);

        JButton startButton = new JButton(START_GAME_BUTTON_TEXT); // Refactoring 2 applied
        startButton.setFont(new Font(FONT_NAME, FONT_PLAIN_STYLE, FONT_SIZE_SMALL)); // Refactoring 7 applied
        welcomePanel.add(startButton, BorderLayout.SOUTH);

        startButton.addActionListener(e -> {
            welcomeFrame.dispose();
            startGame();
            setupMainGameWindow();
        });

        welcomeFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        welcomeFrame.setSize(600, 400);
        welcomeFrame.setLocationRelativeTo(null);
        welcomeFrame.add(welcomePanel);
        welcomeFrame.setVisible(true);
    }

    private void setupMainGameWindow() {
        frame.setVisible(true);
        frame.setSize(BOARD_WIDTH, BOARD_WIDTH); // Refactoring 1 applied
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        gamePanel.setLayout(new BorderLayout());
        gamePanel.setBackground(new Color(139, 137, 137));
        frame.add(gamePanel, BorderLayout.CENTER);

        scorePanel.setLayout(new GridLayout(1, 2));
        scorePanel.setBackground(new Color(255, 204, 51));
        scorePanel.add(dealerScoreLabel);
        scorePanel.add(playerScoreLabel);
        gamePanel.add(scorePanel, BorderLayout.NORTH);

        hitButton.setFocusable(false);
        buttonPanel.add(hitButton);
        stayButton.setFocusable(false);
        buttonPanel.add(stayButton);
        buttonPanel.setBackground(new Color(64, 64, 64));
        gamePanel.add(buttonPanel, BorderLayout.SOUTH);

        hitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                stopSound(currentPlayingSound);
                playSound(hitSoundClip);
                currentPlayingSound = hitSoundClip;
                Card card = deck.remove(deck.size() - 1);
                playerSum += card.getValue();
                playerAceCount += card.isAce() ? 1 : 0;
                playerHand.add(card);

                startCardAnimation();
            }
        });

        stayButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                stopSound(currentPlayingSound);
                playSound(staySoundClip);
                currentPlayingSound = staySoundClip;
                hitButton.setEnabled(false);
                stayButton.setEnabled(false);

                new Thread(() -> {
                    while (dealerSum < 17) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException interruptedException) {
                            System.err.println("Dealer draw interrupted: " + interruptedException.getMessage()); // Refactoring 9 applied
                            Thread.currentThread().interrupt(); // Restore interrupted status
                        }

                        Card card = deck.remove(deck.size() - 1);
                        dealerSum += card.getValue();
                        dealerAceCount += card.isAce() ? 1 : 0;
                        dealerHand.add(card);

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
                stopSoundEffects();
                System.exit(0);
            }
        });

        gamePanel.repaint();
    }

    private void startGame() {
        buildDeck();
        shuffleDeck();

        dealerHand = new ArrayList<>();
        dealerSum = 0;
        dealerAceCount = 0;

        hiddenCard = deck.remove(deck.size() - 1);
        dealerSum += hiddenCard.getValue();
        dealerAceCount += hiddenCard.isAce() ? 1 : 0;

        Card card = deck.remove(deck.size() - 1);
        dealerSum += card.getValue();
        dealerAceCount += card.isAce() ? 1 : 0;
        dealerHand.add(card);

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
        // Code Smell: Manual shuffling loop is less readable and potentially less efficient than standard library.
        // Refactoring: Used `Collections.shuffle()` for a more concise and robust shuffle.
        Collections.shuffle(deck);
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
        dealerScoreLabel.setText(DEALER_LABEL_PREFIX + dealerSum); // Refactoring 2 applied
        playerScoreLabel.setText(PLAYER_LABEL_PREFIX + playerSum); // Refactoring 2 applied
    }

    private void startCardAnimation() {
        isAnimating = true;

        cardX = BOARD_WIDTH / 2 - CARD_WIDTH / 2; // Refactoring 1 applied
        cardY = 0;

        cardAnimationTimer = new Timer(animationDelay, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cardY += animationStep;
                if (cardY >= ANIMATION_TARGET_Y) { // Refactoring 1 applied
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
            // Code Smell: Repeated code for loading each sound clip.
            // Refactoring: Extracted common sound loading logic into a separate `loadClip` method.
            hitSoundClip = loadClip(HIT_SOUND_PATH);
            staySoundClip = loadClip(STAY_SOUND_PATH);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException | URISyntaxException e) {
            System.err.println("Error loading sound effects: " + e.getMessage()); // Refactoring 9 applied
            e.printStackTrace();
        }
    }

    // Extracted method to load an audio clip (part of Refactoring 9)
    private Clip loadClip(String path) throws UnsupportedAudioFileException, IOException, LineUnavailableException, URISyntaxException {
        File soundFile = new File(getClass().getResource(path).toURI());
        AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);
        AudioFormat format = audioStream.getFormat();
        DataLine.Info info = new DataLine.Info(Clip.class, format);
        Clip clip = (Clip) AudioSystem.getLine(info);
        clip.open(audioStream);
        return clip;
    }

    private void playSound(Clip clip) {
        if (clip != null) {
            clip.setFramePosition(0);
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