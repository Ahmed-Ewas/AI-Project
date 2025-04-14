package players.ISMCTS;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import games.uno.UnoGameState;  // For evaluation heuristic
import java.util.*;

public class ISMCTSPlayer extends AbstractPlayer {
    private final int maxIterations;
    private final InformationSet informationSet;
    private final Random rng;
    private ISMCTSNode rootNode;
    private int previousPlayer;

    public ISMCTSPlayer(int maxIterations) {
        super(null, "ISMCTSPlayer");
        this.maxIterations = maxIterations;
        this.rng = new Random();
        this.informationSet = new InformationSet();
    }

    @Override
    public AbstractAction _getAction(AbstractGameState rootState, List<AbstractAction> possibleActions) {
        // Update the information set with the latest observed state
        informationSet.update(rootState, previousPlayer);
        previousPlayer = rootState.getCurrentPlayer();

        // Optionally, tree reuse could be implemented here
        rootNode = new ISMCTSNode(null, null, rootState.getCurrentPlayer());

        for (int i = 0; i < maxIterations; i++) {
            AbstractGameState determinizedState = informationSet.sample();
            if (determinizedState == null) continue;

            ISMCTSNode node = rootNode;
            AbstractGameState state = determinizedState.copy();

            // Selection Phase
            node = select(node, state);

            // Expansion Phase
            if (!state.isGameOver()) {
                node = expand(node, state);
            }

            // Simulation Phase with occasional redeterminization
            int result = simulateWithRedeterminization(state);

            // Backpropagation Phase
            backpropagate(node, result);
        }

        return getBestAction(rootNode, possibleActions);
    }

    private ISMCTSNode select(ISMCTSNode node, AbstractGameState state) {
        while (!state.isGameOver() && node.isFullyExpanded(state)) {
            ISMCTSNode bestChild = null;
            double bestValue = Double.NEGATIVE_INFINITY;
            double parentLog = Math.log(node.getVisitCount() + 1e-6); // safeguard against log(0)

            for (ISMCTSNode child : node.getChildren()) {
                double ucb = ucb1(child, parentLog);
                if (ucb > bestValue) {
                    bestValue = ucb;
                    bestChild = child;
                }
            }

            if (bestChild != null) {
                node = bestChild;
                bestChild.getAction().execute(state);
            } else {
                break;
            }
        }
        return node;
    }

    private ISMCTSNode expand(ISMCTSNode node, AbstractGameState state) {
        List<AbstractAction> actions = state.getActions();
        List<AbstractAction> triedActions = new ArrayList<>();
        for (ISMCTSNode child : node.getChildren()) {
            triedActions.add(child.getAction());
        }

        for (AbstractAction action : actions) {
            if (!triedActions.contains(action)) {
                AbstractGameState newState = state.copy();
                action.execute(newState);
                ISMCTSNode child = new ISMCTSNode(node, action, state.getCurrentPlayer());
                node.addChild(child);
                return child;
            }
        }
        node.setFullyExpanded(true);
        return node;
    }

    private int simulateWithRedeterminization(AbstractGameState state) {
        while (!state.isGameOver()) {
            int currentPlayer = state.getCurrentPlayer();

            // With a 30% probability, re-determinize for the current player
            if (rng.nextDouble() < 0.3) {
                AbstractGameState redeterminedState = informationSet.redeterminize(state, currentPlayer);
                if (redeterminedState != null) {
                    state = redeterminedState;
                }
            }

            List<AbstractAction> actions = state.getActions();
            if (actions.isEmpty()) break;

            AbstractAction action = actions.get(rng.nextInt(actions.size()));
            action.execute(state);
        }
        return evaluate(state);
    }

    private void backpropagate(ISMCTSNode node, int result) {
        while (node != null) {
            node.updateStats(result);
            node = node.getParent();
        }
    }

    private double ucb1(ISMCTSNode node, double parentLog) {
        if (node.getVisitCount() == 0) return Double.POSITIVE_INFINITY;
        return (node.getTotalReward() / node.getVisitCount()) + Math.sqrt(2 * parentLog / node.getVisitCount());
    }

    private AbstractAction getBestAction(ISMCTSNode root, List<AbstractAction> legalActions) {
        return root.getChildren().stream()
                .filter(c -> legalActions.contains(c.getAction()))
                .max(Comparator.comparingInt(ISMCTSNode::getVisitCount))
                .map(ISMCTSNode::getAction)
                .orElse(legalActions.get(rng.nextInt(legalActions.size())));
    }

    private int evaluate(AbstractGameState state) {
        // Terminal evaluation: win or loss
        if (state.isGameOver()) {
            return state.getWinner() == this.getPlayerID() ? 1 : -1;
        }
        // Non-terminal heuristic: evaluate based on hand sizes if the state is of type UnoGameState
        if (state instanceof UnoGameState) {
            UnoGameState unoState = (UnoGameState) state;
            int myCards = unoState.getPlayerDecks().get(this.getPlayerID()).getSize();
            int totalOpponentCards = 0;
            for (int p = 0; p < unoState.getNPlayers(); p++) {
                if (p != this.getPlayerID()) {
                    totalOpponentCards += unoState.getPlayerDecks().get(p).getSize();
                }
            }
            int avgOpponentCards = totalOpponentCards / (unoState.getNPlayers() - 1);
            // The fewer cards I have relative to opponents, the better the state
            return avgOpponentCards - myCards;
        }
        return 0;
    }

    @Override
    public AbstractPlayer copy() {
        return new ISMCTSPlayer(maxIterations);
    }
}
