package org.fun;

import at.ac.tuwien.ifs.sge.agent.AbstractGameAgent;
import at.ac.tuwien.ifs.sge.agent.GameAgent;
import at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode;
import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.game.Game;
import at.ac.tuwien.ifs.sge.util.Util;
import at.ac.tuwien.ifs.sge.util.tree.Tree;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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
