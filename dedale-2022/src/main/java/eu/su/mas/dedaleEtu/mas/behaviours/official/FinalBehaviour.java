package eu.su.mas.dedaleEtu.mas.behaviours.official;

import eu.su.mas.dedale.env.EntityCharacteristics;
import eu.su.mas.dedaleEtu.mas.agents.official.ExploreDFSAgent;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;

public class FinalBehaviour extends OneShotBehaviour {

	private static final long serialVersionUID = 641341260534295805L;
	private ExploreDFSAgent myAgent;
	public FinalBehaviour(Agent agent) {
		super(agent);
		this.myAgent = (ExploreDFSAgent)agent;
	}
	
	@Override
	public void action() {
		EntityCharacteristics ec = myAgent.getMyCharacteristics();
		System.out.println(myAgent.getLocalName() + " gold capacity : " + ec.getGoldCapacity());
		System.out.println(myAgent.getLocalName() + " diamond capacity : " + ec.getDiamondCapacity());
		//this.myAgent.doDelete();
		this.myAgent.takeDown();
	}
}
