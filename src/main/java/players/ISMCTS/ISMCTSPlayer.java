package players.ISMCTS;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;

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
        informationSet.update(rootState, previousPlayer);
        previousPlayer = rootState.getCurrentPlayer();
        rootNode = new ISMCTSNode(null, null, rootState.getCurrentPlayer());

        for (int i = 0; i < maxIterations; i++) {
            AbstractGameState determinizedState = informationSet.sample();
            if (determinizedState == null) continue;

            ISMCTSNode node = rootNode;
            AbstractGameState state = determinizedState.copy();

            // Selection
            node = select(node, state);

            // Expansion
            if (!state.isGameOver()) {
                node = expand(node, state);
            }

            // Simulation with re-determinization per acting player
            int result = simulateWithRedeterminization(state);

            // Backpropagation
            backpropagate(node, result);
        }

        return getBestAction(rootNode, possibleActions);
    }

    private ISMCTSNode select(ISMCTSNode node, AbstractGameState state) {
        while (!state.isGameOver() && node.isFullyExpanded(state)) {
            ISMCTSNode bestChild = null;
            double bestValue = Double.NEGATIVE_INFINITY;
            double parentLog = Math.log(node.getVisitCount());

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

            // Re-determinize for current player
            AbstractGameState redeterminedState = informationSet.redeterminize(state, currentPlayer);
            if (redeterminedState != null) {
                state = redeterminedState;
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
        return (node.getTotalReward() / node.getVisitCount()) +
                Math.sqrt(2 * parentLog / node.getVisitCount());
    }

    private AbstractAction getBestAction(ISMCTSNode root, List<AbstractAction> legalActions) {
        return root.getChildren().stream()
                .filter(c -> legalActions.contains(c.getAction()))
                .max(Comparator.comparingInt(ISMCTSNode::getVisitCount))
                .map(ISMCTSNode::getAction)
                .orElse(legalActions.get(rng.nextInt(legalActions.size())));
    }

    private int evaluate(AbstractGameState state) {
        if (state.isGameOver()) {
            return state.getWinner() == this.getPlayerID() ? 1 : -1;
        }
        return 0;
    }

    @Override
    public AbstractPlayer copy() {
        return new ISMCTSPlayer(maxIterations);
    }
}
