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
    private boolean toReinitialise;
	
	public ReceiveMapBehaviour(ExploreDFSAgent agent) {
		super(agent);
		this.toReinitialise = true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void action() {

    	if (this.toReinitialise == true) {
    		this.timeoutDate = System.currentTimeMillis() + 500;
    		this.timedOut = false;
    		this.mapReceived = 0;
    		System.out.println("[ReceiveMapBehaviour] Agent "+this.myAgent.getLocalName()+" is waiting for a map.");
            this.toReinitialise = false;
    	}
		
		// The agent checks if he received a map from a teammate.
		MessageTemplate msgTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol("SHARE-TOPO"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));

		ACLMessage mapMsg = this.myAgent.receive(msgTemplate);
		
		if (mapMsg != null) {
			// Receiving map
			System.out.println("[ReceiveMapBehaviour] Agent "+this.myAgent.getLocalName()+" has received a map.");

			this.mapReceived = 1;
            this.toReinitialise = true;

			SerializableSimpleGraph<String, HashMap<String, Object>> sgreceived = null;
			try {
				sgreceived = (SerializableSimpleGraph<String, HashMap<String, Object>>) mapMsg.getContentObject();
			} catch (UnreadableException e) {
				e.printStackTrace();
			}
			System.out.println("[ReceiveMapBehaviour] Agent "+this.myAgent.getLocalName()+" is merging maps.");
			((ExploreDFSAgent) this.myAgent).getMap().mergeMap(sgreceived);
			
			// Sending ack
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
			
			if (System.currentTimeMillis() > this.timeoutDate) {
				System.out.println(this.myAgent.getLocalName()+"RMB timedout");
				this.timedOut = true;
	            this.toReinitialise = true;
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
