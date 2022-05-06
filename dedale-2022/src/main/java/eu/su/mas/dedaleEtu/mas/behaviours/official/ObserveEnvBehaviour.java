package eu.su.mas.dedaleEtu.mas.behaviours.official;

import java.util.List;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.official.ExploreDFSAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.FullMapRepresentation;
import jade.core.behaviours.SimpleBehaviour;

public class ObserveEnvBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = 7387125205688858126L;
		
	public ObserveEnvBehaviour(ExploreDFSAgent agent) {
		super(agent);
	}
	
	@Override
	public void action() {
		System.out.println("observe env!");

		FullMapRepresentation map = ((ExploreDFSAgent) this.myAgent).getMap();
		
		if (map == null) {
			map = new FullMapRepresentation(true);
			((ExploreDFSAgent) this.myAgent).setMap(map);
		}

		try {
			this.myAgent.doWait(1000); // Just added here so we can see what the agent is doing
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Retrieve the current position and the list of observations
		String myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
		List<Couple<String,List<Couple<Observation,Integer>>>> lobs = ((AbstractDedaleAgent) this.myAgent).observe();

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
		
		// Update nodesToShare
		((ExploreDFSAgent) this.myAgent).addNodeToShare(myPosition);
		
		// Update next position

		if((((ExploreDFSAgent)this.myAgent).getMap().hasOpenNode())) {
			if (nextNodeId == null) {
				//no directly accessible openNode
				//chose one, compute the path and take the first step.
				nextNodeId = map.getShortestPathToClosestOpenNode(myPosition).get(0);
			}
			((ExploreDFSAgent) this.myAgent).setNextNodeId(nextNodeId);
		}

	}
	
	@Override
	public boolean done() {
		return true;
	}

}
