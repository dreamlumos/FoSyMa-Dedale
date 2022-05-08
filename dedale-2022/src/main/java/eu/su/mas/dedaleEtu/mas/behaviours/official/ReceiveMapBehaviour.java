package eu.su.mas.dedaleEtu.mas.behaviours.official;

import java.util.HashMap;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.official.ExploreDFSAgent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class ReceiveMapBehaviour extends SimpleBehaviour {
	
	private static final long serialVersionUID = -1122887021789014975L;
	
	private int mapReceived;
    private long timeoutDate;
    private boolean timedOut;
	
	public ReceiveMapBehaviour(ExploreDFSAgent agent) {
		super(agent);
        this.timeoutDate = System.currentTimeMillis() + 500;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void action() {

		System.out.println("[ReceiveMapBehaviour] Agent "+this.myAgent.getLocalName()+" is waiting for a map.");

		// The agent checks if he received a map from a teammate.
		MessageTemplate msgTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol("SHARE-TOPO"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));

		ACLMessage mapMsg = this.myAgent.receive(msgTemplate);
		
		if (mapMsg != null) {
			System.out.println("[ReceiveMapBehaviour] Agent "+this.myAgent.getLocalName()+" has received a map.");

			this.mapReceived = 1;

			SerializableSimpleGraph<String, HashMap<String, Object>> sgreceived = null;
			try {
				sgreceived = (SerializableSimpleGraph<String, HashMap<String, Object>>) mapMsg.getContentObject();
			} catch (UnreadableException e) {
				e.printStackTrace();
			}
			((ExploreDFSAgent) this.myAgent).getMap().mergeMap(sgreceived);
			
			ACLMessage mapReceivedAck = mapMsg.createReply();
			mapReceivedAck.setSender(this.myAgent.getAID());
			mapReceivedAck.setPerformative(ACLMessage.CONFIRM);
			
			//msg.setContent("1");
			byte[] b = {1};
			mapReceivedAck.setByteSequenceContent(b);
			
			((AbstractDedaleAgent)this.myAgent).sendMessage(mapReceivedAck);
			System.out.println("[ReceiveMapBehaviour] Agent "+this.myAgent.getLocalName()+" is sending an ack.");
			
		} else {
			this.mapReceived = 0;
			//block();
			
			if (System.currentTimeMillis() > this.timeoutDate) {
				this.timedOut = true;
			}
		}
	}

	@Override
	public boolean done() {
		if (this.mapReceived == 1 || this.timedOut) { // Kiara: idk if this is necessary, have to test how it works with FSM
			return true;
		}
		return false;
	}
	
	@Override
	public int onEnd() {
//		reset();
//		((FSMBehaviour) getParent()).registerState();
		return mapReceived;
	}
}
