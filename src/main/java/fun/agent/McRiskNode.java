package fun.agent;

import at.ac.tuwien.ifs.sge.game.Game;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.util.node.GameNode;

import java.util.Objects;

public class McRiskNode implements GameNode<RiskAction> {

    private Game<RiskAction, ?> game;
    private int wins;
    private int plays;

    public McRiskNode() {
        this(null);
    }

    public McRiskNode(Game<RiskAction, ?> game) {
        this(game, 0, 0);
    }

    public McRiskNode(Game<RiskAction, ?> game, RiskAction action) {
        this(game.doAction(action));
    }

    public McRiskNode(Game<RiskAction, ?> game, int wins, int plays) {
        this.game = game;
        this.wins = wins;
        this.plays = plays;
    }


    @Override
    public Game<RiskAction, ?> getGame() {
        return this.game;
    }

    @Override
    public void setGame(Game<RiskAction, ?> game) {
        this.game = game;
    }

    public int getWins() {
        return this.wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
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

    public void incWins() {
        ++this.wins;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            McRiskNode other = (McRiskNode) o;
            return this.wins == other.wins && this.plays == other.plays &&
                    this.game.equals(other.game);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(
                this.game,
                this.wins,
                this.plays
        );
    }
}
