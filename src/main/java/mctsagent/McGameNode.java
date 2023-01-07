package mctsagent;

import at.ac.tuwien.ifs.sge.game.Game;
import at.ac.tuwien.ifs.sge.util.node.GameNode;

import java.util.Objects;

public class McGameNode<A> implements GameNode<A> {
    private Game<A, ?> game;
    private int wins;
    private int plays;

    public McGameNode() {
        this((Game)null);
    }

    public McGameNode(Game<A, ?> game) {
        this(game, 0, 0);
    }

    public McGameNode(Game<A, ?> game, A action) {
        this(game.doAction(action));
    }

    public McGameNode(Game<A, ?> game, int wins, int plays) {
        this.game = game;
        this.wins = wins;
        this.plays = plays;
    }

    public Game<A, ?> getGame() {
        return this.game;
    }

    public void setGame(Game<A, ?> game) {
        this.game = game;
    }

    public int getWins() {
        return this.wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public void incWins() {
        ++this.wins;
    }

    public int getPlays() {
        return this.plays;
    }

    public void setPlays(int plays) {
        this.plays = plays;
    }

    public void incPlays() {
        ++this.plays;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode<?> mcGameNode = (at.ac.tuwien.ifs.sge.agent.mctsagent.McGameNode)o;
            return this.wins == mcGameNode.wins && this.plays == mcGameNode.plays && this.game.equals(mcGameNode.game);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.game, this.wins, this.plays});
    }
}
