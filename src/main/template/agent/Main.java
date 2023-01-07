package old.agent;

import at.ac.tuwien.ifs.sge.agent.GameAgent;
import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.engine.game.MatchResult;
import at.ac.tuwien.ifs.sge.game.Game;
import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    static final int COMP_TIME_LIMIT = 0;
    static final Logger log = LoggerFactory.getLogger(-2, "[main ");

    public static void main(String[] args) {
        runMatch();
//        runSingleAction();
    }

    private static void runSingleAction() {
        Game<RiskAction, RiskBoard> game = new Risk();

        SimpleFunAgent<Game<RiskAction, RiskBoard>, RiskAction> agent =
                new SimpleFunAgent<>(LoggerFactory.getLogger(-2, "[agent "));

        // TODO: bring game to desired state...

        log.info("Game before action: \n" + game.toTextRepresentation());
        log.info("Possible Actions: \n" + game.getPossibleActions());

        // choose next action
        RiskAction action = agent.computeNextAction(game, COMP_TIME_LIMIT, TimeUnit.SECONDS);

        log.info("Selected action: " + action);

        // run chosen action on game
        Game<RiskAction, RiskBoard> next = game.doAction(action);

        log.info("Game after action: \n" + next.toTextRepresentation());
    }

    private static void runMatch() {
        Game<RiskAction, RiskBoard> game = new Risk();
        SimpleFunAgent<Game<RiskAction, RiskBoard>, RiskAction> agent =
                new SimpleFunAgent<>(LoggerFactory.getLogger(-2, "[agent "));

        GameAgent<Game<RiskAction, RiskBoard>, RiskAction> agentOpponent =
                // new AlphaBetaAgent<>(2,  LoggerFactory.getLogger(-2, "[agentOpponent "));
                new MctsAgent<>(Math.sqrt(32), LoggerFactory.getLogger(-2, "[agentOpponent "));

        ExecutorService pool = Executors.newFixedThreadPool(Math.max(Runtime.getRuntime().availableProcessors(), 2));
        List<GameAgent<Game<RiskAction, RiskBoard>, RiskAction>> gameAgents = new ArrayList<>();

        gameAgents.add(agent);
        gameAgents.add(agentOpponent);

        DebugMatch<Game<RiskAction, RiskBoard>, GameAgent<Game<RiskAction, RiskBoard>, RiskAction>, RiskAction> match =
                new DebugMatch<>(game,
                        gameAgents,
                        30,
                        TimeUnit.SECONDS,
                        true,
                        LoggerFactory.getLogger(-2, "[match "),
                        pool,
                        1_000
                );

        MatchResult<Game<RiskAction, RiskBoard>, GameAgent<Game<RiskAction, RiskBoard>, RiskAction>> result = match.call();

        log.info("Result string:\n" + result);
        log.info("Results: " + Arrays.toString(result.getResult()));
        log.info("Duration: " + result.getDuration());
        log.info("Agents: " + result.getGameAgents());
        log.info("Game: " + match.getGame().getActionRecords());
    }

}
