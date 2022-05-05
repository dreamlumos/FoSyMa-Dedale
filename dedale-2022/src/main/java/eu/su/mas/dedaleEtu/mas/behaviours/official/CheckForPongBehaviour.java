package eu.su.mas.dedaleEtu.mas.behaviours.official;

import eu.su.mas.dedaleEtu.mas.agents.official.ExploreDFSAgent;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class CheckForPongBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = 560497589376177200L;
	
	private boolean pongReceived;
	private int received;
	
	public CheckForPongBehaviour(Agent a) {
		super(a);
	}
	

	@Override
	public void action() {
		System.out.println("Agent "+this.myAgent.getLocalName()+" is checking for pong.");

		// The agent checks if he received a pong from a teammate. 	
		MessageTemplate msgTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol("SHARE-TOPO"),
				MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL));
		ACLMessage pong = this.myAgent.receive(msgTemplate);
		
		if (pong != null) {
			this.pongReceived = true;
			this.received = 1;
			
			// Sharing of the map in a separate behaviour which is added to the pool
//			this.myAgent.addBehaviour(new SharePartialMapBehaviour(this.myAgent, pong));
			((ExploreDFSAgent)this.myAgent).setCurrentPong(pong);
//			String s = ((ExploreDFSAgent)(this.myAgent)).getListBehavTemp();
//			System.out.println(s);
			
		} else {
			this.pongReceived = false;
			this.received = 0;
			block();
		}
		
	}
	@Override
	public boolean done() {
		return pongReceived;
	}
	@Override
	public int onEnd() {
		return received;
	}


}
