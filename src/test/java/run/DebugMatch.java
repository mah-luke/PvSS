package run;

import at.ac.tuwien.ifs.sge.agent.GameAgent;
import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.engine.game.Match;
import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class DebugMatch extends Match<Risk, GameAgent<Risk, RiskAction>, RiskAction> {

    public Risk game;
    public List<GameAgent<Risk, RiskAction>> gameAgents;
    public DebugMatch( Risk game,
                      List<GameAgent<Risk,
                              RiskAction>> gameAgents,
                      long computationTime,
                      TimeUnit timeUnit,
                      boolean debug,
                      Logger log,
                      ExecutorService pool) {
        super(game, gameAgents, computationTime, timeUnit, debug, log, pool);
        this.game = game;
        this.gameAgents = gameAgents;
    }

}
