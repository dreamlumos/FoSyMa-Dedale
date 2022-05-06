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

		ExploreDFSAgent myAgent = (ExploreDFSAgent) this.myAgent;

		String nextNodeId = null;
		boolean moveSuccessful = false;
		if (phase == 0) { // We are in the exploration phase
			
			// Check if map is fully explored OR the time is up
			if (!(myAgent.getMap().hasOpenNode()) || System.currentTimeMillis() > timeOutDate){
				phase = 1; // We will now calculate the treasure distribution
			} else {
			
				String myPosition = myAgent.getCurrentPosition();
				nextNodeId = myAgent.getNextNodeId(); // should maybe try catch a null node here?
	
				System.out.println("Agent " + this.myAgent.getLocalName() + " is moving to " + nextNodeId);
				moveSuccessful = myAgent.moveTo(nextNodeId);
				while (!moveSuccessful) {
					List<Couple<String,List<Couple<Observation,Integer>>>> lobs = myAgent.observe();
					if (Objects.equals(nextNodeId, lobs.get(0).getLeft())) {
						nextNodeId = lobs.get(1).getLeft();
					} else {
						nextNodeId = lobs.get(0).getLeft();
					}
					System.out.println("Agent " + this.myAgent.getLocalName() + " is moving randomly to " + nextNodeId);
					moveSuccessful = myAgent.moveTo(nextNodeId);
				}
			}
		} else if (phase == 1) {
			phase = 2; // We switch to the collecting phase
		} else if (phase == 2) { 
			System.out.println("StepBehaviour: Collecting phase");
			if (shortestPathToPick.isEmpty()) {
				String myPosition = myAgent.getCurrentPosition();
				if (nodeToPick == null) { // treasure to pick hasn't been decided
					nodeToPick = myAgent.getCurrTreasureToPick().getKey();
					shortestPathToPick = myAgent.getMap().getShortestPath(myPosition, nodeToPick);

				} else if (Objects.equals(myPosition, nodeToPick)) {
					phase = 3; // agent arrived at destination, starts collectBehaviour
				}
			} else {
				nextNodeId = shortestPathToPick.remove(0);
				System.out.println("Agent " + this.myAgent.getLocalName() + " is moving to " + nextNodeId);
				moveSuccessful = myAgent.moveTo(nextNodeId);
				while (!moveSuccessful) {
					List<Couple<String,List<Couple<Observation,Integer>>>> lobs = myAgent.observe();
					if (Objects.equals(nextNodeId, lobs.get(0).getLeft())) {
						nextNodeId = lobs.get(1).getLeft();
					} else {
						nextNodeId = lobs.get(0).getLeft();
					}
					System.out.println("Agent " + this.myAgent.getLocalName() + " is moving randomly to " + nextNodeId);
					moveSuccessful = myAgent.moveTo(nextNodeId);
					if (moveSuccessful) {
						shortestPathToPick = myAgent.getMap().getShortestPath(nextNodeId, nodeToPick);
						if (shortestPathToPick == null) { // treasure unreachable, we skip to next collect round
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
		System.out.println("StepBehaviour onEnd phase : " + phase);
		return phase;
	}


}
