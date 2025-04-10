package players.simple;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import games.uno.actions.PlayCard;
import games.uno.cards.UnoCard;

import java.util.List;

public class HighestActionPlayer extends AbstractPlayer {

    public HighestActionPlayer() {
        super(null, "HighestActionPlayer");
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gs, List<AbstractAction> possibleActions) {
        AbstractAction bestAction = null;
        int highestValue = Integer.MIN_VALUE;

        for (AbstractAction action : possibleActions) {
            int actionValue = getActionValue(action, gs);

            if (actionValue > highestValue) {
                highestValue = actionValue;
                bestAction = action;
            }
        }

        return bestAction;
    }

    private int getActionValue(AbstractAction action, AbstractGameState gs) {
        if (action instanceof PlayCard) {
            UnoCard card = (UnoCard) ((PlayCard) action).getCard(gs);
            return evaluateCardValue(card);
        }
        return Integer.MIN_VALUE;
    }

    private int evaluateCardValue(UnoCard card) {
        if (card == null) return Integer.MIN_VALUE;

        switch (card.type) {
            case Wild: return 50;      // Wild cards are most powerful
            case Draw: return 20 + card.drawN; // Draw cards have extra effect
            case Reverse: return 15;   // Reverse is useful
            case Skip: return 10;      // Skip is decent
            default: return card.number; // Number cards use their own value
        }
    }

    @Override
    public HighestActionPlayer copy() {
        return new HighestActionPlayer();
    }
}
