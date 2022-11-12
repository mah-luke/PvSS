package org.fun;

import at.ac.tuwien.ifs.sge.agent.AbstractGameAgent;
import at.ac.tuwien.ifs.sge.agent.GameAgent;
import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.game.Game;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class SimpleFunAgent<G extends Game<A, ?>, A> extends AbstractGameAgent<G, A>
        implements GameAgent<G, A> {

    public SimpleFunAgent(Logger log) {
        super(log);
    }

    @Override
    public A computeNextAction(G game, long computationTime, TimeUnit timeUnit){
        super.setTimers(computationTime, timeUnit);

        // always use first option
        return List.copyOf(game.getPossibleActions()).get(0);
    }



}
