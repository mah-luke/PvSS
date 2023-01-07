package mctsagent;

import at.ac.tuwien.ifs.sge.agent.AbstractGameAgent;
import at.ac.tuwien.ifs.sge.agent.GameAgent;
import at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode;
import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.game.Game;
import at.ac.tuwien.ifs.sge.util.Util;
import at.ac.tuwien.ifs.sge.util.tree.DoubleLinkedTree;
import at.ac.tuwien.ifs.sge.util.tree.Tree;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MctsAgent<G extends Game<A, ?>, A> extends AbstractGameAgent<G, A> implements GameAgent<G, A> {
    private static final int MAX_PRINT_THRESHOLD = 97;
    private static int INSTANCE_NR_COUNTER = 1;
    private final int instanceNr;
    private final double exploitationConstant;
    private Comparator<Tree<at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode<A>>> gameMcTreeUCTComparator;
    private Comparator<Tree<at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode<A>>> gameMcTreeSelectionComparator;
    private Comparator<Tree<at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode<A>>> gameMcTreePlayComparator;
    private Comparator<at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode<A>> gameMcNodePlayComparator;
    private Comparator<Tree<at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode<A>>> gameMcTreeWinComparator;
    private Comparator<at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode<A>> gameMcNodeWinComparator;
    private Comparator<Tree<at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode<A>>> gameMcTreeMoveComparator;
    private Comparator<at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode<A>> gameMcNodeMoveComparator;
    private Comparator<at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode<A>> gameMcNodeGameComparator;
    private Comparator<Tree<at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode<A>>> gameMcTreeGameComparator;
    private Tree<at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode<A>> mcTree;

    public MctsAgent() {
        this((Logger)null);
    }

    public MctsAgent(Logger log) {
        this(Math.sqrt(2.0), log);
    }

    public MctsAgent(double exploitationConstant, Logger log) {
        super(log);
        this.exploitationConstant = exploitationConstant;
        this.mcTree = new DoubleLinkedTree();
        this.instanceNr = INSTANCE_NR_COUNTER++;
    }

    public void setUp(int numberOfPlayers, int playerId) {
        super.setUp(numberOfPlayers, playerId);
        this.mcTree.clear();
        this.mcTree.setNode(new at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode());
        this.gameMcTreeUCTComparator = Comparator.comparingDouble((t) -> {
            return this.upperConfidenceBound(t, this.exploitationConstant);
        });
        this.gameMcNodePlayComparator = Comparator.comparingInt(at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode::getPlays);
        this.gameMcTreePlayComparator = (o1, o2) -> {
            return this.gameMcNodePlayComparator.compare((at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode)o1.getNode(), (at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode)o2.getNode());
        };
        this.gameMcNodeWinComparator = Comparator.comparingInt(at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode::getWins);
        this.gameMcTreeWinComparator = (o1, o2) -> {
            return this.gameMcNodeWinComparator.compare((at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode)o1.getNode(), (at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode)o2.getNode());
        };
        this.gameMcNodeGameComparator = (o1, o2) -> {
            return this.gameComparator.compare(o1.getGame(), o2.getGame());
        };
        this.gameMcTreeGameComparator = (o1, o2) -> {
            return this.gameMcNodeGameComparator.compare((at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode)o1.getNode(), (at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode)o2.getNode());
        };
        this.gameMcTreeSelectionComparator = this.gameMcTreeUCTComparator.thenComparing(this.gameMcTreeGameComparator);
        this.gameMcNodeMoveComparator = this.gameMcNodePlayComparator.thenComparing(this.gameMcNodeWinComparator).thenComparing(this.gameMcNodeGameComparator);
        this.gameMcTreeMoveComparator = (o1, o2) -> {
            return this.gameMcNodeMoveComparator.compare((at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode)o1.getNode(), (at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode)o2.getNode());
        };
    }

    public A computeNextAction(G game, long computationTime, TimeUnit timeUnit) {
        super.setTimers(computationTime, timeUnit);
        this.log.tra_("Searching for root of tree");
        boolean foundRoot = Util.findRoot(this.mcTree, game);
        if (foundRoot) {
            this.log._trace(", done.");
        } else {
            this.log._trace(", failed.");
        }

        this.log.tra_("Check if best move will eventually end game: ");
        if (this.sortPromisingCandidates(this.mcTree, this.gameMcNodeMoveComparator.reversed())) {
            this.log._trace("Yes");
            return ((at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode)((Tree) Collections.max(this.mcTree.getChildren(), this.gameMcTreeMoveComparator)).getNode()).getGame().getPreviousAction();
        } else {
            this.log._trace("No");
            int looped = 0;
            this.log.debf_("MCTS with %d simulations at confidence %.1f%%", new Object[]{((at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode)this.mcTree.getNode()).getPlays(), Util.percentage(((at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode)this.mcTree.getNode()).getWins(), ((at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode)this.mcTree.getNode()).getPlays())});
            int printThreshold = 1;

            while(!this.shouldStopComputation()) {
                if (looped++ % printThreshold == 0) {
                    this.log._deb_("\r");
                    this.log.debf_("MCTS with %d simulations at confidence %.1f%%", new Object[]{((at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode)this.mcTree.getNode()).getPlays(), Util.percentage(((at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode)this.mcTree.getNode()).getWins(), ((at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode)this.mcTree.getNode()).getPlays())});
                }

                Tree<at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode<A>> tree = this.mcTree;
                tree = this.mcSelection(tree);
                this.mcExpansion(tree);
                boolean won = this.mcSimulation(tree, 128, 2);
                this.mcBackPropagation(tree, won);
                if (printThreshold < 97) {
                    printThreshold = Math.max(1, Math.min(97, Math.round((float)((at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode)this.mcTree.getNode()).getPlays() * 11.111111F)));
                }
            }

            long elapsedTime = Math.max(1L, System.nanoTime() - this.START_TIME);
            this.log._deb_("\r");
            this.log.debf_("MCTS with %d simulations at confidence %.1f%%", new Object[]{((at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode)this.mcTree.getNode()).getPlays(), Util.percentage(((at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode)this.mcTree.getNode()).getWins(), ((at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode)this.mcTree.getNode()).getPlays())});
            this.log._debugf(", done in %s with %s/simulation.", new Object[]{Util.convertUnitToReadableString(elapsedTime, TimeUnit.NANOSECONDS, timeUnit), Util.convertUnitToReadableString(elapsedTime / (long)Math.max(1, ((at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode)this.mcTree.getNode()).getPlays()), TimeUnit.NANOSECONDS, TimeUnit.NANOSECONDS)});
            if (this.mcTree.isLeaf()) {
                this.log._debug(". Could not find a move, choosing the next best greedy option.");
                return Collections.max(game.getPossibleActions(), (o1, o2) -> {
                    return this.gameComparator.compare(game.doAction(o1), game.doAction(o2));
                });
            } else {
                return ((at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode)((Tree)Collections.max(this.mcTree.getChildren(), this.gameMcTreeMoveComparator)).getNode()).getGame().getPreviousAction();
            }
        }
    }

    private boolean sortPromisingCandidates(Tree<at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode<A>> tree, Comparator<at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode<A>> comparator) {
        boolean isDetermined;
        for(isDetermined = true; !tree.isLeaf() && isDetermined; tree = tree.getChild(0)) {
            isDetermined = tree.getChildren().stream().allMatch((c) -> {
                return ((at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode)c.getNode()).getGame().getCurrentPlayer() >= 0;
            });
            if (((at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode)tree.getNode()).getGame().getCurrentPlayer() == this.playerId) {
                tree.sort(comparator);
            } else {
                tree.sort(comparator.reversed());
            }
        }

        return isDetermined && ((at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode)tree.getNode()).getGame().isGameOver();
    }

    private Tree<at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode<A>> mcSelection(Tree<at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode<A>> tree) {
        int depth = 0;

        while(!tree.isLeaf() && (depth++ % 31 != 0 || !this.shouldStopComputation())) {
            List<Tree<at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode<A>>> children = new ArrayList(tree.getChildren());
            if (((at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode)tree.getNode()).getGame().getCurrentPlayer() < 0) {
                A action = ((at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode)tree.getNode()).getGame().determineNextAction();
                Iterator var5 = children.iterator();

                while(var5.hasNext()) {
                    Tree<at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode<A>> child = (Tree)var5.next();
                    if (((at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode)child.getNode()).getGame().getPreviousAction().equals(action)) {
                        tree = child;
                        break;
                    }
                }
            } else {
                tree = (Tree)Collections.max(children, this.gameMcTreeSelectionComparator);
            }
        }

        return tree;
    }

    private void mcExpansion(Tree<at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode<A>> tree) {
        if (tree.isLeaf()) {
            Game<A, ?> game = ((at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode)tree.getNode()).getGame();
            Set<A> possibleActions = game.getPossibleActions();
            Iterator var4 = possibleActions.iterator();

            while(var4.hasNext()) {
                A possibleAction = var4.next();
                tree.add(new at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode(game, possibleAction));
            }
        }

    }

    private boolean mcSimulation(Tree<at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode<A>> tree, int simulationsAtLeast, int proportion) {
        int simulationsDone = ((at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode)tree.getNode()).getPlays();
        if (simulationsDone < simulationsAtLeast && this.shouldStopComputation(proportion)) {
            int simulationsLeft = simulationsAtLeast - simulationsDone;
            return this.mcSimulation(tree, this.nanosLeft() / (long)simulationsLeft);
        } else {
            return simulationsDone == 0 ? this.mcSimulation(tree, this.TIMEOUT / 2L - this.nanosElapsed()) : this.mcSimulation(tree);
        }
    }

    private boolean mcSimulation(Tree<at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode<A>> tree) {
        Game<A, ?> game = ((at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode)tree.getNode()).getGame();
        int depth = 0;

        while(!game.isGameOver() && (depth++ % 31 != 0 || !this.shouldStopComputation())) {
            if (game.getCurrentPlayer() < 0) {
                game = game.doAction();
            } else {
                game = game.doAction(Util.selectRandom(game.getPossibleActions(), this.random));
            }
        }

        return this.mcHasWon(game);
    }

    private boolean mcSimulation(Tree<at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode<A>> tree, long timeout) {
        long startTime = System.nanoTime();
        Game<A, ?> game = ((at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode)tree.getNode()).getGame();
        int depth = 0;

        while(!game.isGameOver() && System.nanoTime() - startTime <= timeout && (depth++ % 31 != 0 || !this.shouldStopComputation())) {
            if (game.getCurrentPlayer() < 0) {
                game = game.doAction();
            } else {
                game = game.doAction(Util.selectRandom(game.getPossibleActions(), this.random));
            }
        }

        return this.mcHasWon(game);
    }

    private boolean mcHasWon(Game<A, ?> game) {
        double[] evaluation = game.getGameUtilityValue();
        double score = Util.scoreOutOfUtility(evaluation, this.playerId);
        if (!game.isGameOver() && score > 0.0) {
            evaluation = game.getGameHeuristicValue();
            score = Util.scoreOutOfUtility(evaluation, this.playerId);
        }

        boolean win = score == 1.0;
        boolean tie = score > 0.0;
        win = win || tie && this.random.nextBoolean();
        return win;
    }

    private void mcBackPropagation(Tree<at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode<A>> tree, boolean win) {
        int depth = 0;

        while(!tree.isRoot() && (depth++ % 31 != 0 || !this.shouldStopComputation())) {
            tree = tree.getParent();
            ((at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode)tree.getNode()).incPlays();
            if (win) {
                ((at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode)tree.getNode()).incWins();
            }
        }

    }

    private double upperConfidenceBound(Tree<at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode<A>> tree, double c) {
        double w = (double)((at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode)tree.getNode()).getWins();
        double n = (double)Math.max(((at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode)tree.getNode()).getPlays(), 1);
        double N = n;
        if (!tree.isRoot()) {
            N = (double)((McGameNode)tree.getParent().getNode()).getPlays();
        }

        return w / n + c * Math.sqrt(Math.log(N) / n);
    }

    public String toString() {
        return this.instanceNr <= 1 && INSTANCE_NR_COUNTER <= 2 ? "MctsAgent" : String.format("%s%d", "MctsAgent#", this.instanceNr);
    }
}
