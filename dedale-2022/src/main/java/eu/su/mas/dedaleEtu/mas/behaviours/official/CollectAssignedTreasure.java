package eu.su.mas.dedaleEtu.mas.behaviours.official;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.official.ExploreDFSAgent;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import javafx.util.Pair;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class CollectAssignedTreasure extends SimpleBehaviour {

    private static final long serialVersionUID = 1122887021789013975L;

    private int picked = 0;
    private ExploreDFSAgent myAgent;
        // if treasure value bigger than expected, wait? try again? wait? etc

    public CollectAssignedTreasure(Agent agent){
        super(agent);
        this.myAgent = (ExploreDFSAgent) agent;
    }

    @Override
    public void action() {
        System.out.println("[CollectAssignedTreasure] Agent "+this.myAgent.getLocalName()+" is going to attempt to pick up treasure.");
    	if(this.myAgent.getCurrTreasureToPick() != null) {
            Pair<String, Integer> toPick = this.myAgent.getCurrTreasureToPick();
            String toPickNodeId = toPick.getKey();
            System.out.println("[CollectAssignedTreasure] Agent "+this.myAgent.getLocalName()+" is going to pick up treasure at " + toPickNodeId + ".");

            if (Objects.equals(toPickNodeId, this.myAgent.getCurrentPosition())) { // checking we are at the right position
                // Retrieve the list of observations at current position
                System.out.println("[CollectAssignedTreasure] Agent "+this.myAgent.getLocalName()+" is at " + toPickNodeId + ".");

                List<Couple<String, List<Couple<Observation, Integer>>>> lobs = this.myAgent.observe();
                List<Couple<Observation, Integer>> lObservations = lobs.get(0).getRight(); //list of observations associated to the currentPosition

                for (Couple<Observation, Integer> o : lObservations) {
                    if (Objects.equals(o.getLeft().getName(), this.myAgent.getType())) {
                        boolean isOpen = this.myAgent.openLock(o.getLeft()); // openLock
                        if (isOpen) {
                            System.out.println("The lock is open.");
                            if (o.getRight() <= toPick.getValue()) { // if the amount is less or equal to expected amount
                                try {
                                    int amountPicked = myAgent.pick(); // agent picks up the treasure
                                    System.out.println("[CollectAssignedTreasure] Amount of " + this.myAgent.getType() + " picked by " + this.myAgent.getLocalName() + " : " + amountPicked);
                                } catch(Exception e) {
                                    e.printStackTrace();
                                    System.out.println("[CollectAssignedTreasure] "+this.myAgent.getLocalName() + " failed to pick " + toPick.getValue() + " of " + this.myAgent.getType() + " at " + toPickNodeId);
                                }
                                this.myAgent.setCurrTreasureToPick(null);
                            } else {
//                                long timeOut = System.currentTimeMillis() + 300;
//                                while
//                                    Random r= new Random();
//                                    int moveId=1+r.nextInt(lobs.size()-1);//removing the current position from the list of target, not necessary as to stay is an action but allow quicker random move
//
//                                    //The move action (if any) should be the last action of your behaviour
//                                    ((AbstractDedaleAgent)this.myAgent).moveTo(lobs.get(moveId).getLeft());
//
//    //                                this.picked = 1;
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
    @Override
    public int onEnd() {
        return this.picked;
    }
}
