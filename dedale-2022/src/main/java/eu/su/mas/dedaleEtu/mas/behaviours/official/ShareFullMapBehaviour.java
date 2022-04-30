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

public class ShareFullMapBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = -5609039653282110622L;
	
	private int pongReceived;

	public ShareFullMapBehaviour(Agent agent) {
		super(agent);
	}
	
	@Override
	public void action() {
		this.pongReceived = 0;
		// The agent checks if he received a pong from a teammate.
		MessageTemplate msgTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol("SHARE-TOPO"),
				MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL));
		ACLMessage pong = this.myAgent.receive(msgTemplate);

		if (pong != null) {
			this.pongReceived = 1;
			ACLMessage mapMsg = pong.createReply();
			mapMsg.setSender(this.myAgent.getAID());
			mapMsg.setPerformative(ACLMessage.INFORM);
			mapMsg.setPostTimeStamp(System.currentTimeMillis());
		
			// TODO select a portion of the graph to send
			ArrayList<String> nodesToShare = ((ExploreDFSAgent)this.myAgent).getNodesToShare(pong.getSender().getName()); // getLocalName()?
			// TODO call function
			FullMapRepresentation sg = ((ExploreDFSAgent)this.myAgent).getMap().getPartMap(nodesToShare);

//			SerializableSimpleGraph<String, HashMap<String, Object>> sg = ((ExploreDFSAgent)this.myAgent).getMap().getSerializableGraph();
			try {
				mapMsg.setContentObject(sg);
			} catch (IOException e) {
				e.printStackTrace();
			}

			((AbstractDedaleAgent)this.myAgent).sendMessage(mapMsg);
		}
	}

	@Override
	public boolean done() {
		// TODO Auto-generated method stub
		return false;
	}
}
