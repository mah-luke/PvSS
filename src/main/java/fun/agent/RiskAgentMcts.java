package fun.agent;

import at.ac.tuwien.ifs.sge.agent.AbstractGameAgent;
import at.ac.tuwien.ifs.sge.agent.GameAgent;
import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;

import java.util.concurrent.TimeUnit;

public class RiskAgentMcts extends AbstractGameAgent<Risk, RiskAction> implements GameAgent<Risk, RiskAction> {

    public RiskAgentMcts(Logger log) {
    /* instantiates a AbstractGameAgent so that shouldStopComputation() returns true after 3/4ths of
     * the given time. However, if it can, it will try to compute at least 5 seconds.
     */
        super(3D / 4D, 5, TimeUnit.SECONDS, log);

    //Note: log can be effectively ignored, however it honors the flags of the engine.

    //Do some setup before the TOURNAMENT starts.
  }

    @Override
    public void setUp(int numberOfPlayers, int playerId) {
        super.setUp(numberOfPlayers, playerId);
        // Do setup before the MATCH starts
    }

    public RiskAction computeNextAction(Risk game, long computationTime, TimeUnit timeUnit) {
        return null;
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

