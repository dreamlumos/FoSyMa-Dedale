package eu.su.mas.dedaleEtu.mas.behaviours.official;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

public class PongBehaviour extends OneShotBehaviour {

	private static final long serialVersionUID = -7797906527074053211L;
	private ACLMessage ping;

	public PongBehaviour(Agent agent, ACLMessage ping) {
		super(agent);
	}
	
	@Override
	public void action() {
		
		ACLMessage msg = this.ping.createReply();
		msg.setSender(this.myAgent.getAID());
		msg.setPerformative(ACLMessage.AGREE);
		
		//msg.setContent("1");
		byte[] b = {1};
		msg.setByteSequenceContent(b);
		
		((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
	}
}