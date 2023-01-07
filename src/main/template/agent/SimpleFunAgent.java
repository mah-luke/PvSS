package old.agent;

import at.ac.tuwien.ifs.sge.agent.AbstractGameAgent;
import at.ac.tuwien.ifs.sge.agent.GameAgent;
import mctsagent.McGameNode;
import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.game.Game;
import at.ac.tuwien.ifs.sge.util.Util;
import at.ac.tuwien.ifs.sge.util.tree.Tree;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class SimpleFunAgent<G extends Game<A, ?>, A> extends AbstractGameAgent<G, A>
        implements GameAgent<G, A> {

    private Tree<McGameNode<A>> mcTree;
    private Comparator<Tree<McGameNode<A>>> gameMcTreeUCTComparator;
    private final double exploitationConstant = 1d;
    private Comparator<McGameNode<A>> gameMcNodePlayComparator;
    private Comparator<Tree<McGameNode<A>>> gameMcTreePlayComparator;
    private Comparator<McGameNode<A>> gameMcNodeWinComparator;
    private Comparator<Tree<McGameNode<A>>> gameMcTreeWinComparator;
    private Comparator<McGameNode<A>> gameMcNodeGameComparator;
    private Comparator<Tree<McGameNode<A>>> gameMcTreeGameComparator;
    private Comparator<Tree<McGameNode<A>>> gameMcTreeSelectionComparator;
    private Comparator<McGameNode<A>> gameMcNodeMoveComparator;
    private Comparator<Tree<McGameNode<A>>> gameMcTreeMoveComparator;


    public SimpleFunAgent(Logger log) {
        super(log);
    }

    @Override
    public void setUp(int numberOfPlayers, int playerId) {
        super.setUp(numberOfPlayers, playerId);

        mcTree.clear();
        mcTree.setNode(new McGameNode<A>());

        // UCT Comparator
        gameMcTreeUCTComparator = Comparator.comparingDouble((t) -> {
            return upperConfidenceBound(t, exploitationConstant);
        });

        // Play Comparators
        gameMcNodePlayComparator = Comparator.comparingInt(McGameNode::getPlays);
        gameMcTreePlayComparator = (t1, t2) -> gameMcNodePlayComparator.compare(t1.getNode(), t2.getNode());

        // Win Comparators
        gameMcNodeWinComparator = Comparator.comparingInt(McGameNode::getWins);
        gameMcTreeWinComparator = (t1, t2) -> gameMcNodeWinComparator.compare(t1.getNode(), t2.getNode());

        // Game Comparators
        gameMcNodeGameComparator = (n1, n2) -> gameComparator.compare(n1.getGame(), n2.getGame());
        gameMcTreeGameComparator = (t1, t2) -> gameMcNodeGameComparator.compare(t1.getNode(), t2.getNode());

        // Combine Comparators
        gameMcTreeSelectionComparator = gameMcTreeUCTComparator.thenComparing(gameMcTreeGameComparator);
        gameMcNodeMoveComparator = gameMcNodePlayComparator.thenComparing(gameMcNodeWinComparator).thenComparing(gameMcNodeGameComparator);
        gameMcTreeMoveComparator = (t1, t2) -> gameMcNodeMoveComparator.compare(t1.getNode(), t2.getNode());
    }

    @Override
    public A computeNextAction(G game, long computationTime, TimeUnit timeUnit){
        setTimers(computationTime, timeUnit);
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
        } else {
            log._trace("No");
            log.debf_("MCTS with %d simulations at confidence %.1f%%",
                    mcTree.getNode().getPlays(),
                    Util.percentage(mcTree.getNode().getWins(), mcTree.getNode().getPlays())
            );

            int looped = 0;
            int printThreshold = 1;

            while (!shouldStopComputation()) {
                if (looped++ % printThreshold == 0) {
                    log._deb("\r");
                    log.debf_("MCTS with %d simulations at configence %.1f%%",
                                mcTree.getNode().getPlays(),
                                Util.percentage(mcTree.getNode().getWins(),
                                mcTree.getNode().getPlays())
                    );
                }

                Tree<McGameNode<A>> tree = mcTree;
                tree = mcSelection(tree);
                mcExpansion(tree);
                boolean won = mcSimulation(tree, 128, 2);
                mcBackPropagation(tree, won);
                if (printThreshold < 97)
                    printThreshold = Math.max(
                            1, Math.min(97, Math.round((float) mcTree.getNode().getPlays() * 11.1111111F))
                    );
            }

            long elapsedTime = Math.max(1L, System.nanoTime() - START_TIME);
            log._deb_("\r");
            log.debf_("MCTS with %d siimulations at confidence %.1f%%",
                    mcTree.getNode().getPlays(),
                    Util.percentage((mcTree.getNode()).getWins(),
                    mcTree.getNode().getPlays())
            );
            log._debugf(", done in %s with %s/simulation.",
                    Util.convertUnitToReadableString(elapsedTime, TimeUnit.NANOSECONDS, timeUnit),
                    Util.convertUnitToReadableString(elapsedTime / (long)Math.max(1, (mcTree.getNode()).getPlays()),
                    TimeUnit.NANOSECONDS, TimeUnit.NANOSECONDS)
            );

            if (mcTree.isLeaf()) {
                log._debug(". Could not find a move, choosing the next best greedy option.");
                return Collections.max(
                        game.getPossibleActions(),
                        (a1, a2) -> gameComparator.compare(game.doAction(a1), game.doAction(a2))
                );
            } else {
                return Collections.max(
                        mcTree.getChildren(),
                        gameMcTreeMoveComparator
                ).getNode().getGame().getPreviousAction();
            }
        }
    }

    private boolean sortPromisingCandidates(Tree<McGameNode<A>> tree, Comparator<McGameNode<A>> comparator) {
        boolean isDetermined;
        for (isDetermined = true; !tree.isLeaf() && isDetermined; tree = tree.getChild(0)) {
            isDetermined = tree.getChildren().stream().allMatch(
                    c -> c.getNode().getGame().getCurrentPlayer() >= 0
            );

            if (((McGameNode<A>)tree.getNode()).getGame().getCurrentPlayer() == playerId)
                tree.sort(comparator);
            else
                tree.sort(comparator.reversed());
        }

        return isDetermined && tree.getNode().getGame().isGameOver();
    }

    private Tree<McGameNode<A>> mcSelection(Tree<McGameNode<A>> tree) {
        int depth = 0;

        while (!tree.isLeaf() && (depth++ % 31 != 0 || !shouldStopComputation())) {
            List<Tree<McGameNode<A>>> children = new ArrayList<>(tree.getChildren());

            if (tree.getNode().getGame().getCurrentPlayer() < 0) {
                A action = tree.getNode().getGame().determineNextAction();

                for (Tree<McGameNode<A>> child : children) {
                    if (((McGameNode<A>) child.getNode()).getGame().getPreviousAction().equals(action)) {
                        tree = child;
                        break;
                    }
                }
            } else
                tree = Collections.max(children, gameMcTreeSelectionComparator);
        }

        return tree;
    }

    private void mcExpansion(Tree<McGameNode<A>> tree) {
        if (tree.isLeaf()) {
            Game<A, ?> game = ((McGameNode<A>)tree.getNode()).getGame();
            Set<A> possibleActions = game.getPossibleActions();

            for (A possibleAction : possibleActions) {
                tree.add(new McGameNode<>(game, possibleAction));
            }
        }
    }

    private boolean mcSimulation(Tree<McGameNode<A>> tree, int simulationAtLeast, int proportion) {
        int simulationsDone = tree.getNode().getPlays();
        if (simulationsDone < simulationAtLeast && shouldStopComputation(proportion)) {
            int simulationsLeft = simulationAtLeast - simulationsDone;
            return mcSimulation(tree, nanosLeft() / (long) simulationsLeft);
        } else
            return simulationsDone == 0?
                    mcSimulation(tree, TIMEOUT / 2L - nanosElapsed()) : mcSimulation(tree);
    }

    private boolean mcSimulation(Tree<McGameNode<A>> tree) {
        Game<A, ?> game = tree.getNode().getGame();
        int depth = 0;

        while(!game.isGameOver() && (depth++ % 31 != 0 || !shouldStopComputation())) {
            if (game.getCurrentPlayer() < 0)
                game = game.doAction();
            else
                game = game.doAction(Util.selectRandom(game.getPossibleActions(), random));
        }

        return mcHasWon(game);
    }

    private boolean mcSimulation(Tree<McGameNode<A>> tree, long timeout) {
        long startTime = System.nanoTime();
        Game<A, ?> game = tree.getNode().getGame();
        int depth = 0;

        while(!game.isGameOver() && System.nanoTime() - startTime <= timeout && (depth++ % 31 != 0 || !shouldStopComputation())) {
            if (game.getCurrentPlayer() < 0)
                game = game.doAction();
            else
                game = game.doAction(Util.selectRandom(game.getPossibleActions(), random));
        }

        return mcHasWon(game);
    }

    private boolean mcHasWon(Game<A, ?> game) {
        double[] evaluation = game.getGameUtilityValue();
        double score = Util.scoreOutOfUtility(evaluation, playerId);
        if (!game.isGameOver() && score > 0.0) {
            evaluation = game.getGameHeuristicValue();
            score = Util.scoreOutOfUtility(evaluation, playerId);
        }

        boolean win = score == 1.0;
        boolean tie = score > 0.0;
        return win || tie && random.nextBoolean();
    }

    private void mcBackPropagation(Tree<McGameNode<A>> tree, boolean win) {
        int depth = 0;

        while (!tree.isRoot() && (depth++ % 31 != 0 || !shouldStopComputation())) {
            tree = tree.getParent();
            tree.getNode().incPlays();
            if (win) tree.getNode().incWins();
        }
    }

    private double upperConfidenceBound(Tree<McGameNode<A>> tree, double c) {
        double w = tree.getNode().getWins();
        double n = Math.max(tree.getNode().getPlays(), 1);
        double N = n;
        if (!tree.isRoot()) {
            N = (tree.getParent().getNode()).getPlays();
        }

        return w / n + c * Math.sqrt(Math.log(N) / n);
    }



}
