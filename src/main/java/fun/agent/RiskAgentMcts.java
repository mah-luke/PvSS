package fun.agent;

import at.ac.tuwien.ifs.sge.agent.AbstractGameAgent;
import at.ac.tuwien.ifs.sge.agent.GameAgent;
import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.game.Game;
import at.ac.tuwien.ifs.sge.game.risk.board.*;
import at.ac.tuwien.ifs.sge.util.Util;
import at.ac.tuwien.ifs.sge.util.tree.DoubleLinkedTree;
import at.ac.tuwien.ifs.sge.util.tree.Tree;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class RiskAgentMcts extends AbstractGameAgent<Risk, RiskAction>
        implements GameAgent<Risk, RiskAction> {

    /*
    TODO: rewrite MCTS to use scalar values instead of boolean for T

    TODO:


     */

    private static int INSTANCE_NR_COUNTER;
    private Tree<McRiskNode> mcTree;
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


    public RiskAgentMcts(Logger log) {
        this(Math.sqrt(2.0), log);
    }

    public RiskAgentMcts(double exploitationConstant, Logger log) {
        /* instantiates a AbstractGameAgent so that shouldStopComputation() returns true after 3/4ths of
         * the given time. However, if it can, it will try to compute at least 5 seconds.
         */
        super(3D / 4D, 5, TimeUnit.SECONDS, log);

        //Note: log can be effectively ignored, however it honors the flags of the engine.

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

    public RiskAction computeNextAction(Risk game, long computationTime, TimeUnit timeUnit) {
        super.setTimers(computationTime, timeUnit); // Makes sure shouldStopComputation() works

        RiskBoard board = game.getBoard();

        if (continentTerritories == null) continentTerritories = loadContinentTerritories(board);


        log.tra_("Searching for root of tree");
        boolean foundRoot = Util.findRoot(mcTree, game);

        if (foundRoot) log._trace(", done.");
        else log._trace(", failed.");

        log.tra_("Check if best move will eventually end game: ");
        if (sortPromisingCandidates(mcTree, gameMcNodeMoveComparator.reversed())) {
            log._trace("Yes");
            return Collections
                    .max(mcTree.getChildren(), gameMcTreeMoveComparator)
                    .getNode()
                    .getGame()
                    .getPreviousAction();
        }

        log._trace("No");
        log.debf_("MCTS with %d simulations at confidence %.1f%%",
                mcTree.getNode().getPlays(),
                Util.percentage(mcTree.getNode().getWins(), mcTree.getNode().getPlays())
        );

        int looped = 0;
        int printThreshold = 1;

        // Run mcts tree creation loop until time runs out
        while (!shouldStopComputation()) {
            if (looped++ % printThreshold == 0) {
                log._deb("\r");
                log.debf_("MCTS with %d simulations at confidence %.1f%%",
                            mcTree.getNode().getPlays(),
                            Util.percentage(mcTree.getNode().getWins(),
                            mcTree.getNode().getPlays())
                );
            }

            Tree<McRiskNode> tree = mcSelection(mcTree);

            // Territory Selection Phase at the beginning of the game.
            // Quasi "opening book".
            if (board.isPlayerStillAlive(-1)) {
                RiskAction chosenAction = chooseSetupAction(game);
                if (chosenAction != null) return chosenAction;
            }

            mcExpansion(tree);
            boolean won = mcSimulation(tree, 128, 2);
            mcBackPropagation(tree, won);
            if (printThreshold < 97)
                printThreshold = Math.max(
                        1, Math.min(97, Math.round((float) mcTree.getNode().getPlays() * 11.1111111F))
                );
        }

        log_after_mcts_creation(timeUnit);



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

    private RiskAction chooseSetupAction(Risk game) {
        log.info("Board not yet fully occupied");

        // Check if continent has < 3 free territories and no territories of playerId.
        for (Integer continentId : game.getBoard().getContinentIds()) {
            Set<Integer> freeTerritories = getOccupiedTerritories(-1, continentId, game.getBoard());
            if (freeTerritories.size() < 3 &&
                    getOccupiedTerritories(playerId, continentId, game.getBoard()).size() == 0
            )
                // select action that sets an army in an empty territory in continentId
                for (Integer territory: freeTerritories){
                    // TODO Don't take first option and choose better?
                    RiskAction action = RiskAction.select(territory);
                    if (game.isValidAction(action)) {
                        return action;
                    }
                    else throw new RuntimeException("Not valid action.");
                }
        }

        // Central America most important?
        // Go for South America, then North America
        int[] territoryPriority = {10, 9, 11, 12, 2, 0, 1, 2, 3, 4, 5, 6, 7, 8}; //TODO fine-tune priority
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

    private Set<RiskAction> chooseReinforcementAction(Risk game) {
        log.info("Reinforcement Phase");
        // TODO: add frontline logic -> troops at conflict
        // Card trading is done in this phase
        // If incremental, trade in as late as possible. Prefer territories agent occupies for extra troops.
        // Set troops so that continents can be conquered for bonus.
        return null;
    }

    private Set<RiskAction> chooseAttackAction(Risk game) {
        log.info("Reinforcement Phase");
        // TODO: add one continent preference logic
        // Prefer to conquer continents for bonus.
        // Don't unblock enemy territory which has many troops.
        // Always attack if 2v1, 3v1, 3v2.
        // Even attack if attacking territory has less troops than defending territory.
        return null;
    }
    private Set<RiskAction> chooseOccupyAction(Risk game) {
        log.info("Reinforcement Phase");
        // TODO: Reinforce troops for continuation of attack?
        return null;
    }

    private Set<RiskAction> chooseFortifyAction(Risk game) {
        log.info("Reinforcement Phase");
        // TODO: Move troops to conflict/reinforce occupied territories.
        // Simple approach: sort occupied territories descending by troop count.
        // If territory has conflict (any neighbor is occupied by different player) do nothing.
        // If territory has only neighbors of own player, move them.
        return null;
    }

    /**
     * Return the number of territories occupied by playerId for continentId.
     *
     * @param playerId
     * @param continentId
     * @param board
     * @return
     */
    private Set<Integer> getOccupiedTerritories(int playerId, int continentId, RiskBoard board) {
        return board.getTerritoriesOccupiedByPlayer(playerId).stream().filter(
                    terrId -> board.getTerritories().get(terrId).getContinentId() == continentId
                ).collect(Collectors.toSet());
    }

    private Map<Integer, RiskTerritory> loadConnectedTerritories(RiskBoard board) {
        Map<Integer, Set<RiskTerritory>> map = new HashMap<>(board.getTerritories().size());


        board.neighboringTerritories(12);
        throw new RuntimeException("Don't call this method");
    }

    private Map<Integer, Set<RiskTerritory>> loadContinentTerritories(RiskBoard board) {
        Map<Integer, Set<RiskTerritory>> map = new HashMap<>(6);

        board.getTerritories().forEach((territoryId, territory) ->
                map.computeIfAbsent(territory.getContinentId(), k -> new HashSet<>()).add(territory));
        return map;
    }

    private boolean sortPromisingCandidates(Tree<McRiskNode> tree, Comparator<McRiskNode> comparator) {
        boolean isDetermined;
        for (isDetermined = true; !tree.isLeaf() && isDetermined; tree = tree.getChild(0)) {
            isDetermined = tree.getChildren().stream().allMatch(
                    c -> c.getNode().getGame().getCurrentPlayer() >= 0
            );

            if (tree.getNode().getGame().getCurrentPlayer() == playerId)
                tree.sort(comparator);
            else
                tree.sort(comparator.reversed());
        }

        return isDetermined && tree.getNode().getGame().isGameOver();
    }

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

    private void mcExpansion(Tree<McRiskNode> tree) {
        Set<RiskAction> possibleActions;
        Risk game = (Risk) tree.getNode().getGame();
        RiskBoard board = game.getBoard();

        if (tree.isLeaf()) {
            // TODO smarter expansion, don't add all
            if (board.isReinforcementPhase()) {
                possibleActions = chooseReinforcementAction(game);
            } else if (board.isAttackPhase()) {
                possibleActions = chooseAttackAction(game);
            } else if (board.isOccupyPhase()) {
                possibleActions = chooseOccupyAction(game);
            } else if (board.isFortifyPhase()) {
                possibleActions = chooseFortifyAction(game);
            } else {
                // TODO: Remove RuntimeException on deployment.
                throw new RuntimeException("Phase unknown");
                // Fallback
                // possibleActions = game.getPossibleActions();
            }


            for (RiskAction possibleAction : possibleActions) {
                tree.add(new McRiskNode(game, possibleAction));
            }
        }
    }

    /**
     * Base mcSimulation calling the other mcSimulations
     * @param tree
     * @param simulationAtLeast
     * @param proportion
     * @return
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
        Game<RiskAction, ?> game = tree.getNode().getGame();
        int depth = 0;

        while(!game.isGameOver() && (depth++ % 31 != 0 || !shouldStopComputation())) {
            if (game.getCurrentPlayer() < 0)
                game = game.doAction();
            else
                game = game.doAction(Util.selectRandom(game.getPossibleActions(), random));
        }

        return mcHasWon(game);
    }

    private boolean mcSimulation(Tree<McRiskNode> tree, long timeout) {
        long startTime = System.nanoTime();
        Game<RiskAction, ?> game = tree.getNode().getGame();
        int depth = 0;

        while(!game.isGameOver() && System.nanoTime() - startTime <= timeout &&
                (depth++ % 31 != 0 || !shouldStopComputation())) {
            if (game.getCurrentPlayer() < 0)
                game = game.doAction();
            else
                game = game.doAction(Util.selectRandom(game.getPossibleActions(), random));
        }

        return mcHasWon(game);
    }

    /**
     * Decides whether player has won the game.
     * @param game
     * @return
     */
    private boolean mcHasWon(Game<RiskAction, ?> game) {

        // array with 1 for win and 0 otherwise
        double[] evaluation = game.getGameUtilityValue();

        // val = evaluation[playerId], n = evaluation.length, max = max(evaluation - val)
        // (val >= max? 1/n : 0) + (val > max? (n-1)/n : 0)
        //
        // val <  max   => 0    -> Loose
        // val == max   => 1/n  -> Tie
        // val >  max   => 1    -> Win
        double score = Util.scoreOutOfUtility(evaluation, playerId);

        if (!game.isGameOver() && score > 0.0) {
            evaluation = game.getGameHeuristicValue();
            score = Util.scoreOutOfUtility(evaluation, playerId);
        }

        boolean win = score == 1.0;
        boolean tie = score > 0.0;

        // Return true for ~50% of all ties and every win
        return win || tie && random.nextBoolean();
    }

    private void mcBackPropagation(Tree<McRiskNode> tree, boolean win) {
        int depth = 0;

        while (!tree.isRoot() && (depth++ % 31 != 0 || !shouldStopComputation())) {
            tree = tree.getParent();
            tree.getNode().incPlays();
            if (win) tree.getNode().incWins();
        }
    }

    private double upperConfidenceBound(Tree<McRiskNode> tree, double c) {
        double w = tree.getNode().getWins();
        double n = Math.max(tree.getNode().getPlays(), 1);
        double N = n;
        if (!tree.isRoot()) {
            N = (tree.getParent().getNode()).getPlays();
        }

        return w / n + c * Math.sqrt(Math.log(N) / n);
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
    public void tearDown() {
        // Do some tear down after the MATCH
    }

    @Override
    public void destroy() {
        // Do some tear down after the TOURNAMENT
    }
}

