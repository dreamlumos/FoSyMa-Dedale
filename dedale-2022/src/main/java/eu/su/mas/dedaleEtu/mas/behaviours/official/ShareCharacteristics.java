package eu.su.mas.dedaleEtu.mas.behaviours.official;

import eu.su.mas.dedale.env.EntityCharacteristics;
import eu.su.mas.dedaleEtu.mas.agents.official.ExploreDFSAgent;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;

public class ShareCharacteristics extends SimpleBehaviour {

    private static final long serialVersionUID = -5609039653282110622L;
    
    private ExploreDFSAgent myAgent;

    public ShareCharacteristics(Agent agent) {
        super(agent);
        this.myAgent = (ExploreDFSAgent) agent;
    }

    @Override
    public void action() {

    	ACLMessage unknown = this.myAgent.getCurrentPong();
        if (unknown != null){
        	
        	System.out.println(this.myAgent.getLocalName()+" is sharing its characteristics.");

            ACLMessage infoMsg = unknown.createReply();
            infoMsg.setSender(this.myAgent.getAID());
            infoMsg.setPerformative(ACLMessage.AGREE);

            EntityCharacteristics charac = this.myAgent.getMyCharacteristics();
            int goldCap = charac.getGoldCapacity();
            int diaCap = charac.getDiamondCapacity();
            int commRadius = charac.getCommunicationReach();

            String sg = goldCap + " " + diaCap + " " + commRadius; // "goldCap[space]diaCap[space]commRadius";

            infoMsg.setContent(sg);

            this.myAgent.sendMessage(infoMsg);
        }
    }

    @Override
    public boolean done() {
        return true;
    }

    @Override
    public int onEnd() {
        return 1;
    }
}
