package eu.su.mas.dedaleEtu.mas.behaviours.official;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.official.ExploreDFSAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.FullMapRepresentation;
import jade.core.behaviours.SimpleBehaviour;

public class StepBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = -7075642787451313299L;
		
	public StepBehaviour(ExploreDFSAgent agent) {
		super(agent);
	}
	
	@Override
	public void action() {
		String myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
		String nextNodeId = ((ExploreDFSAgent) myAgent).getNextNodeId();
		
		FullMapRepresentation map = ((ExploreDFSAgent) this.myAgent).getMap();
		if (!map.hasOpenNode()) { //Explo finished
			System.out.println(this.myAgent.getLocalName()+" - Exploration successufully done, behaviour removed.");
		} else {
			//4) select next move.
			//4.1 If there exist one open node directly reachable, go for it,
			//	 otherwise choose one from the openNode list, compute the shortestPath and go for it
			if (nextNodeId == null) {
				//no directly accessible openNode
				//chose one, compute the path and take the first step.
				nextNodeId = map.getShortestPathToClosestOpenNode(myPosition).get(0);//getShortestPath(myPosition,this.openNodes.get(0)).get(0);
				//System.out.println(this.myAgent.getLocalName()+"-- list= "+this.myMap.getOpenNodes()+"| nextNode: "+nextNode);
			} else {
				//System.out.println("nextNode notNUll - "+this.myAgent.getLocalName()+"-- list= "+this.myMap.getOpenNodes()+"\n -- nextNode: "+nextNode);
			}
		}
		
		((AbstractDedaleAgent)this.myAgent).moveTo(nextNodeId);

	}

	@Override
	public boolean done() {
		// TODO Auto-generated method stub
		return false;
	}

	
	
}
