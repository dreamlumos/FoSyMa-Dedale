package eu.su.mas.dedaleEtu.mas.behaviours.official;

import java.util.ArrayList;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.official.ExploreDFSAgent;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

public class PingBehaviour extends OneShotBehaviour { // ProposeInitiator?

	private static final long serialVersionUID = -7797906527074053211L;

	public PingBehaviour(Agent agent) {
		super(agent);
	}
	
	@Override
	public void action() {
		
		ACLMessage ping = new ACLMessage(ACLMessage.PROPOSE);
		ping.setProtocol("SHARE-TOPO");
		ping.setSender(this.myAgent.getAID());
		
		ArrayList<String> receivers = ((ExploreDFSAgent) this.myAgent).getAgentsToPing();
		for (String receiverName: receivers) {
			ping.addReceiver(new AID(receiverName, false));
		}

		//msg.setContent("1");
		byte[] b = {1};
		ping.setByteSequenceContent(b);
		
		((AbstractDedaleAgent)this.myAgent).sendMessage(ping);
				
	}
}


//1: ping (wait for pong for X seconds)
//2: pong (wait for response for X seconds)
//1: 
//	if 2 known:
//		send map (wait for ack for X seconds)
//		2: ack
//	if 2 unknown:
//		please send me your info (timeout)
//		2: sends info
//		1: send map (and update info received)
//		2: ack
//		send my info
//		send map
		