package players.bayesianMCTS;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Card;
import players.PlayerConstants;
import utilities.ElapsedCpuTimer;

import java.util.*;
import java.util.stream.Collectors;

public class BayesianTreeNode {
    private BayesianTreeNode root;
    private BayesianTreeNode parent;
    private Map<AbstractAction, BayesianTreeNode> children = new HashMap<>();
    private final int depth;
    private double totValue;
    private int nVisits;
    private int fmCallsCount;
    private BayesianMCTSPlayer player;
    private Random rnd;
    private AbstractGameState state;
    private Map<Card, Double> cardProbabilities;
    private List<AbstractGameState> possibleHiddenStates;

    public BayesianTreeNode(BayesianMCTSPlayer player, BayesianTreeNode parent,
                            AbstractGameState state, Random rnd,
                            Map<Card, Double> cardProbabilities) {
        this.player = player;
        this.parent = parent;
        this.root = parent == null ? this : parent.root;
        this.totValue = 0.0;
        this.nVisits = 0;
        this.depth = parent == null ? 0 : parent.depth + 1;
        this.rnd = rnd;
        this.cardProbabilities = new HashMap<>(cardProbabilities);
        setState(state);
    }

    void mctsSearch() {
        BayesianMCTSParams params = player.getParameters();
        ElapsedCpuTimer elapsedTimer = new ElapsedCpuTimer();
        if (params.budgetType == PlayerConstants.BUDGET_TIME) {
            elapsedTimer.setMaxTimeMillis(params.budget);
        }

        int numIters = 0;
        boolean stop = false;

        while (!stop) {
            BayesianTreeNode selected = treePolicy();
            double delta = selected.rollOut();
            selected.backUp(delta);
            numIters++;

            if (params.budgetType == PlayerConstants.BUDGET_TIME) {
                long remaining = elapsedTimer.remainingTimeMillis();
                stop = remaining <= 2 * (elapsedTimer.elapsedMillis() / numIters) ||
                        remaining <= params.breakMS;
            } else if (params.budgetType == PlayerConstants.BUDGET_ITERATIONS) {
                stop = numIters >= params.budget;
            } else if (params.budgetType == PlayerConstants.BUDGET_FM_CALLS) {
                stop = fmCallsCount > params.budget;
            }
        }
    }

    private BayesianTreeNode treePolicy() {
        BayesianTreeNode cur = this;
        while (cur.state.isNotTerminal() && cur.depth < player.getParameters().maxTreeDepth) {
            if (!cur.unexpandedActions().isEmpty()) {
                return cur.expand();
            } else {
                AbstractAction actionChosen = cur.ucb();
                cur = cur.children.get(actionChosen);
            }
        }
        return cur;
    }

    private void setState(AbstractGameState newState) {
        state = newState;
        if (newState.isNotTerminal()) {
            for (AbstractAction action : player.getForwardModel()
                    .computeAvailableActions(state, player.getParameters().actionSpace)) {
                children.put(action, null);
            }
        }
    }

    private List<AbstractAction> unexpandedActions() {
        return children.keySet().stream()
                .filter(a -> children.get(a) == null)
                .collect(Collectors.toList());
    }

    private BayesianTreeNode expand() {
        List<AbstractAction> notChosen = unexpandedActions();
        AbstractAction chosen = notChosen.get(rnd.nextInt(notChosen.size()));

        possibleHiddenStates = samplePossibleStates();
        double expectedValue = 0.0;
        BayesianTreeNode child = null;

        for (AbstractGameState hiddenState : possibleHiddenStates) {
            AbstractGameState nextState = hiddenState.copy();
            advance(nextState, chosen.copy());

            if (child == null) {
                child = new BayesianTreeNode(player, this, nextState, rnd, cardProbabilities);
                children.put(chosen, child);
            }

            expectedValue += evaluate(nextState) * getStateProbability(hiddenState);
        }

        if (child != null) {
            child.totValue = expectedValue;
            child.nVisits = 1;
        }

        return child;
    }

    private List<AbstractGameState> samplePossibleStates() {
        List<AbstractGameState> samples = new ArrayList<>();
        for (int i = 0; i < player.getParameters().beliefSamples; i++) {
            samples.add(sampleBeliefState());
        }
        return samples;
    }

    private AbstractGameState sampleBeliefState() {
        AbstractGameState sampledState = state.copy();
        // Implement sampling of hidden information based on cardProbabilities
        return sampledState;
    }

    private double getStateProbability(AbstractGameState hiddenState) {
        // Calculate probability of this hidden state based on cardProbabilities
        return 1.0 / player.getParameters().beliefSamples; // Simple uniform distribution
    }

    private double evaluate(AbstractGameState gs) {
        return player.getParameters().getHeuristic().evaluateState(gs, player.getPlayerID());
    }

    private void advance(AbstractGameState gs, AbstractAction act) {
        player.getForwardModel().next(gs, act);
        root.fmCallsCount++;
    }

    private AbstractAction ucb() {
        AbstractAction bestAction = null;
        double bestValue = -Double.MAX_VALUE;
        BayesianMCTSParams params = player.getParameters();

        for (AbstractAction action : children.keySet()) {
            BayesianTreeNode child = children.get(action);
            if (child == null) continue;

            double childValue = child.totValue / (child.nVisits + params.epsilon);
            double explorationTerm = params.K * Math.sqrt(Math.log(nVisits + 1) / (child.nVisits + params.epsilon));
            double uctValue = (state.getCurrentPlayer() == player.getPlayerID() ? childValue : -childValue) + explorationTerm;
            uctValue = noise(uctValue, params.epsilon, rnd.nextDouble());

            if (uctValue > bestValue) {
                bestAction = action;
                bestValue = uctValue;
            }
        }

        root.fmCallsCount++;
        return bestAction;
    }

    private double rollOut() {
        int rolloutDepth = 0;
        AbstractGameState rolloutState = sampleBeliefState();

        if (player.getParameters().rolloutLength > 0) {
            while (!finishRollout(rolloutState, rolloutDepth)) {
                AbstractAction next = player.getRandomPlayer().getAction(
                        rolloutState,
                        player.getForwardModel().computeAvailableActions(
                                rolloutState,
                                player.getParameters().actionSpace
                        )
                );
                advance(rolloutState, next);
                rolloutDepth++;
            }
        }

        return evaluate(rolloutState);
    }

    private boolean finishRollout(AbstractGameState rollerState, int depth) {
        return depth >= player.getParameters().rolloutLength || !rollerState.isNotTerminal();
    }

    private void backUp(double result) {
        BayesianTreeNode node = this;
        while (node != null) {
            node.nVisits++;
            node.totValue += result;
            node = node.parent;
        }
    }

    public AbstractAction bestAction() {
        AbstractAction bestAction = null;
        double bestValue = -Double.MAX_VALUE;

        for (AbstractAction action : children.keySet()) {
            BayesianTreeNode child = children.get(action);
            if (child == null) continue;

            // Use visit count as the main criterion for best action
            double childValue = child.nVisits;
            if (childValue > bestValue) {
                bestValue = childValue;
                bestAction = action;
            }
        }

        return bestAction;
    }

    private double noise(double value, double epsilon, double random) {
        return value + epsilon * random;
    }
}