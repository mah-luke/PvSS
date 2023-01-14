package fun.agent;

import at.ac.tuwien.ifs.sge.agent.AbstractGameAgent;
import at.ac.tuwien.ifs.sge.agent.GameAgent;
import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.game.Game;
import at.ac.tuwien.ifs.sge.game.risk.board.*;
import at.ac.tuwien.ifs.sge.util.Util;
import at.ac.tuwien.ifs.sge.util.tree.DoubleLinkedTree;
import at.ac.tuwien.ifs.sge.util.tree.Tree;
import org.checkerframework.checker.nullness.Opt;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class RiskAgentMcts extends AbstractGameAgent<Risk, RiskAction>
        implements GameAgent<Risk, RiskAction> {

    /*
    TODO: rewrite MCTS to use scalar values instead of boolean for T

    TODO:


     */

    private static final int RUN_LIMIT = 1_000;
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

        // Opening Book
        // Territory Selection Phase at the beginning of the game.
        if (board.isPlayerStillAlive(-1)) {
            RiskAction chosenAction = chooseSetupAction(game);
            if (chosenAction != null) return chosenAction;
        }


        // Use MCTS Approach after Opening Book
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
        while (!shouldStopComputation() && mcTree.getNode().getPlays() < RUN_LIMIT) {
           if (looped++ % printThreshold == 0) {
                log._deb("\r");
                log.debf_("MCTS with %d simulations at confidence %.1f%%",
                            mcTree.getNode().getPlays(),
                            Util.percentage(mcTree.getNode().getWins(),
                            mcTree.getNode().getPlays())
                );
            }

            Tree<McRiskNode> tree = mcSelection(mcTree);

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
            RiskAction bestAction = Collections.max(
                    mcTree.getChildren(),
                    gameMcTreeMoveComparator
            ).getNode().getGame().getPreviousAction();
            return bestAction;
        }

        // Fallback to greedy otherwise
        log._debug(". Could not find a move, choosing the next best greedy option.");
        Risk finalGame = game;
        return Collections.max(
                game.getPossibleActions(),
                (a1, a2) -> gameComparator.compare(finalGame.doAction(a1), finalGame.doAction(a2))
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

        // Priority for setting troops.
        // Brazil (10), Central America (2)
        // South America (9, 10, 11, 12), North America (0, 1, 2, 3, 4, 5, 6, 7, 8)
        // Iceland (14), then North Africa (24)
        // TODO fine-tune priority
        int[] territoryPriority = {10, 2, 9, 11, 12, 0, 1, 2, 3, 4, 5, 6, 7, 8, 14, 24};
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

    private void chooseReinforcementActions(Tree<McRiskNode> tree) {
        log.trace("Reinforcement Phase");
        // TODO: add front-line logic -> troops at conflict
        // Card trading is done in this phase
        // If incremental, trade in as late as possible. Prefer territories agent occupies for extra troops.
        // Set troops so that continents can be conquered for bonus.

        // Check which continent has many of playerId territories. Prioritize to conquer this continent.
        // Optional set priority of continents (South America > North America > ...),
        // depending on territory count within continent.
        // Hard-code South America, North America?

        Risk game = (Risk) tree.getNode().getGame();
        Set<RiskAction> actions = game.getPossibleActions();

        if (game.getBoard().isPlayerStillAlive(-1)) // TODO: add logic for setup phase after opening book
            return;

        // Trade-in only if you have to
        if (game.getBoard().hasToTradeInCards(game.getCurrentPlayer())) return;

        // TODO If player is close to conquering continent, try to set more troops there.

        // Calculate # border territories
        Set<Integer> territoryBorder = game.getBoard().getTerritoriesOccupiedByPlayer(playerId).stream()
                .filter(t -> game.getBoard().neighboringEnemyTerritories(t).size() > 0)
                .collect(Collectors.toSet());
        Set<RiskAction> specialActions = actions.stream()
                .filter(action -> !filterSpecialActions(action))
                .collect(Collectors.toSet());

        int max;
        Optional<RiskAction> maxReinforcement = (actions.stream()
                .filter(this::filterSpecialActions)
                .filter(action -> game.getBoard().neighboringEnemyTerritories(action.reinforcedId()).size() > 0)
                .max(Comparator.comparingInt(RiskAction::troops)));

        if (maxReinforcement.isPresent())
           max = maxReinforcement.get().troops();
        else max = 0;

        Set<RiskAction> filteredActions = actions.stream()
                .filter(this::filterSpecialActions)
                // Only reinforce territories with enemy neighbor.
                .filter(action -> game.getBoard().neighboringEnemyTerritories(action.reinforcedId()).size() > 0)
                // Reinforce some territories more heavily instead of spreading out.
                .filter(action ->
                        (action.troops() >= max / territoryBorder.size()))
                .collect(Collectors.toSet());
        // Add all actions if no filteredAction left.
        if (filteredActions.size() == 0)
            filteredActions.addAll(actions);

        for (RiskAction action : filteredActions) {
            int wins = 0;
            int plays = 0;
            // TODO bias more promising moves here
            if (wins > plays) wins = plays;
            tree.add(new McRiskNode(tree.getNode().getGame().doAction(action), wins, plays));
        }
    }

    private void chooseAttackActions(Tree<McRiskNode> tree) {
        log.trace("Attack Phase");
        // TODO: add one continent preference logic
        // Prefer to conquer continents for bonus.
        // Don't unblock enemy territory which has many troops.
        // Always attack if 2v1, 3v1, 3v2.
        // Even attack if attacking territory has less troops than defending territory.

        Risk game = (Risk) tree.getNode().getGame();
        RiskBoard board = game.getBoard();

        Set<RiskAction> actions = game.getPossibleActions();
        Set<RiskAction> filteredActions = actions.stream()
                .filter(this::filterSpecialActions)
                // Always attack with maximum amount of troops.
                .filter(action -> action.troops() == board.getMaxAttackingTroops(action.attackingId()))
                // Only keep actions where attackingTroops > defendingTroops.
                .filter(action -> board.getMaxAttackingTroops(action.attackingId()) >
                        Math.min(2, board.getTerritoryTroops(action.defendingId())))
                .collect(Collectors.toSet());
        // Add endPhase() if no attack left.
        if (filteredActions.size() == 0) filteredActions.add(RiskAction.endPhase());

        for (RiskAction action : filteredActions) {
            int wins = 0;
            int plays = 0;
            // TODO bias more promising moves here
            if (wins > plays) wins = plays;
            tree.add(new McRiskNode(tree.getNode().getGame().doAction(action), wins, plays));

        }
    }

    private void chooseOccupyActions(Tree<McRiskNode> tree) {
        log.trace("Occupy Phase");
        // TODO: Reinforce troops for continuation of attack?

        Risk game = (Risk) tree.getNode().getGame();

        Set<RiskAction> actions = game.getPossibleActions();
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
            if (wins > plays) wins = plays;
            tree.add(new McRiskNode(tree.getNode().getGame().doAction(action), wins, plays));
        }
    }

    private void chooseFortifyActions(Tree<McRiskNode> tree) {
        log.trace("Fortify Phase");
        // Move troops from fortifyingId to fortifiedId.

        Risk game = (Risk) tree.getNode().getGame();
        RiskBoard board = game.getBoard();

        Set<RiskAction> actions = game.getPossibleActions();
        // Filter out special actions.
         Set<RiskAction> specialActions = actions.stream()
                 .filter(action -> !filterSpecialActions(action))
                 .collect(Collectors.toSet());
        Set<RiskAction> filteredActions = new HashSet<>();

        // Prune actions.
         Optional<RiskAction> optimalAction = actions.stream()
                 .filter(this::filterSpecialActions)
                 // Only keep actions where fortifyingId has friendly neighbours.
                 .filter(action -> board.neighboringEnemyTerritories(action.fortifyingId()).size() == 0)
                 // Only keep actions where fortifiedId has at least one enemy neighbor.
                 // TODO Relax condition to move troops from way behind to front lines over multiple moves?
                 .filter(action -> board.neighboringEnemyTerritories(action.fortifiedId()).size() > 0)
                 // Move max amount of troops.
                 .max(Comparator.comparingInt(RiskAction::troops));
        optimalAction.ifPresent(filteredActions::add);

        // Add special actions.
        if (filteredActions.size() == 0)
            filteredActions.add(RiskAction.endPhase());

        for (RiskAction action : filteredActions) {
            int wins = 0;
            int plays = 0;
            // TODO bias more promising moves here
            if (wins > plays) wins = plays;
            tree.add(new McRiskNode(tree.getNode().getGame().doAction(action), wins, plays));
        }
    }

    private boolean filterSpecialActions(RiskAction action) {
        return !(action.attackingId() < -1 || action.defendingId() < 0);
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
        Risk baseGame = (Risk) tree.getNode().getGame();
        Risk game = baseGame;
        RiskBoard baseBoard = baseGame.getBoard();



        if (game.getUtilityValue(game.getCurrentPlayer()) == 1 || game.getPossibleActions().size() == 0) {
            // current player has already won
            return;
        }

        if (tree.isLeaf()) {
            // Exclude engine players from heuristics.
            if (baseGame.getCurrentPlayer() >= 0) {
                if (baseBoard.isReinforcementPhase()) {
                    chooseReinforcementActions(tree);
                } else if (baseBoard.isAttackPhase()) {
                    chooseAttackActions(tree);
                } else if (baseBoard.isOccupyPhase()) {
                    chooseOccupyActions(tree);
                } else if (baseBoard.isFortifyPhase()) {
                    chooseFortifyActions(tree);
                }
            }
            // TODO: add expansion logic for enemy player
            // Fallback
            if (tree.getChildren().isEmpty()) {
                log.deb("Expansion via Fallback: adding all possible actions.");
                for (RiskAction possibleAction : baseGame.getPossibleActions())
                    tree.add(new McRiskNode(baseGame, possibleAction));
            }
        }
    }

    /**
     * Base mcSimulation calling the othermcSimulations
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
        Risk game = (Risk) tree.getNode().getGame();
        int depth = 0;

        while(!game.isGameOver() && (depth++ % 31 != 0 || !shouldStopComputation())) {
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
                (depth++ % 31 != 0 || !shouldStopComputation())) {
            if (game.getCurrentPlayer() < 0)
                game = (Risk) game.doAction();
            else
                game = (Risk) game.doAction(Util.selectRandom(game.getPossibleActions(), random));
        }

        return mcHasWon(game);
    }

    /**
     * Decides whether player has won the game.
     * @param game
     * @return
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
            evaluation = continentUtility(game);
            score = Util.scoreOutOfUtility(evaluation, playerId);
        }

        boolean win = score == 1.0;
        boolean tie = score > 0.0;

        // Return true for ~50% of all ties and every win
        return win || tie && random.nextBoolean();
    }

    @SuppressWarnings("IntegerDivisionInFloatingPointContext")
    private double[] continentUtility(Risk game) {
        double[] evaluation  = new double[game.getNumberOfPlayers()];

        for(int i = 0; i < game.getNumberOfPlayers(); i++) {
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

