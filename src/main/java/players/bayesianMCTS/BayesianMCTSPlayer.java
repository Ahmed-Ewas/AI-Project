package players.bayesianMCTS;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.components.Card;  // Now using Card instead of Component
import players.PlayerConstants;
import players.simple.RandomPlayer;
import utilities.ElapsedCpuTimer;

import java.util.*;

public class BayesianMCTSPlayer extends AbstractPlayer {
    private Random rnd;
    private RandomPlayer randomPlayer;
    private Map<Card, Double> cardProbabilities;  // Changed from Component to Card
    private List<Card> knownCards;  // Changed from knownComponents

    public BayesianMCTSPlayer() {
        this(System.currentTimeMillis());
    }

    public BayesianMCTSPlayer(long seed) {
        this(new BayesianMCTSParams());
        getParameters().setRandomSeed(seed);
    }

    public BayesianMCTSPlayer(BayesianMCTSParams params) {
        super(params, "Bayesian MCTS");
        this.rnd = new Random(params.getRandomSeed());
        this.randomPlayer = new RandomPlayer(this.rnd);
        this.knownCards = new ArrayList<>();
        this.cardProbabilities = new HashMap<>();
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> actions) {
        updateBeliefState(gameState);
        BayesianTreeNode root = new BayesianTreeNode(this, null, gameState, rnd, cardProbabilities);
        root.mctsSearch();
        return root.bestAction();
    }

    private void updateBeliefState(AbstractGameState gameState) {
        if (cardProbabilities.isEmpty()) {
            initializeCardProbabilities(gameState);
        }

        List<Card> visibleCards = getVisibleCards(gameState);
        for (Card card : visibleCards) {
            if (!knownCards.contains(card)) {
                knownCards.add(card);
                cardProbabilities.put(card, 0.0);
            }
        }
        updateRemainingProbabilities(gameState);
    }

    private void initializeCardProbabilities(AbstractGameState gameState) {
        List<Card> allCards = getAllPossibleCards(gameState);
        double initialProb = 1.0 / allCards.size();
        for (Card card : allCards) {
            cardProbabilities.put(card, initialProb);
        }
    }

    // Game-specific methods
    private List<Card> getVisibleCards(AbstractGameState gameState) {
        // Implement based on your game
        // Example: return gameState.getPlayerCards(getPlayerID());
        return new ArrayList<>();
    }

    private List<Card> getAllPossibleCards(AbstractGameState gameState) {
        // Return all possible cards in the game
        // Example: return gameState.getAllCards();
        return new ArrayList<>();
    }

    private void updateRemainingProbabilities(AbstractGameState gameState) {
        int remaining = getAllPossibleCards(gameState).size() - knownCards.size();
        if (remaining > 0) {
            double newProb = 1.0 / remaining;
            cardProbabilities.replaceAll((k, v) -> knownCards.contains(k) ? 0.0 : newProb);
        }
    }

    public BayesianMCTSParams getParameters() {
        return (BayesianMCTSParams) parameters;
    }

    public Random getRnd() {
        return rnd;
    }

    public RandomPlayer getRandomPlayer() {
        return randomPlayer;
    }

    @Override
    public BayesianMCTSPlayer copy() {
        return new BayesianMCTSPlayer((BayesianMCTSParams) parameters.copy());
    }
}