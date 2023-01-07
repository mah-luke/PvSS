package old.agent;

import at.ac.tuwien.ifs.sge.agent.GameAgent;
import at.ac.tuwien.ifs.sge.agent.HumanAgent;
import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.engine.game.MatchResult;
import at.ac.tuwien.ifs.sge.game.ActionRecord;
import at.ac.tuwien.ifs.sge.game.Game;
import at.ac.tuwien.ifs.sge.util.Util;
import at.ac.tuwien.ifs.sge.util.pair.ImmutablePair;
import at.ac.tuwien.ifs.sge.util.pair.Pair;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class DebugMatch<G extends Game<? extends A, ?>, E extends GameAgent<G, ? extends A>, A>
        implements Callable<MatchResult<G, E>> {
    private final boolean withHumanPlayer;
    private final boolean debug;
    private final long computationTime;
    private final TimeUnit timeUnit;
    private final Logger log;
    private final ExecutorService pool;
    private MatchResult<G, E> matchResult;
    private Game<A, ?> game;
    private final List<E> gameAgents;
    private String lastTextualRepresentation;
    private final int maxActions;

    public DebugMatch(Game<A, ?> game, List<E> gameAgents, long computationTime, TimeUnit timeUnit, boolean debug, Logger log, ExecutorService pool, int maxActions) {
        this.game = game;
        this.gameAgents = gameAgents;
        if (game.getNumberOfPlayers() != gameAgents.size()) {
            throw new IllegalArgumentException("Not the correct number of players");
        } else {
            boolean withHumanPlayer = false;

            GameAgent gameAgent;
            for(Iterator var11 = gameAgents.iterator(); var11.hasNext(); withHumanPlayer = withHumanPlayer || gameAgent instanceof HumanAgent) {
                gameAgent = (GameAgent)var11.next();
            }

            this.withHumanPlayer = withHumanPlayer;
            this.debug = debug;
            this.computationTime = computationTime;
            this.timeUnit = timeUnit;
            this.log = log;
            this.pool = pool;
            this.matchResult = null;
            this.maxActions = maxActions;
        }
    }

    public MatchResult<G, E> call() {
        long startTime = System.nanoTime();
        if (this.matchResult != null) {
            return this.matchResult;
        } else {
            this.lastTextualRepresentation = "";
            Deque<Pair<String, Future<Void>>> setUps = new ArrayDeque(this.gameAgents.size());

            int setUpsSize;
            for(setUpsSize = 0; setUpsSize < this.gameAgents.size(); ++setUpsSize) {
                E gameAgent = (E)this.gameAgents.get(setUpsSize);
                int finalSetUpsSize = setUpsSize;
                setUps.add(new ImmutablePair<>(gameAgent.toString(), this.pool.submit(() -> {
                    gameAgent.setUp(this.gameAgents.size(), finalSetUpsSize);
                    return null;
                })));
            }

            setUpsSize = setUps.size();
            this.log.traProcess_("Setting up agents", 0, setUpsSize);

            while(!setUps.isEmpty() && Thread.currentThread().isAlive() && !Thread.currentThread().isInterrupted()) {
                Pair<String, Future<Void>> setUp = setUps.pop();
                this.log._tra_("\r");
                this.log.traProcess_("Setting up agents", setUpsSize - setUps.size(), setUpsSize);

                try {
                    ((Future)setUp.getB()).get();
                } catch (InterruptedException var21) {
                    this.log._trace(", failed.");
                    this.log.debug("Interrupted while setting up agent ".concat((String)setUp.getA()));
                    this.log.printStackTrace(var21);
                } catch (ExecutionException var22) {
                    this.log.debug("Exception while setting up agent ".concat((String)setUp.getA()));
                    this.log.printStackTrace(var22);
                }
            }

            if (!setUps.isEmpty()) {
                this.log._trace(", failed.");
                this.log.warn("Following agents where not verified to be set up: ".concat((String)setUps.stream().map(Pair::getA).collect(Collectors.joining(", "))));
            } else {
                this.log._trace(", done.");
            }

            this.log.tracef("Computation time: %s", new Object[]{Util.convertUnitToMinimalString(this.computationTime, this.timeUnit)});
            if (this.maxActions < 2147483646) {
                this.log.tracef("Maximum number of actions: %s", new Object[]{this.maxActions});
                this.log.tracef("Max completion time: %s", new Object[]{Util.convertUnitToMinimalString(this.computationTime * (long)this.maxActions, this.timeUnit)});
            }

            int nrOfActions = 0;
            double[] result = new double[this.gameAgents.size()];
            Arrays.fill(result, 1.0);
            int lastPlayer = -1;
            boolean isHuman = false;

            while(!this.game.isGameOver() && nrOfActions < this.maxActions && Thread.currentThread().isAlive() && !Thread.currentThread().isInterrupted()) {
                int thisPlayer = this.game.getCurrentPlayer();
                if (thisPlayer >= 0) {
                    G playersGame = (G) this.game.getGame(thisPlayer);
                    isHuman = this.gameAgents.get(thisPlayer) instanceof HumanAgent;
                    if (lastPlayer != thisPlayer) {
                        Logger var10000 = this.log;
                        int var10001 = this.game.getCurrentPlayer();
                        var10000.info("Player " + var10001 + " - " + ((GameAgent)this.gameAgents.get(thisPlayer)).toString() + ":");
                        if (!this.withHumanPlayer || isHuman) {
                            this.printTextualRepresentation();
                        }
                    }

                    Future<A> actionFuture = (Future<A>) this.pool.submit(() -> {
                        return ((GameAgent)this.gameAgents.get(thisPlayer)).computeNextAction(playersGame, this.computationTime, this.timeUnit);
                    });
                    A action = null;

                    try {
                        action = actionFuture.get(this.computationTime, this.timeUnit);
                    } catch (InterruptedException var23) {
                        this.log.error("Interrupted.");
                    } catch (ExecutionException var24) {
                        this.log.error("Exception while executing computeNextAction().");
                        this.log.printStackTrace(var24);
                    } catch (TimeoutException var25) {
                        if (!isHuman && !this.debug) {
                            this.log.warn("Agent timeout.");
                        } else {
                            try {
                                action = actionFuture.get();
                            } catch (InterruptedException var19) {
                                this.log.error("Interrupted.");
                            } catch (ExecutionException var20) {
                                this.log.error("Exception while executing computeNextAction().");
                                this.log.printStackTrace(var20);
                            }
                        }
                    }

                    if (action == null) {
                        this.log.warn("No action given.");
                        result[thisPlayer] = -1.0;
                        this.matchResult = new MatchResult(this.gameAgents, startTime, System.nanoTime(), result);
                        this.log.debf_("%d actions: ", new Object[]{nrOfActions});
                        this.log._debug(ActionRecord.iterableToString(this.game.getActionRecords()));
                        return this.matchResult;
                    }

                    if (!isHuman) {
                        this.log._info_("> " + action.toString());
                    }

                    if (!this.game.isValidAction(action)) {
                        this.log.warn("Illegal action given.");

                        try {
                            this.game.doAction(action);
                        } catch (IllegalArgumentException var16) {
                            this.log.printStackTrace(var16);
                        }

                        result[thisPlayer] = -1.0;
                        this.matchResult = new MatchResult(this.gameAgents, startTime, System.nanoTime(), result);
                        this.log.debf_("%d actions: ", new Object[]{nrOfActions});
                        this.log._debug(ActionRecord.iterableToString(this.game.getActionRecords()));
                        return this.matchResult;
                    }

                    this.game = this.game.doAction(action);
                } else {
                    A action = this.game.determineNextAction();
                    if (action == null || !this.game.isValidAction(action)) {
                        this.log.err_("There is a programming error in the implementation of the game.");
                        if (action == null) {
                            this.log._error(" Next action is null.");
                        } else {
                            this.log._error(" Next action is invalid.");
                        }

                        throw new IllegalStateException("The current game violates the implementation contract");
                    }

                    this.game = this.game.doAction(action);
                }

                ++nrOfActions;
                lastPlayer = thisPlayer;
                if (this.game.getCurrentPlayer() >= 0 && isHuman && !(this.gameAgents.get(this.game.getCurrentPlayer()) instanceof HumanAgent) || thisPlayer == this.game.getCurrentPlayer()) {
                    this.printTextualRepresentation();
                }
            }

            long endTime = System.nanoTime();
            double[] utility = this.game.getGameUtilityValue();

            for(int i = 0; i < result.length; ++i) {
                result[i] = utility[i];
            }

            this.log._info_("-----");
            this.log.info("Game over.");
            this.log._info_(this.game.toTextRepresentation());
            this.log.inf(this.game.getNumberOfActions() + " actions ");
            List<ActionRecord<A>> actionRecords = this.game.getActionRecords();
            this.log._info(ActionRecord.iterableToString(actionRecords));
            Deque<Pair<String, Future<Void>>> tearDowns = new ArrayDeque(this.gameAgents.size());
            Iterator var13 = this.gameAgents.iterator();

            while(var13.hasNext()) {
                E gameAgent = (E) var13.next();
                tearDowns.add(new ImmutablePair(gameAgent.toString(), this.pool.submit(() -> {
                    gameAgent.tearDown();
                    return null;
                })));
            }

            int tearDownsSize = tearDowns.size();
            this.log.traProcess_("Tearing down agents", 0, tearDownsSize);

            while(!tearDowns.isEmpty() && Thread.currentThread().isAlive() && !Thread.currentThread().isInterrupted()) {
                Pair<String, Future<Void>> tearDown = (Pair)tearDowns.pop();
                this.log._tra_("\r");
                this.log.traProcess_("Tearing down agents", tearDownsSize - tearDowns.size(), tearDownsSize);

                try {
                    ((Future)tearDown.getB()).get();
                } catch (InterruptedException var17) {
                    this.log._trace(", failed.");
                    this.log.debug("Interrupted while tearing down agent ".concat((String)tearDown.getA()));
                    this.log.printStackTrace(var17);
                } catch (ExecutionException var18) {
                    this.log._trace(", failed.");
                    this.log.debug("Exception while tearing down agent ".concat((String)tearDown.getA()));
                    this.log.printStackTrace(var18);
                }
            }

            if (!tearDowns.isEmpty()) {
                this.log._trace(", failed.");
                this.log.warn("Following agents where not verified to be torn down: ".concat((String)tearDowns.stream().map(Pair::getA).collect(Collectors.joining(", "))));
            } else {
                this.log._trace(", done.");
            }

            this.matchResult = new MatchResult(this.gameAgents, startTime, endTime, result);
            return this.matchResult;
        }
    }

    private void printTextualRepresentation() {
        String textualRepresentation = this.game.getGame().toTextRepresentation();
        this.log._info_(textualRepresentation);
        this.lastTextualRepresentation = textualRepresentation;
    }

    public List<E> getGameAgents() {
        return this.gameAgents;
    }

    public Game<A, ?> getGame() {
        return this.game;
    }

}
