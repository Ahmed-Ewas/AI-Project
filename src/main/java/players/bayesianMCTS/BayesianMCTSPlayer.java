
package players.bayesianMCTS;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.components.Card;
import players.PlayerConstants;
import players.simple.RandomPlayer;
import utilities.ElapsedCpuTimer;

import java.util.*;

public class BayesianMCTSPlayer extends AbstractPlayer {
    private Random rnd;
    private RandomPlayer randomPlayer;
    private BlackjackInformationSet informationSet;

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
        this.informationSet = new BlackjackInformationSet(params.getRandomSeed());
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> actions) {
        // Update our information set with the current observed state
        informationSet.update(gameState, getPlayerID());

        // Create root node with our current information
        BayesianTreeNode root = new BayesianTreeNode(this, null, gameState, rnd, null);
        root.mctsSearch();
        return root.bestAction();
    }

    public BlackjackInformationSet getInformationSet() {
        return informationSet;
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
