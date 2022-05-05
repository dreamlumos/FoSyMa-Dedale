package eu.su.mas.dedaleEtu.mas.behaviours.official;

import dataStructures.serializableGraph.SerializableNode;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.official.ExploreDFSAgent;
import jade.core.Agent;
import jade.core.Node;
import jade.core.behaviours.SimpleBehaviour;
import javafx.util.Pair;
import org.graphstream.ui.javafx.util.AttributeUtils;

import java.util.List;
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
//            ((AbstractDedaleAgent) this.myAgent).openLock(((SerializableNode)node).getAttribute());
            List<Couple<String, List<Couple<Observation, Integer>>>> observations = ((AbstractDedaleAgent) this.myAgent).observe();
            for(Couple<String, List<Couple<Observation, Integer>>> obs: observations){
                if(Objects.equals(obs.getLeft(), nodeId)){
                    List<Couple<Observation, Integer>> lobs = obs.getRight();
                    for(Couple<Observation, Integer> o: lobs){
                        if(Objects.equals(o.getLeft().getName(), ((ExploreDFSAgent) this.myAgent).getType())){
                            ((ExploreDFSAgent) this.myAgent).openLock(o.getLeft()); // openLock
                        }
                    }
                }
            }
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
