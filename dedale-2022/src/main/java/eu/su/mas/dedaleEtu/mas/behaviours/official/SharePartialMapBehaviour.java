package eu.su.mas.dedaleEtu.mas.behaviours.official;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.official.ExploreDFSAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.FullMapRepresentation;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class SharePartialMapBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = -5609039653282110622L;
	
	private int phase; //Phase 0: sending map, Phase 1: waiting for ack
	private boolean ackReceivedOrTimedOut;
	private long timeoutDate;
	private boolean toReinitialise;
	private ExploreDFSAgent myAgent;

	public SharePartialMapBehaviour(Agent agent) {
		super(agent);
		this.phase = 0;
		this.toReinitialise = true;
		this.ackReceivedOrTimedOut = false;
		this.myAgent = (ExploreDFSAgent) agent;
	}
	
	@Override
	public void action() {
		
    	if (this.toReinitialise == true) {
    		this.timeoutDate = System.currentTimeMillis() + 500;
			this.ackReceivedOrTimedOut = false;
			this.phase = 0;
            this.toReinitialise = false;
    	}

		if (this.phase == 0) {
			System.out.println("Agent "+this.myAgent.getLocalName()+" is sharing a map.");
			
			ACLMessage pong = this.myAgent.getCurrentPong();
			ACLMessage mapMsg = pong.createReply();
			mapMsg.setSender(this.myAgent.getAID());
			mapMsg.setPerformative(ACLMessage.INFORM);
		
			ArrayList<String> nodesToShare = ((ExploreDFSAgent)this.myAgent).getNodesToShare(pong.getSender().getLocalName());
			FullMapRepresentation partialMap = ((ExploreDFSAgent)this.myAgent).getMap().getPartialMap(nodesToShare);
			SerializableSimpleGraph<String, HashMap<String, Object>> sg = partialMap.getSerializableGraph();
	
			try {
				mapMsg.setContentObject(sg);
			} catch (IOException e) {
				e.printStackTrace();
			}
	
			((AbstractDedaleAgent)this.myAgent).sendMessage(mapMsg);
			this.timeoutDate = System.currentTimeMillis() + 1000; // 1s timeout
			this.phase = 1;
			
		} else if (this.phase == 1) {
		
			// Wait for ack 	
			MessageTemplate msgTemplate = MessageTemplate.and(
					MessageTemplate.MatchProtocol("SHARE-TOPO"),
					MessageTemplate.MatchPerformative(ACLMessage.CONFIRM));
			ACLMessage ackMsg = this.myAgent.receive(msgTemplate);
			
			if (ackMsg != null) {
				System.out.println("Agent "+this.myAgent.getLocalName()+" received ack from Agent "+ackMsg.getSender().getLocalName());
				((ExploreDFSAgent) this.myAgent).clearNodesToShare(ackMsg.getSender().getLocalName());
				this.ackReceivedOrTimedOut = true;
	            this.toReinitialise = true;
			} else if (System.currentTimeMillis() > this.timeoutDate) {
				System.out.println("Agent "+this.myAgent.getLocalName()+" didn't receive ack and timed out");
				this.ackReceivedOrTimedOut = true;
	            this.toReinitialise = true;
			}
			
		}
	}

	@Override
	public boolean done() {
		return ackReceivedOrTimedOut;
	}

	@Override
	public int onEnd() {
		return 0;
	}
}
