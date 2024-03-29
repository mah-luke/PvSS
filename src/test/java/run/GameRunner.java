package run;

import at.ac.tuwien.ifs.sge.agent.GameAgent;
import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.engine.game.MatchResult;
import at.ac.tuwien.ifs.sge.game.Game;
import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;
import fun.agent.FunAgent;

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
    static final Logger log = LoggerFactory.getLogger(0, "[main ");
    static final FunAgent FUN_AGENT = new FunAgent(
            LoggerFactory.getLogger(0, "[agent ")
    );


    static final GreedyAgent riskAgentGreedy = new GreedyAgent(
            LoggerFactory.getLogger(0, "[agentOpponent ")
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
        GameAgent<Risk, RiskAction> agent = FUN_AGENT;
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
        GameAgent<Risk, RiskAction> agent = FUN_AGENT;
        GameAgent<Risk, RiskAction> agentOpponent = riskAgentGreedy;

        ExecutorService pool = Executors.newFixedThreadPool(
                Math.max(Runtime.getRuntime().availableProcessors(), 2)
        );

        List<GameAgent<Risk, RiskAction>> gameAgents = new ArrayList<>(2);
        gameAgents.add(agent);
        gameAgents.add(agentOpponent);


        String setUpPlayerDead = "<0, -(1)->10>, <1, -(1)->32>, <0, -(1)->2>, <1, -(1)->1>, <0, -(1)->9>, <1, -(1)->34>, <0, -(1)->11>, <1, -(1)->3>, <0, -(1)->12>, <1, -(1)->36>, <0, -(1)->0>, <1, -(1)->5>, <0, -(1)->4>, <1, -(1)->38>, <0, -(1)->6>, <1, -(1)->7>, <0, -(1)->8>, <1, -(1)->40>, <0, -(1)->39>, <1, -(1)->33>, <0, -(1)->14>, <1, -(1)->35>, <0, -(1)->24>, <1, -(1)->37>, <0, -(1)->30>, <1, -(1)->41>, <0, -(1)->27>, <1, -(1)->13>, <0, -(1)->16>, <1, -(1)->15>, <0, -(1)->25>, <1, -(1)->17>, <0, -(1)->20>, <1, -(1)->18>, <0, -(1)->21>, <1, -(1)->19>, <0, -(1)->31>, <1, -(1)->26>, <0, -(1)->29>, <1, -(1)->28>, <0, -(1)->23>, <1, -(1)->22>";

        String firstAttack = "<0, -(1)->10>, <1, -(1)->32>, <0, -(1)->2>, <1, -(1)->1>, <0, -(1)->9>, <1, -(1)->34>, <0, -(1)->11>, <1, -(1)->3>, <0, -(1)->12>, <1, -(1)->36>, <0, -(1)->0>, <1, -(1)->5>, <0, -(1)->4>, <1, -(1)->38>, <0, -(1)->6>, <1, -(1)->7>, <0, -(1)->8>, <1, -(1)->40>, <0, -(1)->39>, <1, -(1)->33>, <0, -(1)->14>, <1, -(1)->35>, <0, -(1)->24>, <1, -(1)->37>, <0, -(1)->30>, <1, -(1)->41>, <0, -(1)->27>, <1, -(1)->13>, <0, -(1)->16>, <1, -(1)->15>, <0, -(1)->25>, <1, -(1)->17>, <0, -(1)->20>, <1, -(1)->18>, <0, -(1)->21>, <1, -(1)->19>, <0, -(1)->31>, <1, -(1)->26>, <0, -(1)->29>, <1, -(1)->28>, <0, -(1)->23>, <1, -(1)->22>, <0, -(1)->31>, <1, -(1)->32>, <0, -(1)->16>, <1, -(1)->32>, <0, -(1)->6>, <1, -(1)->32>, <0, -(1)->21>, <1, -(1)->32>, <0, -(1)->31>, <1, -(1)->32>, <0, -(1)->0>, <1, -(1)->32>, <0, -(1)->27>, <1, -(1)->32>, <0, -(1)->31>, <1, -(1)->32>, <0, -(1)->16>, <1, -(1)->32>, <0, -(1)->2>, <1, -(1)->32>, <0, -(1)->8>, <1, -(1)->32>, <0, -(1)->21>, <1, -(1)->32>, <0, -(1)->24>, <1, -(1)->32>, <0, -(1)->27>, <1, -(1)->32>, <0, -(1)->6>, <1, -(1)->32>, <0, -(1)->21>, <1, -(1)->32>, <0, -(1)->30>, <1, -(1)->32>, <0, -(1)->0>, <1, -(1)->32>, <0, -(1)->8>, <1, -(1)->32>, <0, -(1)->39>, <1, -(1)->32>, <0, -(1)->8>, <1, -(1)->32>, <0, -(1)->2>, <1, -(1)->32>, <0, -(1)->29>, <1, -(1)->32>, <0, -(1)->0>, <1, -(1)->32>, <0, -(1)->4>, <1, -(1)->32>, <0, -(1)->24>, <1, -(1)->32>, <0, -(1)->39>, <1, -(1)->32>, <0, -(1)->21>, <1, -(1)->28>, <0, -(1)->31>, <1, -(1)->28>, <1, -(5)->3>, <1, -(2)->32>";

        String onlyAustraliaLeft = "<0, -(1)->10>, <1, -(1)->32>, <0, -(1)->2>, <1, -(1)->1>, <0, -(1)->9>, <1, -(1)->34>, <0, -(1)->11>, <1, -(1)->3>, <0, -(1)->12>, <1, -(1)->36>, <0, -(1)->0>, <1, -(1)->5>, <0, -(1)->4>, <1, -(1)->38>, <0, -(1)->6>, <1, -(1)->7>, <0, -(1)->8>, <1, -(1)->40>, <0, -(1)->39>, <1, -(1)->33>, <0, -(1)->14>, <1, -(1)->35>, <0, -(1)->24>, <1, -(1)->37>, <0, -(1)->30>, <1, -(1)->41>, <0, -(1)->27>, <1, -(1)->13>, <0, -(1)->16>, <1, -(1)->15>, <0, -(1)->25>, <1, -(1)->17>, <0, -(1)->20>, <1, -(1)->18>, <0, -(1)->21>, <1, -(1)->19>, <0, -(1)->31>, <1, -(1)->26>, <0, -(1)->29>, <1, -(1)->28>, <0, -(1)->23>, <1, -(1)->22>, <0, -(1)->31>, <1, -(1)->32>, <0, -(1)->16>, <1, -(1)->32>, <0, -(1)->6>, <1, -(1)->32>, <0, -(1)->21>, <1, -(1)->32>, <0, -(1)->31>, <1, -(1)->32>, <0, -(1)->0>, <1, -(1)->32>, <0, -(1)->27>, <1, -(1)->32>, <0, -(1)->31>, <1, -(1)->32>, <0, -(1)->16>, <1, -(1)->32>, <0, -(1)->2>, <1, -(1)->32>, <0, -(1)->8>, <1, -(1)->32>, <0, -(1)->21>, <1, -(1)->32>, <0, -(1)->24>, <1, -(1)->32>, <0, -(1)->27>, <1, -(1)->32>, <0, -(1)->6>, <1, -(1)->32>, <0, -(1)->21>, <1, -(1)->32>, <0, -(1)->30>, <1, -(1)->32>, <0, -(1)->0>, <1, -(1)->32>, <0, -(1)->8>, <1, -(1)->32>, <0, -(1)->39>, <1, -(1)->32>, <0, -(1)->8>, <1, -(1)->32>, <0, -(1)->2>, <1, -(1)->32>, <0, -(1)->29>, <1, -(1)->32>, <0, -(1)->0>, <1, -(1)->32>, <0, -(1)->4>, <1, -(1)->32>, <0, -(1)->24>, <1, -(1)->32>, <0, -(1)->39>, <1, -(1)->32>, <0, -(1)->21>, <1, -(1)->28>, <0, -(1)->31>, <1, -(1)->28>, <1, -(5)->3>, <1, -(2)->32>, <1, 3-(3)->6>, <-6, 0X2>, <1, 3-(3)->6>, <-6, 0X1>, <1, O5>, <1, 6-(2)->8>, <-6, 1X1>, <1, 6-(2)->8>, <-6, 1X1>, <1, 6-(2)->8>, <-6, 2X0>, <1, 32-(3)->21>, <-6, 0X2>, <1, 32-(3)->21>, <-6, 2X0>, <1, 32-(3)->21>, <-6, 1X1>, <1, 32-(3)->21>, <-6, 0X2>, <1, O26>, <1, 21-(2)->23>, <-6, 0X1>, <1, O25>, <1, 23-(2)->25>, <-6, 0X1>, <1, O24>, <1, end phase>, <1, 28-(2)->32>, <0, -(7)->24>, <0, 39-(2)->34>, <-6, 0X1>, <0, O2>, <0, 27-(2)->28>, <-6, 0X1>, <0, O2>, <0, 24-(3)->17>, <-6, 1X0>, <0, 24-(3)->17>, <-6, 0X1>, <0, O8>, <0, 16-(2)->15>, <-6, 0X1>, <0, O2>, <0, 17-(3)->22>, <-6, 1X0>, <0, 31-(3)->33>, <-6, 1X0>, <0, 2-(2)->3>, <-6, 1X0>, <0, 17-(3)->18>, <-6, 1X0>, <0, 31-(3)->37>, <-6, 0X1>, <0, O3>, <0, 0-(3)->5>, <-6, 1X0>, <0, 17-(3)->22>, <-6, 0X1>, <0, O5>, <0, 0-(2)->5>, <-6, 0X1>, <0, O2>, <0, 22-(3)->32>, <-6, 2X0>, <0, 22-(2)->21>, <-6, 0X1>, <0, O2>, <0, 37-(2)->35>, <-6, 0X1>, <0, O2>, <0, end phase>, <0, 34-(1)->28>, <1, -(5)->36>, <1, end phase>, <1, 36-(5)->18>, <0, -(1)->28>, <0, -(2)->20>, <0, -(1)->4>, <0, -(4)->29>, <0, -(1)->24>, <0, -(1)->14>, <0, 28-(3)->26>, <-6, 0X1>, <0, O3>, <0, 26-(2)->36>, <-6, 1X0>, <0, 4-(2)->7>, <-6, 1X0>, <0, 29-(3)->33>, <-6, 0X1>, <0, O5>, <0, end phase>, <0, 33-(4)->35>, <1, -(4)->3>, <1, 32-(2)->22>, <-6, 1X0>, <1, 32-(1)->22>, <-6, 1X0>, <1, 25-(3)->20>, <-6, 2X0>, <1, 25-(3)->20>, <-6, 0X2>, <1, 25-(3)->20>, <-6, 0X1>, <1, O21>, <1, 20-(1)->21>, <-6, 0X1>, <1, 20-(1)->21>, <-6, 0X1>, <1, O20>, <1, 21-(1)->22>, <-6, 0X1>, <1, O19>, <1, 18-(3)->16>, <-6, 0X1>, <1, O5>, <1, 22-(2)->24>, <-6, 1X1>, <1, 22-(2)->24>, <-6, 1X0>, <1, 22-(2)->24>, <-6, 0X1>, <1, O16>, <1, 3-(3)->8>, <-6, 0X2>, <1, O4>, <1, end phase>, <1, 24-(15)->20>, <0, -(5)->28>, <0, -(3)->4>, <0, -(1)->14>, <0, 14-(2)->13>, <-6, 1X0>, <0, 35-(3)->36>, <-6, 1X0>, <0, 35-(3)->36>, <-6, 0X1>, <0, O4>, <0, 28-(3)->32>, <-6, 0X1>, <0, O5>, <0, 4-(3)->6>, <-6, 0X1>, <0, O4>, <0, 36-(3)->18>, <-6, 0X1>, <0, O3>, <0, 6-(3)->3>, <-6, 0X1>, <0, O3>, <0, 32-(3)->21>, <-6, 0X1>, <0, O4>, <0, 3-(2)->7>, <-6, 0X1>, <0, O2>, <0, 21-(3)->23>, <-6, 0X1>, <0, O3>, <0, 23-(2)->25>, <-6, 0X1>, <0, O2>, <0, end phase>, <0, 7-(1)->5>, <1, -(2)->1>, <1, -(1)->16>, <1, 20-(1)->21>, <-6, 0X1>, <1, O15>, <1, 21-(2)->23>, <-6, 0X1>, <1, O14>, <1, 23-(2)->25>, <-6, 1X1>, <1, 23-(2)->25>, <-6, 0X1>, <1, O12>, <1, 1-(2)->5>, <-6, 0X2>, <1, 1-(2)->5>, <-6, 1X0>, <1, 16-(2)->18>, <-6, 2X0>, <1, 16-(2)->18>, <-6, 1X1>, <1, 16-(2)->18>, <-6, 2X0>, <1, 1-(1)->5>, <-6, 0X1>, <1, O1>, <1, end phase>, <1, 25-(11)->20>, <0, -(2)->14>, <0, -(7)->3>, <0, -(9)->7>, <0, 14-(3)->16>, <-6, 1X0>, <0, 7-(3)->5>, <-6, 0X1>, <0, O9>, <0, 14-(2)->13>, <-6, 0X1>, <0, O2>, <0, 3-(3)->8>, <-6, 2X0>, <0, 3-(3)->8>, <-6, 0X2>, <0, 3-(3)->8>, <-6, 0X2>, <0, O5>, <0, 5-(3)->1>, <-6, 0X1>, <0, O8>, <0, end phase>, <0, 26-(1)->18>, <1, -(6)->40>, <1, end phase>, <1, 20-(5)->25>, <0, -(3)->13>, <0, -(3)->14>, <0, -(17)->15>, <0, -(1)->30>, <0, 14-(3)->16>, <-6, 0X1>, <0, O3>, <0, 15-(3)->19>, <-6, 1X0>, <0, 15-(3)->19>, <-6, 0X1>, <0, O17>, <0, 19-(3)->24>, <-6, 0X1>, <0, O16>, <0, 24-(3)->20>, <-6, 0X2>, <0, 24-(3)->20>, <-6, 1X1>, <0, 24-(3)->22>, <-6, 0X1>, <0, O14>, <0, 22-(3)->21>, <-6, 0X1>, <0, O13>, <0, 21-(3)->20>, <-6, 0X2>, <0, 21-(3)->23>, <-6, 0X1>, <0, O12>, <0, 23-(3)->25>, <-6, 1X1>, <0, 23-(3)->25>, <-6, 2X0>, <0, 23-(3)->25>, <-6, 0X2>, <0, 23-(3)->25>, <-6, 0X2>, <0, 23-(3)->25>, <-6, 0X1>, <0, O8>, <0, 25-(3)->20>, <-6, 1X1>, <0, 25-(3)->20>, <-6, 0X1>, <0, O6>, <0, end phase>, <0, end phase>, <1, -(3)->38>, <1, end phase>, <1, 38-(2)->41>";

        String selected = onlyAustraliaLeft;

        List<String> split = Stream.of(selected.substring(1).split(", <"))
                .map(s -> s.substring(0, s.length() - 1))
                .collect(Collectors.toList());


        for (String s : split) {
            System.out.println(s);
        }

        List<RiskAction> actions = split.stream().
                map(string -> RiskAction.fromString(string.split("^-?\\d+, ")[1]))
                .collect(Collectors.toList());

        // TODO: parse player to create actionrecord

        Game<RiskAction, RiskBoard> game = new Risk(); /// new Risk(Risk(0 , true, actionRecords, board));

        // for (RiskAction action : actions) game = game.doAction(action);

        DebugMatch match = new DebugMatch(
                (Risk) game,
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
        log.info("Actions: " + match.game.getActionRecords());
        log.info("# of Actions " +  match.game.getActionRecords().size());
    }
}
