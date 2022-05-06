package eu.su.mas.dedaleEtu.mas.behaviours.official;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedaleEtu.mas.agents.official.ExploreDFSAgent;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import javafx.util.Pair;
import java.util.List;
import java.util.Objects;

public class CollectAssignedTreasure extends SimpleBehaviour {

    private static final long serialVersionUID = 1122887021789013975L;
    
    private ExploreDFSAgent myAgent;
        // if treasure value bigger than expected, wait? try again? wait? etc

    public CollectAssignedTreasure(Agent agent){
        super(agent);
        this.myAgent = (ExploreDFSAgent) agent;
    }

    @Override
    public void action() {
    	
    	System.out.println("Agent "+this.myAgent.getLocalName()+" is picking up treasure.");
    	if(this.myAgent.getCurrTreasureToPick() != null) {
            Pair<String, Integer> toPick = this.myAgent.getCurrTreasureToPick();
            String toPickNodeId = toPick.getKey();

            if (Objects.equals(toPickNodeId, this.myAgent.getCurrentPosition())) { // checking we are at the right position
                System.out.println("test");
                // Retrieve the list of observations at current position
                List<Couple<String, List<Couple<Observation, Integer>>>> lobs = this.myAgent.observe();
                List<Couple<Observation, Integer>> lObservations = lobs.get(0).getRight(); //list of observations associated to the currentPosition

                for (Couple<Observation, Integer> o : lObservations) {
                    if (Objects.equals(o.getLeft().getName(), this.myAgent.getType())) {
                        boolean isOpen = this.myAgent.openLock(o.getLeft()); // openLock
                        if (isOpen) {
                            if (o.getRight() <= toPick.getValue()) { // if the amount is less or equal to expected amount
                                int amountPicked = myAgent.pick(); // agent picks up the treasure
                                System.out.println("Amount picked: " + amountPicked);
                                this.myAgent.setCurrTreasureToPick(null);
                            }
                        }
                    }
                }

            }
        }
    }

    @Override
    public boolean done() {
        return true;
    }
}
