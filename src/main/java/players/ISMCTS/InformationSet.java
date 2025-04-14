package players.ISMCTS;

import core.AbstractGameState;
import core.components.Card;
import core.components.Deck;
import games.uno.UnoGameState;
import games.uno.cards.UnoCard;
import java.util.*;

public class InformationSet {
    private List<AbstractGameState> states = new ArrayList<>();
    private final Random random = new Random();

    public void update(AbstractGameState observedState, int observingPlayer) {
        states.clear();
        int numSamples = 10;
        for (int i = 0; i < numSamples; i++) {
            AbstractGameState clone = observedState.copy();
            AbstractGameState determinizedState = determinize(clone, observingPlayer);
            if (determinizedState != null) {
                states.add(determinizedState);
            }
        }
    }

    public AbstractGameState sample() {
        if (states.isEmpty()) return null;
        return states.get(random.nextInt(states.size()));
    }

    private AbstractGameState determinize(AbstractGameState state, int observer) {
        if (!(state instanceof UnoGameState)) return null;

        UnoGameState unoState = (UnoGameState) state;

        // Retrieve known cards: the observer's hand and the discard pile
        List<Card> knownCards = new ArrayList<>();
        Deck<UnoCard> playerHand = unoState.getPlayerDecks().get(observer);
        knownCards.addAll(playerHand.getComponents());

        Deck<UnoCard> discardPile = unoState.getDiscardDeck();
        knownCards.addAll(discardPile.getComponents());

        // Construct the draw deck by subtracting known cards
        Deck<UnoCard> drawDeck = unoState.getDrawDeck();
        List<Card> remainingUnknowns = new ArrayList<>(drawDeck.getComponents());
        remainingUnknowns.removeAll(knownCards);

        // Shuffle the unknown cards
        Collections.shuffle(remainingUnknowns, random);

        // Redistribute unknown cards to other players
        for (int p = 0; p < unoState.getNPlayers(); p++) {
            if (p == observer) continue;
            Deck<UnoCard> opponentHand = unoState.getPlayerDecks().get(p);
            int handSize = opponentHand.getSize();  // Preserve original hand size
            opponentHand.clear();
            for (int j = 0; j < handSize && !remainingUnknowns.isEmpty(); j++) {
                opponentHand.add((UnoCard) remainingUnknowns.remove(0));
            }
        }

        // Reset the draw deck with any remaining cards and shuffle
        drawDeck.clear();
        for (Card card : remainingUnknowns) {
            drawDeck.add((UnoCard) card);
        }
        drawDeck.shuffle(random);

        return unoState;
    }

    public AbstractGameState redeterminize(AbstractGameState observedState, int observer) {
        AbstractGameState clone = observedState.copy();
        return determinize(clone, observer);
    }
}
