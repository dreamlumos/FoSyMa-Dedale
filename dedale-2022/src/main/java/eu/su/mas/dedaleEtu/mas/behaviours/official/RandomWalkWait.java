package eu.su.mas.dedaleEtu.mas.behaviours.official;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.TickerBehaviour;

import java.util.List;
import java.util.Random;

public class RandomWalkWait extends SimpleBehaviour {
    private static final long serialVersionUID = 9088209402507795289L;
    private long timeOut;

    public RandomWalkWait (final AbstractDedaleAgent myagent) {
        super(myagent);
        timeOut = System.currentTimeMillis() + 300;
    }

    @Override
    public void action() {

        //Example to retrieve the current position
        String myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
        System.out.println(this.myAgent.getLocalName()+" -- myCurrentPosition is: "+myPosition);
        if (myPosition!=null){
            //List of observable from the agent's current position
            List<Couple<String, List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition

            //Random move from the current position
            Random r= new Random();
            int moveId=1+r.nextInt(lobs.size()-1);//removing the current position from the list of target, not necessary as to stay is an action but allow quicker random move

            //The move action (if any) should be the last action of your behaviour
            ((AbstractDedaleAgent)this.myAgent).moveTo(lobs.get(moveId).getLeft());
        }

    }

    @Override
    public boolean done() {
        return System.currentTimeMillis() > this.timeOut;
    }

    public int onEnd(){
        reset();
        return 1;
    }


}