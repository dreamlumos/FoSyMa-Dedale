package eu.su.mas.dedaleEtu.mas.behaviours.official;

import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedaleEtu.mas.agents.official.ExploreDFSAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.FullMapRepresentation;
import jade.core.behaviours.SimpleBehaviour;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import dataStructures.tuple.Couple;

public class StepBehaviour2 extends SimpleBehaviour {

	private static final long serialVersionUID = -7075642787451313299L;
	
	ExploreDFSAgent myAgent;
	
	private int phase = 0; 
	// 0: exploration phase
	// 1: time to start collecting
	// 2: moving to treasure
	// 3: return to calculations
	
	private long exploTimeOutDate;
	
	private List<String> path;
	
	private String nodeToPick = null; // node where we are going to next collect treasure
	private List<String> shortestPathToPick = new ArrayList<>();

	private int unsuccessfulMoves = 0;
	private boolean nextClosest = false; // if we have already tried to get the second closest node

	public StepBehaviour2(ExploreDFSAgent agent) {
		super(agent);
		this.myAgent = agent;
		this.exploTimeOutDate = System.currentTimeMillis() + 120000; // 60000; // 180000;
	}
	
	@Override
	/* Exploration policy:
	 * - Move to closest open node if possible (5 attempts)
	 * - TODO: should probably try to reach that node with a diff path before changing objectives
	 * - Else move to NEXT closest open node (5 attempts)
	 * - TODO: would be good to be able to try the third, fourth etc. closest nodes instead of giving up and going random
	 * - Else random move
	 * 
	 * Collection policy:
	 * 
	 */
	public void action() {

		if (this.myAgent.getRestartExplo()) {
			this.exploTimeOutDate = System.currentTimeMillis() + 30000; // 60000; // 180000;
			this.phase = 0;
			this.unsuccessfulMoves = 0;
			this.shortestPathToPick = new ArrayList<>();
			this.nextClosest = false; // if we have already tried to get the second closest node
		}
		
		FullMapRepresentation map = this.myAgent.getMap();
		
		boolean moveSuccessful = false;
		
		if (phase == 0) { // We are in the exploration phase
			
			// Check if map is fully explored OR the time is up
			if (!(map.hasOpenNode()) || System.currentTimeMillis() > exploTimeOutDate){
				if (System.currentTimeMillis() > exploTimeOutDate) {
					System.out.println("[StepBehaviour] "+myAgent.getLocalName()+" is going to phase 1 because time is out.");
					this.myAgent.setExploDone(false);
				} else {
					System.out.println("[StepBehaviour] "+myAgent.getLocalName()+" is going to phase 1 because map is fully explored.");
					this.myAgent.setExploDone(true);
				}
				phase = 1; // We will now calculate the treasure distribution
				return; // Unnecessary but just for readability
				
			} else {
				
				String myPosition = this.myAgent.getCurrentPosition();
				System.out.println("[StepBehaviour] " + this.myAgent.getLocalName() + " is at node " + myPosition);

				// Calculating next step
				boolean makeRandomStep = false;
				String nextNodeId = this.myAgent.getNextNodeId();
				if (this.unsuccessfulMoves >= 5 && !this.nextClosest) {
					// Compute the path to NEXT closest open node
					List<String> pathToClosestOpenNode =  map.getShortestPathToNextClosestOpenNode(myPosition); //TODO: check if this function really works how we want
					if (pathToClosestOpenNode != null) {
						this.path = pathToClosestOpenNode;
						nextNodeId = this.path.get(0);
					} else { // Make a random step (this situation should only happen if there is only one open node left, so there is no "next closest" open node)
						makeRandomStep = true;
					}
					this.nextClosest = true;
				} else if (!this.nextClosest){
					if (nextNodeId == null) { // None of the neighbouring nodes are open
						
						// Compute the path to closest open node
						List<String> pathToClosestOpenNode = map.getShortestPathToClosestOpenNode(myPosition);
						if (pathToClosestOpenNode != null) {
							this.path = pathToClosestOpenNode;
							nextNodeId = this.path.get(0);	
						} else { // Make a random step (this situation should never happen in theory)
							makeRandomStep = true;
							System.out.println("[StepBehaviour] Something weird is going on.");
						}
					}
					this.nextClosest = false;
				} else { // Neither the closest, nor the second closest open nodes are accessible, so we just move randomly
					makeRandomStep = true;
				}
				
				// We move randomly to a neighbour node
				if (makeRandomStep) {
					if (myPosition != null) {
						List<Couple<String,List<Couple<Observation,Integer>>>> lobs = this.myAgent.observe();
						Random r = new Random();
						int moveId = 1 + r.nextInt(lobs.size() - 1); //removing the current position from the list of target, not necessary as to stay is an action but allow quicker random move
						nextNodeId = lobs.get(moveId).getLeft();
					} else {
						System.out.println("[StepBehaviour] myPosition is null.");
					}
				}
				
				// Moving
				System.out.println("[StepBehaviour] " + this.myAgent.getLocalName() + " attempts to move to " + nextNodeId);
				moveSuccessful = myAgent.moveTo(nextNodeId);
				
				this.unsuccessfulMoves = 0;
				// We make max 5 attempts to move to the node
				while (!moveSuccessful && this.unsuccessfulMoves < 5) { 
					System.out.println("[StepBehaviour] " + this.myAgent.getLocalName() + " wants to move to " + nextNodeId + " but is blocked! (attempt to move failed)");
					this.unsuccessfulMoves++;
					moveSuccessful = myAgent.moveTo(nextNodeId);
				}
			}
			
		} else if (phase == 1) {
			phase = 2; // We switch to the collecting phase
			System.out.println("_________________________________________");
			
		} else if (phase == 2) {
			System.out.println("[StepBehaviour] Collecting phase for " + this.myAgent.getLocalName());
			//System.out.println("[StepBehaviour] Calculated gold agents: "+myAgent.getGoldAgents());
			//System.out.println("[StepBehaviour] Calculated diamond agents: "+myAgent.getDiamondAgents());
			if (this.shortestPathToPick == null || this.shortestPathToPick.isEmpty()) {
				String myPosition = myAgent.getCurrentPosition();
				if (this.nodeToPick == null) { // treasure to pick hasn't been decided
					if (this.myAgent.getCurrTreasureToPick() != null) {
						this.nodeToPick = myAgent.getCurrTreasureToPick().getKey();
						this.shortestPathToPick = myAgent.getMap().getShortestPath(myPosition, nodeToPick);
					} else {
						this.phase = 3;
					}
				} else if (Objects.equals(myPosition, nodeToPick)) {
					phase = 3; // agent arrived at destination, starts collectBehaviour
				}
			} else {
				String myPosition = myAgent.getCurrentPosition();
				System.out.println("[StepBehaviour] Agent " + this.myAgent.getLocalName() + " is at node " + myPosition);
				
				String goalNode = shortestPathToPick.get(shortestPathToPick.size()-1);
				String nextNodeId = shortestPathToPick.remove(0);
//				if (shortestPathToPick.size()>0) {
//					System.out.println("[StepBehaviour] "+shortestPathToPick.get(0));
//				}
				System.out.println("[StepBehaviour] Agent " + this.myAgent.getLocalName() + " is moving to " + nextNodeId);
				moveSuccessful = myAgent.moveTo(nextNodeId);
				
				this.unsuccessfulMoves = 0;
				while (!moveSuccessful) {
					if (this.unsuccessfulMoves > 4) {
						// recompute a new treasure node to reach
						HashMap<String, Integer> oldTreasureDict = null;
						switch (this.myAgent.getType()) {
							case "Gold":
								oldTreasureDict = map.getGoldDict();
								break;
							case "Diamond":
								oldTreasureDict = map.getDiamondDict();
								break;
							default:
								break;
						}
						
						HashMap<String, Integer> newTreasureDict = new HashMap<>();
						List<String> treasureNodes = new ArrayList<>(oldTreasureDict.keySet());
						for (String n: treasureNodes) {
							if (!Objects.equals(n, nextNodeId)) {
								newTreasureDict.put(n, oldTreasureDict.get(n)); // we remove the blocked node from the list of treasure nodes
							}
						}
						this.myAgent.setGoldDict(newTreasureDict);
						this.unsuccessfulMoves = 0;
						this.phase = 1; // re-compute a treasure path
						break;
					}
					this.unsuccessfulMoves++;
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
