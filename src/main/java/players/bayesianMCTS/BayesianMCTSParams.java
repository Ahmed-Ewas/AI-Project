package players.bayesianMCTS;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import players.PlayerParameters;
import java.util.Arrays;
import core.AbstractPlayer;

public class BayesianMCTSParams extends PlayerParameters {
    public double K = Math.sqrt(2);
    public int rolloutLength = 10;
    public int maxTreeDepth = 100;
    public double epsilon = 1e-6;
    public IStateHeuristic heuristic = AbstractGameState::getHeuristicScore;
    public int beliefSamples = 10; // Number of belief states to sample
    public int determinizations = 10; // Number of determinizations to generate

    public BayesianMCTSParams() {
        addTunableParameter("K", Math.sqrt(2), Arrays.asList(0.0, 0.1, 1.0, Math.sqrt(2), 3.0, 10.0));
        addTunableParameter("rolloutLength", 10, Arrays.asList(0, 3, 10, 30, 100));
        addTunableParameter("maxTreeDepth", 100, Arrays.asList(1, 3, 10, 30, 100));
        addTunableParameter("epsilon", 1e-6);
        addTunableParameter("heuristic", (IStateHeuristic) AbstractGameState::getHeuristicScore);
        addTunableParameter("beliefSamples", 10, Arrays.asList(1, 5, 10, 20, 50));
        addTunableParameter("determinizations", 10, Arrays.asList(1, 5, 10, 20, 50));
    }

    @Override
    public void reset() {
        super.reset();
        K = (double) getParameterValue("K");
        rolloutLength = (int) getParameterValue("rolloutLength");
        maxTreeDepth = (int) getParameterValue("maxTreeDepth");
        epsilon = (double) getParameterValue("epsilon");
        heuristic = (IStateHeuristic) getParameterValue("heuristic");
        beliefSamples = (int) getParameterValue("beliefSamples");
        determinizations = (int) getParameterValue("determinizations");
    }

    @Override
    protected BayesianMCTSParams _copy() {
        return new BayesianMCTSParams();
    }

    public IStateHeuristic getHeuristic() {
        return heuristic;
    }

    @Override
    public AbstractPlayer instantiate() {
        // Explicitly cast to BayesianMCTSParams to avoid the constructor error
        return new BayesianMCTSPlayer((BayesianMCTSParams)this.copy());
    }
}