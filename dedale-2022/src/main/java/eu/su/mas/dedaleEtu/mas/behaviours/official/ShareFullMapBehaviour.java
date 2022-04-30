package eu.su.mas.dedaleEtu.mas.behaviours.official;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.FullMapRepresentation;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;

public class ShareFullMapBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = -5609039653282110622L;
	
	private FullMapRepresentation map;
	private List<String> receivers;

	public ShareFullMapBehaviour(Agent agent, FullMapRepresentation map, List<String> receivers) {
		super(agent);
		this.map = map;
		this.receivers = receivers;
	}
	
	@Override
	public void action() {
		
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.setProtocol("SHARE-TOPO");
		msg.setSender(this.myAgent.getAID());
		msg.setPostTimeStamp(System.currentTimeMillis());
		
		for (String r: this.receivers) {
			msg.addReceiver(new AID(r, false));
		}
		
		// TODO select a portion of the graph to send
		SerializableSimpleGraph<String, HashMap<String, Object>> sg = this.map.getSerializableGraph();
		try {					
			msg.setContentObject(sg);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
	}

	@Override
	public boolean done() {
		// TODO Auto-generated method stub
		return false;
	}
}
