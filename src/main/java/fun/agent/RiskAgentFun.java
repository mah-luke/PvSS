package fun.agent;

import at.ac.tuwien.ifs.sge.agent.AbstractGameAgent;
import at.ac.tuwien.ifs.sge.agent.GameAgent;
import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskBoard;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class RiskAgentFun extends AbstractGameAgent<Risk, RiskAction> implements
    GameAgent<Risk, RiskAction> {


  public RiskAgentFun(Logger log) {
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
    // Do some setup before the MATCH starts
  }

  @Override
  public RiskAction computeNextAction(Risk game, long computationTime, TimeUnit timeUnit) {
    super.setTimers(computationTime, timeUnit); //Makes sure shouldStopComputation() works

    nanosElapsed(); //returns how many nanos already elapsed.
    nanosLeft(); //returns how much nanos are left (according to timeOutRatio).

    shouldStopComputation(); //returns true if nanosLeft() < 0 or the current thread is stopped.

    RiskBoard board = game.getBoard();
    // returns how many territories a player currently occupies
    board.getNrOfTerritoriesOccupiedByPlayer(playerId);

    //make yourself familiar with the other public methods of board

    //returns the heuristic value i.e. how many territories the given player occupies
    game.getHeuristicValue();
    game.getHeuristicValue(playerId); //equivalent

    //returns the next possible actions
    Set<RiskAction> possibleActions = game.getPossibleActions();

    //gready search for the next best move
    double bestUtilityValue = Double.NEGATIVE_INFINITY;
    double bestHeuristicValue = Double.NEGATIVE_INFINITY;
    RiskAction bestAction = null;

    for (RiskAction possibleAction : possibleActions) {
      Risk next = (Risk) game.doAction(possibleAction); //create a game with that move applied

      double nextUtilityValue = next.getUtilityValue(playerId);
      double nextHeuristicValue = next.getHeuristicValue(playerId);

      if (bestUtilityValue <= nextUtilityValue) {
        if (bestUtilityValue < nextUtilityValue || bestHeuristicValue <= nextHeuristicValue) {
          bestUtilityValue = nextUtilityValue;
          bestHeuristicValue = nextHeuristicValue;
          bestAction = possibleAction;
        }
      }
    }

    //maybe do some fallback action in case action is not valid
    assert bestAction != null;
    assert game.isValidAction(bestAction);

    log.debugf("Found best move: %s", bestAction.toString());

    return bestAction;
  }

  @Override
  public void tearDown() {
    //Do some tear down after the MATCH
  }

  @Override
  public void destroy() {
    //Do some tear down after the TOURNAMENT
  }
}
