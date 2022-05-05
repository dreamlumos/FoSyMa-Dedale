package eu.su.mas.dedaleEtu.mas.behaviours.official;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.official.ExploreDFSAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.FullMapRepresentation;
import jade.core.behaviours.SimpleBehaviour;

public class StepBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = -7075642787451313299L;
	private int mapExplored = 0;
		
	public StepBehaviour(ExploreDFSAgent agent) {
		super(agent);
	}
	
	@Override
	public void action() {

		// check if map is wholly explored

		if(mapExplored == 0) {
			String myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
			String nextNodeId = ((ExploreDFSAgent) myAgent).getNextNodeId();

//		if (myPosition == null) { 
//			System.out.println("help map not initialised yet");
//		} else {
//			FullMapRepresentation map = ((ExploreDFSAgent) this.myAgent).getMap();
//			if (!map.hasOpenNode()) { //Explo finished
//				System.out.println(this.myAgent.getLocalName()+" - Exploration successfully done, behaviour removed.");
//			} else {
//				//4) select next move.
//				//4.1 If there exist one open node directly reachable, go for it,
//				//	 otherwise choose one from the openNode list, compute the shortestPath and go for it
//				if (nextNodeId == null) {
//					//no directly accessible openNode
//					//chose one, compute the path and take the first step.
//					nextNodeId = map.getShortestPathToClosestOpenNode(myPosition).get(0);//getShortestPath(myPosition,this.openNodes.get(0)).get(0);
//					//System.out.println(this.myAgent.getLocalName()+"-- list= "+this.myMap.getOpenNodes()+"| nextNode: "+nextNode);
//				} else {
//					//System.out.println("nextNode notNUll - "+this.myAgent.getLocalName()+"-- list= "+this.myMap.getOpenNodes()+"\n -- nextNode: "+nextNode);
//				}
//			}
			System.out.println("Agent " + this.myAgent.getLocalName() + " is moving to " + nextNodeId);
			((AbstractDedaleAgent) this.myAgent).moveTo(nextNodeId);
//		}
		}
		else if(mapExplored == 1){ // we switch to the collect phase

		}
	}

	@Override
	public boolean done() {
		return true;
	}

	@Override
	public int onEnd() {
		return mapExplored;
	}


}
