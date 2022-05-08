package eu.su.mas.dedaleEtu.mas.behaviours.official;

import eu.su.mas.dedale.env.Observation;
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

	private int unsuccessfulMovesCollect = 0;

	public StepBehaviour(ExploreDFSAgent agent) {
		super(agent);
		timeOutDate = System.currentTimeMillis() + 199999999; //60000; // 60000; // 180000;
	}
	
	@Override
	public void action() {

//		System.out.println("THIS IS STEP BEHAVIOUR");

		ExploreDFSAgent myAgent = (ExploreDFSAgent) this.myAgent;
		int unsuccessfulMovesExplo = myAgent.getUnsuccessfulMovesExplo();
		String nextNodeId = null;
		boolean moveSuccessful = false;
		if (phase == 0) { // We are in the exploration phase
			
			// Check if map is fully explored OR the time is up
			if (!(myAgent.getMap().hasOpenNode()) || System.currentTimeMillis() > timeOutDate){
				if (!(myAgent.getMap().hasOpenNode()) ) {
					System.out.println("[StepBehaviour] "+myAgent.getLocalName()+" is going to phase 1 because map is fully explored.");
				} else {
					System.out.println("[StepBehaviour] "+myAgent.getLocalName()+" is going to phase 1 because time is out.");
				}
				phase = 1; // We will now calculate the treasure distribution
			} else {
			
				String myPosition = myAgent.getCurrentPosition();
				System.out.println("[StepBehaviour] Agent " + this.myAgent.getLocalName() + " is at node " + myPosition);
				nextNodeId = myAgent.getNextNodeId(); // should maybe try catch a null node here?
	
				System.out.println("[StepBehaviour] Agent " + this.myAgent.getLocalName() + " is moving to " + nextNodeId);
				moveSuccessful = myAgent.moveTo(nextNodeId);
				while (!moveSuccessful) {
//					unsuccessfulMovesExplo ++;
					myAgent.updateUnsuccessfulMovesExplo(1);
					if(myAgent.getUnsuccessfulMovesExplo() > 4){

						break;
					}
					System.out.println("[StepBehaviour] Phase of exploration for " + this.myAgent.getLocalName());
					System.out.println("[StepBehaviour] Agent " + this.myAgent.getLocalName() + " is at node " + myPosition);
					System.out.println("[StepBehaviour] Agent " + this.myAgent.getLocalName() + " wants to move to " + nextNodeId + " but is blocked!");
					List<Couple<String,List<Couple<Observation,Integer>>>> lobs = myAgent.observe();
					for(Couple<String,List<Couple<Observation,Integer>>> obs: lobs){
						if(!Objects.equals(obs.getLeft(), nextNodeId)){
							if(!Objects.equals(obs.getLeft(), myPosition)) {
//								System.out.println("");
//								System.out.println(myPosition);
//								System.out.println(obs.getLeft());
//								System.out.println(nextNodeId);
								nextNodeId = obs.getLeft();
								break;
							}
						}
					}
//					if (Objects.equals(nextNodeId, lobs.get(0).getLeft())) {
//						nextNodeId = lobs.get(1).getLeft();
//					} else {
//						nextNodeId = lobs.get(0).getLeft();
//					}

					System.out.println("[StepBehaviour] Agent " + this.myAgent.getLocalName() + " is moving randomly to " + nextNodeId);
					moveSuccessful = myAgent.moveTo(nextNodeId);
				}
			}
		} else if (phase == 1) {
			phase = 2; // We switch to the collecting phase
			System.out.println("_________________________________________");
		} else if (phase == 2) {
			System.out.println("[StepBehaviour] Collecting phase for " + this.myAgent.getLocalName());
			System.out.println(myAgent.getGoldAgents());
			System.out.println(myAgent.getDiamondAgents());
			if (shortestPathToPick.isEmpty()) {
				String myPosition = myAgent.getCurrentPosition();
				if (nodeToPick == null) { // treasure to pick hasn't been decided
					if(myAgent.getCurrTreasureToPick() != null) {
						nodeToPick = myAgent.getCurrTreasureToPick().getKey();
						shortestPathToPick = myAgent.getMap().getShortestPath(myPosition, nodeToPick);
					}else{
						this.phase = 3;
					}
				} else if (Objects.equals(myPosition, nodeToPick)) {
					phase = 3; // agent arrived at destination, starts collectBehaviour
				}
			} else {
				String myPosition = myAgent.getCurrentPosition();
				System.out.println("[StepBehaviour] Agent " + this.myAgent.getLocalName() + " is at node " + myPosition);
				String goalNode = shortestPathToPick.get(shortestPathToPick.size()-1);
				nextNodeId = shortestPathToPick.remove(0);
//				if (shortestPathToPick.size()>0) {
//					System.out.println("[StepBehaviour] "+shortestPathToPick.get(0));
//				}
				System.out.println("[StepBehaviour] Agent " + this.myAgent.getLocalName() + " is moving to " + nextNodeId);
				moveSuccessful = myAgent.moveTo(nextNodeId);
				while (!moveSuccessful) {
					if(unsuccessfulMovesCollect > 4){
						// recompute a new treasure node to reach
						switch(((ExploreDFSAgent) this.myAgent).getType()){
							case "Gold":
								HashMap<String, Integer> oldGoldDict = ((ExploreDFSAgent) this.myAgent).getMap().getGoldDict();
								HashMap<String, Integer> newGoldDict = new HashMap<>();
								List<String> goldNodes = new ArrayList<>(oldGoldDict.keySet());
								for(String n: goldNodes){
									if(!Objects.equals(n, nextNodeId)){
										newGoldDict.put(n, oldGoldDict.get(n)); // we remove the blocked node from the list of treasure nodes
									}
								}
								((ExploreDFSAgent) this.myAgent).setGoldDict(newGoldDict);
								break;
							case "Diamond":
								HashMap<String, Integer> oldDiaDict = ((ExploreDFSAgent) this.myAgent).getMap().getDiamondDict();
								HashMap<String, Integer> newDiaDict = new HashMap<>();
								List<String> diaNodes = new ArrayList<>(oldDiaDict.keySet());
								for(String n: diaNodes){
									if(!Objects.equals(n, nextNodeId) || !Objects.equals(n, goalNode)){
										newDiaDict.put(n, oldDiaDict.get(n)); // we remove the blocked node from the list of treasure nodes
									}
								}
								((ExploreDFSAgent) this.myAgent).setDiamondDict(newDiaDict);
								break;
							default:
								break;
						}
						this.phase = 1; // re-compute a treasure path
						break;
					}
					unsuccessfulMovesCollect++;
//					System.out.println("[StepBehaviour] Phase of collect for " + this.myAgent.getLocalName());
					System.out.println("[StepBehaviour] Agent " + this.myAgent.getLocalName() + " is at node " + myPosition);
					System.out.println("[StepBehaviour] Agent " + this.myAgent.getLocalName() + " wants to move to " + nextNodeId + " but is blocked!");
					List<Couple<String,List<Couple<Observation,Integer>>>> lobs = myAgent.observe();
//					if (Objects.equals(nextNodeId, lobs.get(0).getLeft())) {
					for (Couple<String,List<Couple<Observation,Integer>>> obs: lobs) {
//						if(!(Objects.equals(obs.getLeft(), nextNodeId)) && !(Objects.equals(obs.getLeft(), myPosition))){
//							nextNodeId = obs.getLeft();
//						}
						if (!Objects.equals(obs.getLeft(), nextNodeId)) {
							if (!Objects.equals(obs.getLeft(), myPosition)) {
//								System.out.println("");
//								System.out.println(myPosition);
//								System.out.println(obs.getLeft());
//								System.out.println(nextNodeId);
								nextNodeId = obs.getLeft();
								break;
							}
						}
					}
					System.out.println("[StepBehaviour] Agent " + this.myAgent.getLocalName() + " is moving randomly to " + nextNodeId);
					moveSuccessful = myAgent.moveTo(nextNodeId);
					if (moveSuccessful) {
						shortestPathToPick = myAgent.getMap().getShortestPath(nextNodeId, nodeToPick);
						if (shortestPathToPick == null) { // treasure unreachable, we skip to next collect round
							this.phase = 1;
						}
					}
				}
			}
		} else if (phase == 3) {
			phase = 1;
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
		//System.out.println("[StepBehaviour] onEnd phase : " + phase);
		return phase;
	}
}
