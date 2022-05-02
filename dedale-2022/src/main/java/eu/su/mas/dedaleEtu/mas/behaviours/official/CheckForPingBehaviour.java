package eu.su.mas.dedaleEtu.mas.behaviours.official;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.official.ExploreDFSAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.FullMapRepresentation;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class CheckForPingBehaviour extends SimpleBehaviour { // OneShotBehaviour?

	private static final long serialVersionUID = -2824535739297657670L;
		
	private int pingReceived; // 1 if ping message received, 0 otherwise
	
	public CheckForPingBehaviour(ExploreDFSAgent agent) {
		super(agent);
	}

	@Override
	public void action() {
		
		System.out.println("Agent "+this.myAgent.getLocalName()+" is checking for ping.");

		// The agent checks if he received a ping from a teammate. 	
		MessageTemplate msgTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol("SHARE-TOPO"),
				MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));
		ACLMessage ping = this.myAgent.receive(msgTemplate);
		
		if (ping != null) {
			this.pingReceived = 1;
			
			// byte[] pingContent = ping.getByteSequenceContent();
			// TODO: maybe use 0 to indicate that agent 1 doesn't known agent 2, and 1 to just ping
			
			ACLMessage pong = ping.createReply();
			pong.setSender(this.myAgent.getAID());
			pong.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
			
			//msg.setContent("1");
			byte[] b = {1};
			pong.setByteSequenceContent(b);
			
			((AbstractDedaleAgent)this.myAgent).sendMessage(pong);
			
		} else {
			this.pingReceived = 0;
		}
	}

	@Override
	public boolean done() {
		if (this.pingReceived == 1) { // Kiara: idk if this is necessary, have to test how it works with FSM
			return true;
		}
		return false;
	}
	
	@Override
	public int onEnd() {
//		reset();
//		((FSMBehaviour) getParent()).registerState();
		return pingReceived;
	}
}
