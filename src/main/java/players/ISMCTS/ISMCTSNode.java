package players.ISMCTS;

import core.AbstractGameState;
import core.actions.AbstractAction;
import java.util.ArrayList;
import java.util.List;

public class ISMCTSNode {
    private final ISMCTSNode parent;
    private final AbstractAction action;
    private final int player;
    private int visitCount;
    private double totalReward;
    private final List<ISMCTSNode> children;
    private boolean fullyExpanded;

    public ISMCTSNode(ISMCTSNode parent, AbstractAction action, int player) {
        this.parent = parent;
        this.action = action;
        this.player = player;
        this.visitCount = 0;
        this.totalReward = 0;
        this.children = new ArrayList<>();
        this.fullyExpanded = false;
    }

    public void updateStats(double reward) {
        visitCount++;
        totalReward += reward;
    }

    public boolean isFullyExpanded(AbstractGameState state) {
        // A node is fully expanded if the number of children matches the number of legal actions.
        return fullyExpanded || children.size() >= state.getActions().size();
    }

    public void setFullyExpanded(boolean fullyExpanded) {
        this.fullyExpanded = fullyExpanded;
    }

    public ISMCTSNode getParent() {
        return parent;
    }

    public AbstractAction getAction() {
        return action;
    }

    public int getPlayer() {
        return player;
    }

    public int getVisitCount() {
        return visitCount;
    }

    public double getTotalReward() {
        return totalReward;
    }

    public List<ISMCTSNode> getChildren() {
        return children;
    }

    public void addChild(ISMCTSNode child) {
        children.add(child);
    }
}
