package eu.su.mas.dedaleEtu.mas.behaviours.official;

import java.util.List;
import java.util.Random;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.official.ExploreDFSAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.FullMapRepresentation;
import jade.core.behaviours.SimpleBehaviour;

public class ObserveEnvBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = 7387125205688858126L;
		
	private ExploreDFSAgent myAgent;
	
	public ObserveEnvBehaviour(ExploreDFSAgent agent) {
		super(agent);
		this.myAgent = agent;
	}
	
	@Override
	public void action() {
		
		System.out.println(this.myAgent.getLocalName()+" is observing its environment!");

		FullMapRepresentation map = this.myAgent.getMap();
		
		if (map == null) {
			map = new FullMapRepresentation(true);
			this.myAgent.setMap(map);
		}

		try {
			this.myAgent.doWait(1); // Waiting time here so we can see what the agent is doing
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Retrieve the current position and the list of observations
		String myPosition = this.myAgent.getCurrentPosition();
		List<Couple<String,List<Couple<Observation,Integer>>>> lobs = this.myAgent.observe();

		// Update current node
		List<Couple<Observation,Integer>> lObservations = lobs.get(0).getRight(); //list of observations associated to the currentPosition
		map.addNode(myPosition, lObservations, System.currentTimeMillis(), -1);

		// Update neighbouring nodes
		String nextNodeId = null;
		
		int nbObs = lobs.size();
		for (int i = 1; i<nbObs; i++) {
			
			String nodeId = lobs.get(i).getLeft();
			boolean isNewNode = map.addNode(nodeId, lobs.get(i).getRight(), -1, System.currentTimeMillis());
			
			// the node may exist, but not necessarily the edge
			map.addEdge(myPosition, nodeId);
			if (nextNodeId == null && isNewNode) {
				nextNodeId = nodeId;
			}
		}
		this.myAgent.setNextNodeId(nextNodeId);
		
		// Update nodesToShare
		this.myAgent.addNodeToShare(myPosition);
		
//		// Update next position
//
//		if ((this.myAgent.getMap().hasOpenNode())) {
//			if (this.myAgent.getUnsuccessfulMovesExplo() > 4) {
//				nextNodeId = map.getShortestPathToNextClosestOpenNode(myPosition).get(0);
//				this.myAgent.setUnsuccessfulMovesExplo();
//			} else {
//				if (nextNodeId == null) {
//					//no directly accessible openNode
//					//chose one, compute the path and take the first step.
//					List<String> shortestPath = map.getShortestPathToClosestOpenNode(myPosition);
//					if (shortestPath != null) {
//						nextNodeId = shortestPath.get(0);
//					} else {
////						shortestPath = map.getShortestPathToClosestOpenNode(myPosition);
////						nextNodeId = shortestPath.get(0);
//						//?
//						if (myPosition != null) {
//							//List of observable from the agent's current position
//
//							//Random move from the current position
//							Random r = new Random();
//							int moveId = 1 + r.nextInt(lobs.size() - 1);//removing the current position from the list of target, not necessary as to stay is an action but allow quicker random move
//
//							//The move action (if any) should be the last action of your behaviour
//							nextNodeId = lobs.get(moveId).getLeft();
////							((AbstractDedaleAgent) this.myAgent).moveTo(lobs.get(moveId).getLeft());
//						}
//					}
//				}
//			}
//			this.myAgent.setNextNodeId(nextNodeId);
//		}

	}
	
	@Override
	public boolean done() {
		return true;
	}

}
