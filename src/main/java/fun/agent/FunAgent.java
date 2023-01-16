package fun.agent;

import at.ac.tuwien.ifs.sge.agent.AbstractGameAgent;
import at.ac.tuwien.ifs.sge.agent.GameAgent;
import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.game.risk.board.*;
import at.ac.tuwien.ifs.sge.util.Util;
import at.ac.tuwien.ifs.sge.util.tree.DoubleLinkedTree;
import at.ac.tuwien.ifs.sge.util.tree.Tree;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * A game agent for the board game Risk.
 * It utilizes MCTS with clever heuristics to prune the tree of possible action.
 */
public class FunAgent extends AbstractGameAgent<Risk, RiskAction>
        implements GameAgent<Risk, RiskAction> {

    /*
    TODO: rewrite MCTS to use scalar values instead of boolean for T

    TODO:


     */

    private static final int RUN_LIMIT = 1_000;
    private static int INSTANCE_NR_COUNTER;
    private final Tree<McRiskNode> mcTree;
    private Comparator<Tree<McRiskNode>> gameMcTreeUCTComparator;
    private final double exploitationConstant;
    private final int instanceNr;
    private Comparator<McRiskNode> gameMcNodePlayComparator;
    private Comparator<Tree<McRiskNode>> gameMcTreePlayComparator;
    private Comparator<McRiskNode> gameMcNodeWinComparator;
    private Comparator<Tree<McRiskNode>> gameMcTreeWinComparator;
    private Comparator<McRiskNode> gameMcNodeGameComparator;
    private Comparator<Tree<McRiskNode>> gameMcTreeGameComparator;
    private Comparator<Tree<McRiskNode>> gameMcTreeSelectionComparator;
    private Comparator<McRiskNode> gameMcNodeMoveComparator;
    private Comparator<Tree<McRiskNode>> gameMcTreeMoveComparator;

    private Map<Integer, Set<RiskTerritory>> continentTerritories;


    public FunAgent(Logger log) {
        this(Math.sqrt(2.0), log);
    }

    public FunAgent(double exploitationConstant, Logger log) {
        /* instantiates a AbstractGameAgent so that shouldStopComputation() returns true after 3/4ths of
         * the given time. However, if it can, it will try to compute at least 5 seconds.
         */
        super(3D / 4D, 5, TimeUnit.SECONDS, log);

        //Do some setup before the TOURNAMENT starts.
        this.exploitationConstant = exploitationConstant;
        this.mcTree = new DoubleLinkedTree<>();
        this.instanceNr = INSTANCE_NR_COUNTER++;
    }

    @Override
    public void setUp(int numberOfPlayers, int playerId) {
        super.setUp(numberOfPlayers, playerId);
        // Do setup before the MATCH starts
        mcTree.clear();
        mcTree.setNode(new McRiskNode());

        // UCT Comparator
        gameMcTreeUCTComparator = Comparator.comparingDouble((t) -> upperConfidenceBound(t, exploitationConstant));

        // Play Comparators
        gameMcNodePlayComparator = Comparator.comparingInt(McRiskNode::getPlays);
        gameMcTreePlayComparator = (t1, t2) -> gameMcNodePlayComparator.compare(t1.getNode(), t2.getNode());

        // Win Comparators
        gameMcNodeWinComparator = Comparator.comparingInt(McRiskNode::getWins);
        gameMcTreeWinComparator = (t1, t2) -> gameMcNodeWinComparator.compare(t1.getNode(), t2.getNode());

        // Game Comparators
        gameMcNodeGameComparator = (n1, n2) -> gameComparator.compare(n1.getGame(), n2.getGame());
        gameMcTreeGameComparator = (t1, t2) -> gameMcNodeGameComparator.compare(t1.getNode(), t2.getNode());

        // Combine Comparators
        gameMcTreeSelectionComparator = gameMcTreeUCTComparator.thenComparing(gameMcTreeGameComparator);
        gameMcNodeMoveComparator = gameMcNodePlayComparator
                .thenComparing(gameMcNodeWinComparator).thenComparing(gameMcNodeGameComparator);
        gameMcTreeMoveComparator = (t1, t2) -> gameMcNodeMoveComparator.compare(t1.getNode(), t2.getNode());
    }

    /**
     * Return the chosen action for the given game in its current state.
     */
    public RiskAction computeNextAction(Risk game, long computationTime, TimeUnit timeUnit) {
        super.setTimers(computationTime, timeUnit); // Makes sure shouldStopComputation() works

        RiskBoard board = game.getBoard();
        if (continentTerritories == null) continentTerritories = loadContinentTerritories(board);

        // |=====   Use opening Book if possible ======================================================================|

        // Territory Selection Phase at the beginning of the game.
        if (board.isPlayerStillAlive(-1)) {
            RiskAction chosenAction = chooseOpeningAction(game);
            if (chosenAction != null) return chosenAction;
        }

        // |=====   Use MCTS Approach after Opening Book ==============================================================|
        log.tra_("Searching for root of tree");
        boolean foundRoot = Util.findRoot(mcTree, game);

        if (foundRoot) log._trace(", done.");
        else log._trace(", failed.");

        log.debf_("MCTS with %d simulations at confidence %.1f%%",
                mcTree.getNode().getPlays(),
                Util.percentage(mcTree.getNode().getWins(), mcTree.getNode().getPlays())
        );

        int looped = 0;
        int printThreshold = 1;

        // Run mcts algorithm until time runs out
        while (!shouldStopComputation() && mcTree.getNode().getPlays() < RUN_LIMIT) {
           if (looped++ % printThreshold == 0) {
                log._deb("\r");
                log.debf_("MCTS with %d simulations at confidence %.1f%%",
                            mcTree.getNode().getPlays(),
                            Util.percentage(mcTree.getNode().getWins(),
                            mcTree.getNode().getPlays())
                );
            }

           // |----- Do MCTS Selection Phase --------------------------------------------------------------------------|
           Tree<McRiskNode> tree = mcSelection(mcTree);

           // |----- Do MCTS Expansion Phase --------------------------------------------------------------------------|
           mcExpansion(tree);

           // |----- Do MCTS Simulation Phase -------------------------------------------------------------------------|
           boolean won = mcSimulation(tree, 128, 2);

           // |----- Do MCTS Backpropagation Phase --------------------------------------------------------------------|
           mcBackPropagation(tree, won);


           if (printThreshold < 97)
                printThreshold = Math.max(
                        1, Math.min(97, Math.round((float) mcTree.getNode().getPlays() * 11.1111111F))
                );
        }

        log_after_mcts_creation(timeUnit);

        // |===== Return Action =======================================================================================|

        // Return best Action of Mcts Tree
        if (!mcTree.isLeaf()) {
            return Collections.max(
                    mcTree.getChildren(),
                    gameMcTreeMoveComparator
            ).getNode().getGame().getPreviousAction();
        }

        // Fallback to greedy otherwise
        log._debug(". Could not find a move, choosing the next best greedy option.");
        return Collections.max(
                game.getPossibleActions(),
                (a1, a2) -> gameComparator.compare(game.doAction(a1), game.doAction(a2))
        );
    }

    /**
     * MCTS Selection Phase. Find the next node to expand by calculating UCB1 and
     * return subtree containing selected node as root.
     *
     * @param tree Base tree the search is done on.
     * @return Tree with selected node as root.
     */
    private Tree<McRiskNode> mcSelection(Tree<McRiskNode> tree) {
        int depth = 0;

        while (!tree.isLeaf() && (depth++ % 31 != 0 || !shouldStopComputation())) {
            List<Tree<McRiskNode>> children = new ArrayList<>(tree.getChildren());

            if (tree.getNode().getGame().getCurrentPlayer() < 0) {
                RiskAction action = tree.getNode().getGame().determineNextAction();

                for (Tree<McRiskNode> child : children) {
                    if ((child.getNode()).getGame().getPreviousAction().equals(action)) {
                        tree = child;
                        break;
                    }
                }
            } else
                tree = Collections.max(children, gameMcTreeSelectionComparator);
        }

        return tree;
    }

    /**
     * MCTS Expansion Phase. Expand the root node of given tree with possible actions.
     */
    private void mcExpansion(Tree<McRiskNode> tree) {
        Risk game = (Risk) tree.getNode().getGame();
        RiskBoard baseBoard = game.getBoard();
        Set<RiskAction> possibleActions = game.getPossibleActions();


        if (game.getUtilityValue(game.getCurrentPlayer()) == 1) {
            // current player has already won
            return;
        }

        if (tree.isLeaf()) {
            // Exclude engine players from heuristics.
            if (game.getCurrentPlayer() >= 0) {
                if (baseBoard.isReinforcementPhase()) {
                    expandReinforcementActions(tree);
                } else if (baseBoard.isAttackPhase()) {
                    expandAttackActions(tree);
                } else if (baseBoard.isOccupyPhase()) {
                    expandOccupyActions(tree);
                } else if (baseBoard.isFortifyPhase()) {
                    expandFortifyActions(tree);
                }
            }
            // Fallback
            if (tree.getChildren().isEmpty()) {
                log.debug("Expansion via Fallback: adding all possible actions.");
                for (RiskAction possibleAction : possibleActions)
                    tree.add(new McRiskNode(game, possibleAction));
            }
        }
    }

    /**
     * MCTS Simulation Phase. Base mcSimulation calling the other mcSimulations.
     * Simulation is done with a depth of up to 128 actions, if the game didn't finish until then,
     * a heuristic value is calculated to decide which player is currently winning.
     * <p>
     * Going deeper than 128 actions resulted in a worse agent.
     */
    private boolean mcSimulation(Tree<McRiskNode> tree, int simulationAtLeast, int proportion) {
        int simulationsDone = tree.getNode().getPlays();
        if (simulationsDone < simulationAtLeast && shouldStopComputation(proportion)) {
            int simulationsLeft = simulationAtLeast - simulationsDone;
            return mcSimulation(tree, nanosLeft() / (long) simulationsLeft);
        } else
            return simulationsDone == 0?
                    mcSimulation(tree, TIMEOUT / 2L - nanosElapsed()) : mcSimulation(tree);
    }

    private boolean mcSimulation(Tree<McRiskNode> tree) {
        Risk game = (Risk) tree.getNode().getGame();
        int depth = 0;

        while(!game.isGameOver() && ++depth % 128 != 0 && !shouldStopComputation()) {
            if (game.getCurrentPlayer() < 0)
                game = (Risk) game.doAction();
            else
                game = (Risk) game.doAction(Util.selectRandom(game.getPossibleActions(), random));
        }

        return mcHasWon(game);
    }

    private boolean mcSimulation(Tree<McRiskNode> tree, long timeout) {
        long startTime = System.nanoTime();
        Risk game = (Risk) tree.getNode().getGame();
        int depth = 0;

        while(!game.isGameOver() && System.nanoTime() - startTime <= timeout &&
                ++depth % 128 != 0 && !shouldStopComputation()) {
            if (game.getCurrentPlayer() < 0)
                game = (Risk) game.doAction();
            else
                game = (Risk) game.doAction(Util.selectRandom(game.getPossibleActions(), random));
        }

        return mcHasWon(game);
    }

    /**
     * MCTS Backpropagation Phase. Propagate Simulation back to root.
     */
    private void mcBackPropagation(Tree<McRiskNode> tree, boolean win) {
        int depth = 0;

        while (!tree.isRoot() && (depth++ % 31 != 0 || !shouldStopComputation())) {
            tree = tree.getParent();
            tree.getNode().incPlays();
            if (win) tree.getNode().incWins();
        }
    }

    /**
     * MCTS calculation of UCB1. Return UCB1 for given tree.
     */
    private double upperConfidenceBound(Tree<McRiskNode> tree, double c) {
        double w = tree.getNode().getWins();
        double n = Math.max(tree.getNode().getPlays(), 1);
        double N = n;
        if (!tree.isRoot()) {
            N = (tree.getParent().getNode()).getPlays();
        }

        return w / n + c * Math.sqrt(Math.log(N) / n);
    }

    /**
     * Decides whether player has won the game, if needed with heuristic h1:
     *              h1 = # Occupied Territories + Continent Bonuses
     */
    private boolean mcHasWon(Risk game) {

        // array with 1 for win and 0 otherwise
        double[] evaluation = game.getGameUtilityValue();

        // val = evaluation[playerId], n = evaluation.length, max = max(evaluation - val)
        // (val >= max? 1/n : 0) + (val > max? (n-1)/n : 0)
        //
        // val <  max   => 0            -> Loose
        // val == max   => 1/n aka 1/2  -> Tie
        // val >  max   => 1            -> Win
        double score = Util.scoreOutOfUtility(evaluation, playerId);

        if (!game.isGameOver() && score > 0.0) {
            // calculate evaluation = territories / 3 + continentBonuses
            evaluation = heuristicUtility(game);
            score = Util.scoreOutOfUtility(evaluation, playerId);
        }

        boolean win = score == 1.0;
        boolean tie = score > 0.0;

        // Return true for ~50% of all ties and 100% of all wins
        return win || tie && random.nextBoolean();
    }

    /**
     * Choose the best action for the next action of the current player in the given game.
     * This is done by utilizing an opening book of preferred moves.
     * <p>
     * Those preferred moves consist of territories in both North and South America with a focus on South America
     * as this is the next best continent to hold besides Australia while still allowing fast
     * expansion to both Africa and North America.
     * However, if an enemy threatens to take a whole continent, the continent bonus is prevented by placing
     * a troop in the threatened continent.
     *
     * @param game The game we are searching the best action for.
     * @return The best action if an opening action is possible, otherwise null.
     */
    private RiskAction chooseOpeningAction(Risk game) {
        log.debug("Board not yet fully occupied");

        // Check if continent has < 3 free territories and no territories of playerId.
        for (Integer continentId : game.getBoard().getContinentIds()) {
            Set<Integer> freeTerritories = getOccupiedTerritories(-1, continentId, game.getBoard());
            if (freeTerritories.size() < 3 &&
                    getOccupiedTerritories(playerId, continentId, game.getBoard()).size() == 0
            )
                // Select action that sets an army in an empty territory in continentId.
                // This prevents the continent bonus for the enemy player.
                for (Integer territory: freeTerritories){
                    // TODO Don't take first option and choose better?
                    RiskAction action = RiskAction.select(territory);
                    if (game.isValidAction(action)) {
                        return action;
                    }
                    else throw new RuntimeException("Not valid action.");
                }
        }

        // Priority of territories (opening book).
        // Brazil, Central America, South America, North America, Iceland, North Africa, Kamchatka
        int[] territoryPriority = {10, 2, 9, 11, 12, 0, 1, 3, 4, 5, 6, 7, 8, 14, 24, 31};
        for (int territoryId: territoryPriority) {
            if (game.getBoard().getTerritoryOccupantId(territoryId) == -1) {
                RiskAction action = RiskAction.select(territoryId);
                if (game.isValidAction(action)) {
                    return action;
                }
                else throw new RuntimeException("Not valid action.");
            }
        }
        return null;
    }

    /**
     * Expand the tree with pruned actions via heuristics for the reinforcement Phase.
     */
    private void expandReinforcementActions(Tree<McRiskNode> tree) {
        log.trace("Reinforcement Phase");

        Risk game = (Risk) tree.getNode().getGame();
        Set <RiskAction> actions = game.getPossibleActions();

        if (game.getBoard().isPlayerStillAlive(-1)) // TODO: add logic for setup phase after opening book
            return;

        // Trade-in only if you have to
        if (game.getBoard().hasToTradeInCards(game.getCurrentPlayer())) return;

        Set<Integer> borderTerritories = getBorderTerritories(game).stream()
                // Only keep borderTerritories which are in possibleActions().
                .filter(territoryId -> actions.stream().anyMatch(action -> action.reinforcedId() == territoryId))
                .collect(Collectors.toSet());

        // Maximum number of reinforcable troops border territories
        int maxTroops = (actions.stream()
                .filter(this::isRegularAction)
                .filter(action -> borderTerritories.contains(action.fortifiedId()))
                .max(Comparator.comparingInt(RiskAction::troops)))
                .map(RiskAction::troops)
                .orElse(1);

        // Filter out actions.
        Set<RiskAction> filteredActions = actions.stream()
                .filter(this::isRegularAction)
                // Only reinforce territories with enemy neighbor.
                .filter(action -> borderTerritories.contains(action.fortifiedId()))
                // Reinforce with enough troops to be able to reinforce only border territories
                .filter(action -> (action.troops() >= maxTroops / borderTerritories.size()))
                .collect(Collectors.toSet());

        // Add all actions if no filteredAction left, e.g. getting troops from card --> not on border.
        if (filteredActions.size() == 0)
            filteredActions.addAll(actions);

        for (RiskAction action : filteredActions) {
            int wins = 0;
            int plays = 0;
            // TODO bias more promising moves here
            tree.add(new McRiskNode(tree.getNode().getGame().doAction(action), wins, plays));
        }
    }

    /**
     * Expand the tree with pruned actions via heuristics for the reinforcement Phase.
     */
    private void expandAttackActions(Tree<McRiskNode> tree) {
        log.trace("Attack Phase");
        // TODO: add one continent preference logic
        // Prefer to conquer continents for bonus.
        // Don't unblock enemy territory which has many troops.
        // Always attack if 2v1, 3v1, 3v2.
        // Even attack if attacking territory has less troops than defending territory.

        Risk game = (Risk) tree.getNode().getGame();
        RiskBoard board = game.getBoard();
        Set <RiskAction> actions = game.getPossibleActions();

        Set<RiskAction> filteredActions = actions.stream()
                .filter(this::isRegularAction)
                // Always attack with maximum amount of troops.
                .filter(action -> action.troops() == board.getMaxAttackingTroops(action.attackingId()))
                // Only keep actions where attackingTroops > defendingTroops.
                .filter(action -> board.getMaxAttackingTroops(action.attackingId()) >
                        Math.min(2, board.getTerritoryTroops(action.defendingId())))
                .collect(Collectors.toSet());
        // Add endPhase() if no attack left.
        if (filteredActions.size() == 0)
            filteredActions.add(RiskAction.endPhase());

        for (RiskAction action : filteredActions) {
            int wins = 0;
            int plays = 0;
            // TODO bias more promising moves here
            tree.add(new McRiskNode(tree.getNode().getGame().doAction(action), wins, plays));

        }
    }

    /**
     * Expand the tree with pruned actions via heuristics for the occupy Phase.
     */
    private void expandOccupyActions(Tree<McRiskNode> tree) {
        log.trace("Occupy Phase");
        // TODO: Reinforce troops for continuation of attack?

        Risk game = (Risk) tree.getNode().getGame();
        Set <RiskAction> actions = game.getPossibleActions();

        Set<RiskAction> filteredActions = new HashSet<>();

        Optional<RiskAction> optimalAction = actions.stream()
                // Always move with max amount of troops.
                .max(Comparator.comparingInt(RiskAction::troops));
        optimalAction.ifPresent(filteredActions::add);

        if (filteredActions.size() == 0 ) {
            filteredActions.addAll(actions);
            log.error("OccupyActions filteredActions.size() == 0");
        }

        for (RiskAction action : filteredActions) {
            int wins = 0;
            int plays = 0;
            // TODO bias more promising moves here
            tree.add(new McRiskNode(tree.getNode().getGame().doAction(action), wins, plays));
        }
    }

    /**
     * Expand the tree with pruned actions via heuristics for the fortify Phase.
     */
    private void expandFortifyActions(Tree<McRiskNode> tree) {
        log.trace("Fortify Phase");
        // Move troops from fortifyingId to fortifiedId.

        Risk game = (Risk) tree.getNode().getGame();
        RiskBoard board = game.getBoard();
        Set <RiskAction> actions = game.getPossibleActions();
        Set<RiskAction> filteredActions = new HashSet<>();

        // Prune actions.
        actions.stream()
                .filter(this::isRegularAction)
                // Only keep actions where fortifyingId has friendly neighbours.
                .filter(action -> board.neighboringEnemyTerritories(action.fortifyingId()).size() == 0)
                // Only keep actions where fortifiedId has at least one enemy neighbor.
                // TODO Relax condition to move troops from way behind to front lines over multiple moves.
                .filter(action -> board.neighboringEnemyTerritories(action.fortifiedId()).size() > 0)
                // Move max amount of troops.
                .max(Comparator.comparingInt(RiskAction::troops))
                .ifPresentOrElse(filteredActions::add, () -> filteredActions.addAll(actions));

        for (RiskAction action : filteredActions) {
            int wins = 0;
            int plays = 0;
            // TODO Bias more promising moves here.
            tree.add(new McRiskNode(tree.getNode().getGame().doAction(action), wins, plays));
        }
    }

    private boolean isRegularAction(RiskAction action) {
        return !(action.attackingId() < -1 || action.defendingId() < 0);
    }

    /*
     * Calculate territories which have a neighboring enemy territory.
     */
    private Set<Integer> getBorderTerritories(Risk game) {
        return game.getBoard().getTerritoriesOccupiedByPlayer(game.getCurrentPlayer()).stream()
                .filter(territoryId -> game.getBoard().neighboringEnemyTerritories(territoryId).size() > 0)
                .collect(Collectors.toSet());
    }

    /**
     * Return the Set of territories occupied by playerId for continentId on the given board.
     */
    private Set<Integer> getOccupiedTerritories(int playerId, int continentId, RiskBoard board) {
        return board.getTerritoriesOccupiedByPlayer(playerId).stream().filter(
                terrId -> board.getTerritories().get(terrId).getContinentId() == continentId
        ).collect(Collectors.toSet());
    }

    private Map<Integer, Set<RiskTerritory>> loadContinentTerritories(RiskBoard board) {
        Map<Integer, Set<RiskTerritory>> map = new HashMap<>(6);

        board.getTerritories().forEach((territoryId, territory) ->
                map.computeIfAbsent(territory.getContinentId(), k -> new HashSet<>()).add(territory));
        return map;
    }

    /**
     * Calculate heuristic h1 by following formula:
     *          h1[playerId] = #occupiedTerritories[playerId]/3 + continentBonuses[playerId].
     */
    @SuppressWarnings("IntegerDivisionInFloatingPointContext")
    private double[] heuristicUtility(Risk game) {
        double[] evaluation  = new double[game.getNumberOfPlayers()];

        for(int i = 0; i < game.getNumberOfPlayers(); i++) {
            // Integer Division to automatically get floor
            evaluation[i] = game.getBoard().getNrOfTerritoriesOccupiedByPlayer(i) / 3;

            for (int continent: game.getBoard().getContinentIds()) {
                if (continentConquered(i, continent, game)) {
                    evaluation[i] += game.getBoard().getContinentBonus(continent);
                }
            }

        }
        return evaluation;
    }

    private boolean continentConquered(int player, int continent, Risk game) {
        RiskBoard board = game.getBoard();
        return board.getContinents().containsKey(continent) && board.getTerritories().values().stream()
                .filter(t -> t.getContinentId() == continent)
                .allMatch(t -> t.getOccupantPlayerId() == player);
    }

    private void log_after_mcts_creation(TimeUnit timeUnit) {
        long elapsedTime = Math.max(1L, System.nanoTime() - START_TIME);
        log._deb_("\r");
        log.debf_("MCTS with %d simulations at confidence %.1f%%",
                mcTree.getNode().getPlays(),
                Util.percentage((mcTree.getNode()).getWins(),
                        mcTree.getNode().getPlays())
        );
        log._debugf(", done in %s with %s/simulation.",
                Util.convertUnitToReadableString(elapsedTime, TimeUnit.NANOSECONDS, timeUnit),
                Util.convertUnitToReadableString(elapsedTime / (long)Math.max(1, (mcTree.getNode()).getPlays()),
                        TimeUnit.NANOSECONDS, TimeUnit.MILLISECONDS)
        );
    }

    @Override
    public String toString() {
        return "Fun";
    }

    @Override
    public void tearDown() {
        // Do some tear down after the MATCH
    }

    @Override
    public void destroy() {
        // Do some tear down after the TOURNAMENT
    }
}
