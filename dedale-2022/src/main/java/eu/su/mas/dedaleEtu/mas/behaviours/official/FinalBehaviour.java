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
		String type = myAgent.getType();
		
		System.out.println("[FinalBehaviour] Type of "+myAgent.getLocalName()+": "+type);
		if (type == "Gold") {
			System.out.println("[FinalBehaviour] "+myAgent.getLocalName()+"'s original capacity: "+ec.getGoldCapacity());
		}
		if (type == "Diamond") {
			System.out.println("[FinalBehaviour] "+myAgent.getLocalName()+"'s original capacity: "+ec.getDiamondCapacity());
		}
		System.out.println("[FinalBehaviour] "+myAgent.getLocalName() + " remaining capacity: " + myAgent.getBackPackFreeSpace());
		//this.myAgent.takeDown();
		this.myAgent.doDelete();
	}
}
