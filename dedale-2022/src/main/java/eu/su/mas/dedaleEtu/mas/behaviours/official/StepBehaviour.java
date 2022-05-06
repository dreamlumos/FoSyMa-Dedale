package eu.su.mas.dedaleEtu.mas.behaviours.official;

import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.official.ExploreDFSAgent;
import jade.core.behaviours.SimpleBehaviour;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import dataStructures.tuple.Couple;

public class StepBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = -7075642787451313299L;
	private int phase = 0; // 0: exploration phase, 1: time to start collecting, 2: moving to treasure, 3: return to calculations, 4: blocked
	private long timeOutDate;
	private String nodeToPick = null;
	private List<String> shortestPathToPick = new ArrayList<>();
	private int unsuccessfulMoves = 0;
		
	public StepBehaviour(ExploreDFSAgent agent) {
		super(agent);
		timeOutDate = System.currentTimeMillis() + 180000;
	}
	
	@Override
	public void action() {
		System.out.println("THIS IS STEP BEHAVIOUR");

		// check if map is wholly explored OR the time is up
		if (!(((ExploreDFSAgent)this.myAgent).getMap().hasOpenNode())){
			phase = 1;
		}
		if (System.currentTimeMillis() > timeOutDate){
			phase = 1;
		}
		if (((ExploreDFSAgent)this.myAgent).getCurrTreasureToPick() != null){
			phase = 2;
		}
		String nextNodeId = null;
		boolean moveSuccessful = false;
		if (phase == 0) {
			String myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
			nextNodeId = ((ExploreDFSAgent) myAgent).getNextNodeId(); // should maybe try catch a null node here?

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
			moveSuccessful = ((AbstractDedaleAgent) this.myAgent).moveTo(nextNodeId);
			while(!moveSuccessful){
				List<Couple<String,List<Couple<Observation,Integer>>>> lobs = ((AbstractDedaleAgent) this.myAgent).observe();
				if(Objects.equals(nextNodeId, lobs.get(0).getLeft())) {
					nextNodeId = lobs.get(1).getLeft();
				}else{
					nextNodeId = lobs.get(0).getLeft();
				}
				System.out.println("Agent " + this.myAgent.getLocalName() + " is moving randomly to " + nextNodeId);
				moveSuccessful = ((AbstractDedaleAgent) this.myAgent).moveTo(nextNodeId);
			}
//		}
		} else if (phase == 2) { // we switch to the collect phase
			System.out.println("Step Behaviour phase 2");
			if (shortestPathToPick.isEmpty()) {
				String myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
				if (nodeToPick == null) { // treasure to pick hasn't been decided
					nodeToPick = ((ExploreDFSAgent) this.myAgent).getCurrTreasureToPick().getKey();
					shortestPathToPick = ((ExploreDFSAgent) this.myAgent).getMap().getShortestPath(myPosition, nodeToPick);

				} else if (Objects.equals(myPosition, nodeToPick)) {
						phase = 3; // agent arrived at destination, starts collectBehaviour
					}
			} else {
				nextNodeId = shortestPathToPick.remove(0);
				System.out.println("Agent " + this.myAgent.getLocalName() + " is moving to " + nextNodeId);
				moveSuccessful = ((AbstractDedaleAgent) this.myAgent).moveTo(nextNodeId);
				while(!moveSuccessful){
					List<Couple<String,List<Couple<Observation,Integer>>>> lobs = ((AbstractDedaleAgent) this.myAgent).observe();
					if(Objects.equals(nextNodeId, lobs.get(0).getLeft())) {
						nextNodeId = lobs.get(1).getLeft();
					}else{
						nextNodeId = lobs.get(0).getLeft();
					}
					System.out.println("Agent " + this.myAgent.getLocalName() + " is moving randomly to " + nextNodeId);
					moveSuccessful = ((AbstractDedaleAgent) this.myAgent).moveTo(nextNodeId);
					if(moveSuccessful) {
						shortestPathToPick = ((ExploreDFSAgent) this.myAgent).getMap().getShortestPath(nextNodeId, nodeToPick);
						if(shortestPathToPick == null){ // treasure unreachable, we skip to next collect round
							this.phase = 3;
						}
					}
				}
			}
		}
//		else if (phase == 4) {
//			List<Couple<String,List<Couple<Observation,Integer>>>> lobs = ((AbstractDedaleAgent) this.myAgent).observe();
//			nextNodeId = lobs.get(0).getLeft();
//
//			System.out.println("Agent " + this.myAgent.getLocalName() + " is moving randomly to " + nextNodeId);
//			moveSuccessful = ((AbstractDedaleAgent) this.myAgent).moveTo(nextNodeId);
//
//		}
		
//		if (!moveSuccessful) {
//			this.unsuccessfulMoves++;
//			System.out.println(nextNodeId);
//			if (this.unsuccessfulMoves > 5) {
//				this.phase = 4;
//			}
//		} else {
//			this.unsuccessfulMoves = 0;
//		}
	}

	@Override
	public boolean done() {
		return true;
	}

	@Override
	public int onEnd() {
		System.out.println("phase = " + phase);
		return phase;
	}


}
