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
	private ACLMessage pong;
	private boolean ackReceivedOrTimedOut;

	public SharePartialMapBehaviour(Agent agent, ACLMessage pong) {
		super(agent);
		this.pong = pong;
		this.phase = 0;
	}
	
	@Override
	public void action() {
		
		// TODO: timeout

		if (this.phase == 0) {
			
			ACLMessage mapMsg = pong.createReply();
			mapMsg.setSender(this.myAgent.getAID());
			mapMsg.setPerformative(ACLMessage.INFORM);
		
			ArrayList<String> nodesToShare = ((ExploreDFSAgent)this.myAgent).getNodesToShare(pong.getSender().getName()); // getLocalName()?
			FullMapRepresentation partialMap = ((ExploreDFSAgent)this.myAgent).getMap().getPartMap(nodesToShare);
			SerializableSimpleGraph<String, HashMap<String, Object>> sg = partialMap.getSerializableGraph();
	
			try {
				mapMsg.setContentObject(sg);
			} catch (IOException e) {
				e.printStackTrace();
			}
	
			((AbstractDedaleAgent)this.myAgent).sendMessage(mapMsg);
			
		} else if (this.phase == 1) {
		
			// Wait for ack 	
			MessageTemplate msgTemplate = MessageTemplate.and(
					MessageTemplate.MatchProtocol("SHARE-TOPO"),
					MessageTemplate.MatchPerformative(ACLMessage.CONFIRM));
			ACLMessage ackMsg = this.myAgent.receive(msgTemplate);
			
			
		}
	}

	@Override
	public boolean done() {
		// TODO Auto-generated method stub
		return false;
	}
}
