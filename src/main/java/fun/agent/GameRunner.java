package fun.agent;

import at.ac.tuwien.ifs.sge.agent.GameAgent;
import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.engine.game.Match;
import at.ac.tuwien.ifs.sge.engine.game.MatchResult;
import at.ac.tuwien.ifs.sge.game.Game;
import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;
import fun.agent.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class GameRunner {

    static final int COMP_TIME_LIMIT = 0;
    static final Logger log = LoggerFactory.getLogger(-2, "[main ");
    static final RiskAgentMcts riskAgentMcts = new RiskAgentMcts(log);

    public static void main(String[] args) {
        log.info(Arrays.toString(args));
        if (args.length > 0) {
            if (args[0].equals("match"))
                runMatch();
            else if (args[0].equals("action"))
                runAction();
        }
    }

    public static void runAction() {
        Risk game = new Risk();

        RiskAgentFun agent = new RiskAgentFun(
                LoggerFactory.getLogger(
                        -2,
                        "[agent "
                )
        );

        // TODO: bring game to desired state...

        log.info("Game before action: \n" + game.toTextRepresentation());
        log.info("Possible Actions: \n" + game.getPossibleActions());

        // choose next action
        RiskAction chosenAction = agent.computeNextAction(
                game,
                COMP_TIME_LIMIT,
                TimeUnit.SECONDS
        );

        log.info("Selected action: " + chosenAction);

        // do chosen action
        Risk next = (Risk) game.doAction(chosenAction);

        log.info("Game after action: \n" + next.toTextRepresentation());

    }

    public static void runMatch() {
        Game<RiskAction, RiskBoard> game = new Risk();
        GameAgent<Risk, RiskAction> agent = new RiskAgentFun(
                LoggerFactory.getLogger(-2, "[agent ")
        );
        GameAgent<Risk, RiskAction> agentOpponent =
                new RiskAgentFun(LoggerFactory.getLogger(-2, "[agentOpponent "));

        ExecutorService pool = Executors.newFixedThreadPool(
                Math.max(Runtime.getRuntime().availableProcessors(), 2)
        );

        List<GameAgent<Risk, RiskAction>> gameAgents = new ArrayList<>();
        gameAgents.add(agent);
        gameAgents.add(agentOpponent);

        Match<Risk, GameAgent<Risk, RiskAction>, RiskAction> match =
                new Match<>(
                        game,
                        gameAgents,
                        30,
                        TimeUnit.SECONDS,
                        true,
                        LoggerFactory.getLogger(-2, "[match "),
                        pool
                );

        MatchResult<Risk, GameAgent<Risk, RiskAction>> result = match.call();

        log.info("Result string:\n" + result);
        log.info("Results: " + Arrays.toString(result.getResult()));
        log.info("Duration: " + result.getDuration());
        log.info("Agents: " + result.getGameAgents());
//        log.info("Game: " + match.getGame().getActionRecords());

    }
}
