package players.bayesianMCTS;

import core.AbstractGameState;
import core.components.Deck;
import core.components.FrenchCard;
import games.blackjack.BlackjackGameState;

import java.util.*;

public class BlackjackInformationSet {
    private final List<AbstractGameState> possibleStates = new ArrayList<>();
    private final Random random;

    public BlackjackInformationSet(long seed) {
        this.random = new Random(seed);
    }

    public void update(AbstractGameState observedState, int observingPlayer) {
        possibleStates.clear();

        if (!(observedState instanceof BlackjackGameState)) return;

        BlackjackGameState blackjackState = (BlackjackGameState) observedState;

        // Generate multiple determinizations
        for (int i = 0; i < 10; i++) {
            AbstractGameState determinization = determinize(blackjackState.copy(), observingPlayer);
            if (determinization != null) {
                possibleStates.add(determinization);
            }
        }
    }

    public AbstractGameState sample() {
        if (possibleStates.isEmpty()) return null;
        return possibleStates.get(random.nextInt(possibleStates.size()));
    }

    private AbstractGameState determinize(AbstractGameState state, int observer) {
        if (!(state instanceof BlackjackGameState)) return null;

        BlackjackGameState blackjackState = (BlackjackGameState) state;

        // Get visible and all possible cards
        List<FrenchCard> visibleCards = getVisibleCards(blackjackState, observer);
        List<FrenchCard> allCards = getAllPossibleCards(blackjackState);

        // Create list of unknown cards
        List<FrenchCard> unknownCards = new ArrayList<>(allCards);
        unknownCards.removeAll(visibleCards);

        // Shuffle unknown cards
        Collections.shuffle(unknownCards, random);

        // Redistribute cards
        reassignCards(blackjackState, observer, unknownCards);

        return blackjackState;
    }

    @SuppressWarnings("unchecked")
    private List<FrenchCard> getVisibleCards(BlackjackGameState state, int observer) {
        List<FrenchCard> visibleCards = new ArrayList<>();

        // Add player's own cards
        Deck<FrenchCard> playerHand = (Deck<FrenchCard>) state.getPlayerDecks().get(observer);
        visibleCards.addAll(playerHand.getComponents());

        // Add dealer's visible card (first card)
        int dealerPlayer = state.getNPlayers() - 1; // Dealer is last player
        Deck<FrenchCard> dealerCards = (Deck<FrenchCard>) state.getPlayerDecks().get(dealerPlayer);
        visibleCards.add(dealerCards.get(0)); // Always show first dealer card

        return visibleCards;
    }

    @SuppressWarnings("unchecked")
    private List<FrenchCard> getAllPossibleCards(BlackjackGameState state) {
        List<FrenchCard> allCards = new ArrayList<>();

        // Add all cards from draw deck
        Deck<FrenchCard> drawDeck = (Deck<FrenchCard>) state.getDrawDeck();
        allCards.addAll(drawDeck.getComponents());

        // Add all player cards (including dealer)
        for (Deck<?> playerDeck : state.getPlayerDecks()) {
            Deck<FrenchCard> frenchDeck = (Deck<FrenchCard>) playerDeck;
            allCards.addAll(frenchDeck.getComponents());
        }

        return allCards;
    }

    @SuppressWarnings("unchecked")
    private void reassignCards(BlackjackGameState state, int observer, List<FrenchCard> unknownCards) {
        // Clear the draw deck
        Deck<FrenchCard> drawDeck = (Deck<FrenchCard>) state.getDrawDeck();
        drawDeck.clear();

        // Get dealer's deck
        int dealerPlayer = state.getNPlayers() - 1;
        Deck<FrenchCard> dealerCards = (Deck<FrenchCard>) state.getPlayerDecks().get(dealerPlayer);

        // Keep dealer's visible card (first card)
        FrenchCard visibleDealerCard = dealerCards.get(0);
        dealerCards.clear();
        dealerCards.add(visibleDealerCard); // Restore visible card

        // Add remaining unknown cards to draw deck
        drawDeck.add(unknownCards);
    }
}