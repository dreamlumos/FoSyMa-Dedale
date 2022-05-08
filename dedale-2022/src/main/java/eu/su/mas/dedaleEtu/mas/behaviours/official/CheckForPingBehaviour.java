package eu.su.mas.dedaleEtu.mas.behaviours.official;

import eu.su.mas.dedaleEtu.mas.agents.official.ExploreDFSAgent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class CheckForPingBehaviour extends SimpleBehaviour { // OneShotBehaviour?

	private static final long serialVersionUID = -2824535739297657670L;
		
	private int pingReceived; // 1 if ping message received, 2 if from unknown agent, 0 otherwise
	
	public CheckForPingBehaviour(ExploreDFSAgent agent) {
		super(agent);
	}

	@Override
	public void action() {
		
		ExploreDFSAgent myAgent = (ExploreDFSAgent) this.myAgent;
		
		System.out.println("Agent "+myAgent.getLocalName()+" is checking for ping.");

		// The agent checks if he received a ping from a teammate. 	
		MessageTemplate msgTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol("SHARE-TOPO"),
				MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));
//				MessageTemplate.MatchConversationId();


		ACLMessage ping = myAgent.receive(msgTemplate);
		if (ping != null) {
			this.pingReceived = 1;
			System.out.println(myAgent.getLocalName()+" received a ping from "+ ping.getSender().getLocalName());

			String sentBy = ping.getSender().getLocalName();
			boolean knownAgent = false;

			if (myAgent.getKnownAgentCharacteristics() != null){
				knownAgent = myAgent.getKnownAgentCharacteristics().containsKey(sentBy);
			} // is null when launched, could try catch

//			boolean knownAgent = true; // Zoe : put it at TRUE to not cause any bug in case my code sucks

			if (knownAgent) {
				// byte[] pingContent = ping.getByteSequenceContent();

				ACLMessage pong = ping.createReply();
				pong.setSender(myAgent.getAID());
				pong.setPerformative(ACLMessage.ACCEPT_PROPOSAL);

				//msg.setContent("1");
				byte[] b = {1};
				pong.setByteSequenceContent(b);

				myAgent.sendMessage(pong);
				System.out.println(this.myAgent.getLocalName()+" is sending a pong to "+ping.getSender().getLocalName());
			}
			else { // myAgent doesn't have any information about the sender
				this.pingReceived = 2;
				ACLMessage unknown = ping.createReply();
				unknown.setSender(myAgent.getAID());
				unknown.setPerformative(ACLMessage.UNKNOWN);

				//msg.setContent("1");
				byte[] b = {1};
				unknown.setByteSequenceContent(b);

				myAgent.sendMessage(unknown);
				System.out.println(this.myAgent.getLocalName()+" is sending a 'Unknown' to "+ping.getSender().getLocalName());
			}
			
		} else {
			this.pingReceived = 0;
			block();
		}
	}

	@Override
	public boolean done() {
		// Kiara: idk if this is necessary, have to test how it works with FSM
		return this.pingReceived == 1 || this.pingReceived == 2;
	}
	
	@Override
	public int onEnd() {
//		reset();
//		((FSMBehaviour) getParent()).registerState();
		return pingReceived;
	}
}
