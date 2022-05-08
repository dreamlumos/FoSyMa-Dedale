package eu.su.mas.dedaleEtu.mas.behaviours.official;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedale.env.EntityCharacteristics;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.official.ExploreDFSAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.FullMapRepresentation;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class ShareCharacteristics extends SimpleBehaviour {

    private static final long serialVersionUID = -5609039653282110622L;

    private int phase; //Phase 0: sending info, Phase 1: waiting for ack/pong?
    private ACLMessage unknown;
//    private boolean ackReceivedOrTimedOut;
//    private long timeoutDate;

    public ShareCharacteristics(Agent agent, ACLMessage unknown) {
        super(agent);
        this.unknown = unknown;
        this.phase = 0;
        if(this.unknown == null){
            this.phase = 2;
        }

    }

    @Override
    public void action() {

    	System.out.println(this.myAgent.getLocalName()+" is sharing its characteristics.");
    	
        if (this.phase == 0) {

            ACLMessage infoMsg = unknown.createReply();
            infoMsg.setSender(this.myAgent.getAID());
            infoMsg.setPerformative(ACLMessage.AGREE);

            EntityCharacteristics charac = ((ExploreDFSAgent)this.myAgent).getMyCharacteristics();
            int goldCap = charac.getGoldCapacity();
            int diaCap = charac.getDiamondCapacity();
            int commRadius = charac.getCommunicationReach();

            String sg = goldCap + " " + diaCap + " " + commRadius; // "goldCap[space]diaCap[space]commRadius";

            infoMsg.setContent(sg);

            ((AbstractDedaleAgent)this.myAgent).sendMessage(infoMsg);
//            this.timeoutDate = System.currentTimeMillis() + 1000; // 1s timeout

//        } else if (this.phase == 1) { // Zoe : wait where is this.phase being modified?
//
//            // Zoe : maybe here i should directly add a checkForPongBehaviour to my agent? instead of recoding it?
//            // Wait for pong
//            MessageTemplate msgTemplate = MessageTemplate.and(
//                    MessageTemplate.MatchProtocol("SHARE-TOPO"),
//                    MessageTemplate.MatchPerformative(ACLMessage.CONFIRM));
//            ACLMessage ackMsg = this.myAgent.receive(msgTemplate);
//
//            if (ackMsg != null) {
//                System.out.println("Agent "+this.myAgent.getLocalName()+" received pong from Agent "+ackMsg.getSender().getLocalName());
//                ((ExploreDFSAgent) this.myAgent).clearNodesToShare(ackMsg.getSender().getLocalName());
//                this.ackReceivedOrTimedOut = true;
//            } else if (System.currentTimeMillis() > this.timeoutDate) {
//                System.out.println("Agent "+this.myAgent.getLocalName()+" didn't receive pong and timed out");
//                this.ackReceivedOrTimedOut = true;
//            }

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
