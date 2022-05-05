package eu.su.mas.dedaleEtu.mas.behaviours.official;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.official.ExploreDFSAgent;
import jade.core.Agent;
import jade.core.Node;
import jade.core.behaviours.SimpleBehaviour;
import javafx.util.Pair;

import java.util.Objects;

public class CollectAssignedTreasure extends SimpleBehaviour {

    private static final long serialVersionUID = 1122887021789013975L;
        // if treasure value bigger than expected, wait? try again? wait? etc

    public CollectAssignedTreasure(Agent agent, Pair<String, Integer> toPick){
        String nodeId = toPick.getKey();
        if(Objects.equals(nodeId, ((AbstractDedaleAgent) this.myAgent).getCurrentPosition())){
            // openLock
            // check the amount of treasure against the max value
            // if less, then pick
//            Node node = ((ExploreDFSAgent) this.myAgent).getMap().g.getNode(nodeId);
//            ((AbstractDedaleAgent) this.myAgent).openLock(node.);
        }
    }

    @Override
    public void action() {

    }

    @Override
    public boolean done() {
        return false;
    }
}
