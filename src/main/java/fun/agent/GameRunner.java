package fun.agent;

import at.ac.tuwien.ifs.sge.agent.GameAgent;
import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.engine.game.MatchResult;
import at.ac.tuwien.ifs.sge.game.ActionRecord;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GameRunner {

    static final int COMP_TIME_LIMIT = 0;
    static final Logger log = LoggerFactory.getLogger(-3, "[main ");
    static final RiskAgentMcts riskAgentMcts = new RiskAgentMcts(
            LoggerFactory.getLogger(-2, "[agent ")
    );

    static final RiskAgentFun riskAgentGreedy = new RiskAgentFun(
            LoggerFactory.getLogger(-2, "[agentOpponent ")
    );

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
        GameAgent<Risk, RiskAction> agent = riskAgentMcts;
        agent.setUp(1,0);

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
        GameAgent<Risk, RiskAction> agent = riskAgentMcts;
        GameAgent<Risk, RiskAction> agentOpponent = riskAgentGreedy;

        ExecutorService pool = Executors.newFixedThreadPool(
                Math.max(Runtime.getRuntime().availableProcessors(), 2)
        );

        List<GameAgent<Risk, RiskAction>> gameAgents = new ArrayList<>(2);
        gameAgents.add(agent);
        gameAgents.add(agentOpponent);



        String setUpPlayerDead = "<0, -(1)->10>, <1, -(1)->32>, <0, -(1)->2>, <1, -(1)->1>, <0, -(1)->9>, <1, -(1)->34>, <0, -(1)->11>, <1, -(1)->3>, <0, -(1)->12>, <1, -(1)->36>, <0, -(1)->0>, <1, -(1)->5>, <0, -(1)->4>, <1, -(1)->38>, <0, -(1)->6>, <1, -(1)->7>, <0, -(1)->8>, <1, -(1)->40>, <0, -(1)->39>, <1, -(1)->33>, <0, -(1)->14>, <1, -(1)->35>, <0, -(1)->24>, <1, -(1)->37>, <0, -(1)->30>, <1, -(1)->41>, <0, -(1)->27>, <1, -(1)->13>, <0, -(1)->16>, <1, -(1)->15>, <0, -(1)->25>, <1, -(1)->17>, <0, -(1)->20>, <1, -(1)->18>, <0, -(1)->21>, <1, -(1)->19>, <0, -(1)->31>, <1, -(1)->26>, <0, -(1)->29>, <1, -(1)->28>, <0, -(1)->23>, <1, -(1)->22>";

        String firstAttack = "<0, -(1)->10>, <1, -(1)->32>, <0, -(1)->2>, <1, -(1)->1>, <0, -(1)->9>, <1, -(1)->34>, <0, -(1)->11>, <1, -(1)->3>, <0, -(1)->12>, <1, -(1)->36>, <0, -(1)->0>, <1, -(1)->5>, <0, -(1)->4>, <1, -(1)->38>, <0, -(1)->6>, <1, -(1)->7>, <0, -(1)->8>, <1, -(1)->40>, <0, -(1)->39>, <1, -(1)->33>, <0, -(1)->14>, <1, -(1)->35>, <0, -(1)->24>, <1, -(1)->37>, <0, -(1)->30>, <1, -(1)->41>, <0, -(1)->27>, <1, -(1)->13>, <0, -(1)->16>, <1, -(1)->15>, <0, -(1)->25>, <1, -(1)->17>, <0, -(1)->20>, <1, -(1)->18>, <0, -(1)->21>, <1, -(1)->19>, <0, -(1)->31>, <1, -(1)->26>, <0, -(1)->29>, <1, -(1)->28>, <0, -(1)->23>, <1, -(1)->22>, <0, -(1)->31>, <1, -(1)->32>, <0, -(1)->16>, <1, -(1)->32>, <0, -(1)->6>, <1, -(1)->32>, <0, -(1)->21>, <1, -(1)->32>, <0, -(1)->31>, <1, -(1)->32>, <0, -(1)->0>, <1, -(1)->32>, <0, -(1)->27>, <1, -(1)->32>, <0, -(1)->31>, <1, -(1)->32>, <0, -(1)->16>, <1, -(1)->32>, <0, -(1)->2>, <1, -(1)->32>, <0, -(1)->8>, <1, -(1)->32>, <0, -(1)->21>, <1, -(1)->32>, <0, -(1)->24>, <1, -(1)->32>, <0, -(1)->27>, <1, -(1)->32>, <0, -(1)->6>, <1, -(1)->32>, <0, -(1)->21>, <1, -(1)->32>, <0, -(1)->30>, <1, -(1)->32>, <0, -(1)->0>, <1, -(1)->32>, <0, -(1)->8>, <1, -(1)->32>, <0, -(1)->39>, <1, -(1)->32>, <0, -(1)->8>, <1, -(1)->32>, <0, -(1)->2>, <1, -(1)->32>, <0, -(1)->29>, <1, -(1)->32>, <0, -(1)->0>, <1, -(1)->32>, <0, -(1)->4>, <1, -(1)->32>, <0, -(1)->24>, <1, -(1)->32>, <0, -(1)->39>, <1, -(1)->32>, <0, -(1)->21>, <1, -(1)->28>, <0, -(1)->31>, <1, -(1)->28>, <1, -(5)->3>, <1, -(2)->32>";

        String selected = firstAttack;

        List<String> split = Stream.of(selected.substring(1).split(", <"))
                .map(s -> s.substring(0, s.length() - 1))
                .collect(Collectors.toList());

        List<RiskAction> actions = split.stream().
                map(string -> RiskAction.fromString(string.substring(3)))
                .collect(Collectors.toList());

        // TODO: parse player to create actionrecord

        Game<RiskAction, RiskBoard> game = new Risk(); /// new Risk(Risk(0 , true, actionRecords, board));

        for (RiskAction action : actions) game = game.doAction(action);

        DebugMatch match = new DebugMatch(
                (Risk) game,
                gameAgents,
                15,
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
        log.info("Actions: " + match.game.getActionRecords());
    }
}
