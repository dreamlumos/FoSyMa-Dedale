package eu.su.mas.dedaleEtu.mas.behaviours.official;

import java.util.*;
import java.util.stream.Collectors;

import javafx.util.Pair;
import org.paukov.combinatorics3.Generator;

import eu.su.mas.dedaleEtu.mas.agents.official.ExploreDFSAgent;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;

import static java.util.Collections.max;

public class CalculateDistributionBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = -687749337806691639L;

	private boolean computed = false;
	private int collectingPhaseOver = 0; // 1 when over, 0 otherwise
	private HashMap<String, ArrayList<Integer>> knownAgentCharacteristics;
	private ExploreDFSAgent myAgent;

	public CalculateDistributionBehaviour(Agent agent) {
		super(agent);
		this.myAgent = (ExploreDFSAgent) agent; // so that we don't need to cast this.myAgent every time
	}

	@Override
	public void action() {
		
		HashMap<String, Integer> goldDict = this.myAgent.getGoldDict();
		HashMap<String, Integer> diamondDict = this.myAgent.getDiamondDict();

		int totalGold = 0;
		int totalDiamond = 0;
		for (Integer goldAmount : goldDict.values()) {
//			System.out.println("Computing the gold amount");
			totalGold += goldAmount;
		}
		System.out.println("[CalculateDistribution] Total gold : " + totalGold);

		for (Integer diamondAmount: diamondDict.values()) {
			totalDiamond += diamondAmount;
		}
		System.out.println("[CalculateDistribution] Total diamond : " + totalDiamond);

		List<String> goldAgents = this.myAgent.getGoldAgents();
		List<String> diamondAgents = this.myAgent.getDiamondAgents();

		knownAgentCharacteristics = this.myAgent.getKnownAgentCharacteristics();

		String type = this.myAgent.getType();
		if (type != null){ // if we have already calculated the type of each agent
			
			int totalCapacity = 0;
			int totalTreasure = 0;
			
			if (type == "Gold") {
				totalTreasure = totalGold;
				for (String agent: goldAgents) {
					totalCapacity += knownAgentCharacteristics.get(agent).get(0);
				}
				System.out.println("[CalculateDistribution] Gold dict: "+goldDict);
			} else if (type == "Diamond") {
				totalTreasure = totalDiamond;
				for (String agent: diamondAgents) {
					totalCapacity += knownAgentCharacteristics.get(agent).get(1);
				}
				System.out.println("[CalculateDistribution] Diamond dict: "+diamondDict);
			}

			//System.out.println("[CalculateDistribution] Total Cap : " + totalCapacity);

			if ((totalTreasure <= 0 || totalCapacity <= 0)) {
				this.collectingPhaseOver = 1;
				System.out.println("[CalculateDistribution] "+this.myAgent.getLocalName()+": Collecting phase is over.");
				return;
			}

		} else {

			// Generating all possible coalitions
			// A coalition here refers to the group of agents that will be in charge of collecting a certain type of treasure
			ArrayList<List<String>> goldCoalitions = new ArrayList<List<String>>(); // diamondCoalitions will be inferred as the complement of goldCoalitions
			List<String> knownAgents = new ArrayList<>(knownAgentCharacteristics.keySet());
			int nbAgents = knownAgents.size();
			for (int i = 1; i < nbAgents+1; i++) {
				Generator.combination(knownAgents)
						.simple(i)
						.stream()
						.forEach(goldCoalitions::add);
			}
//			System.out.println(goldCoalitions);

			// Calculating the best combination of gold and diamond coalitions
			int bestValue = 0;
			List<String> bestGoldCoalition = new ArrayList<String>();
			List<String> bestDiamondCoalition = new ArrayList<String>();
			for (List<String> goldCoalition: goldCoalitions) {
				// Obtaining the diamond coalition (complement of the gold coalition)
				List<String> diamondCoalition = new ArrayList<String>(knownAgents);
				diamondCoalition.removeAll(goldCoalition);

				// Calculating total capacity for this combination of coalitions
				int goldCapacity = 0;
				for (String agent : goldCoalition) {
					goldCapacity += knownAgentCharacteristics.get(agent).get(0);
					//System.out.println("[CalculateDistribution] goldCapacity: "+goldCapacity);
				}
				int diamondCapacity = 0;
				for (String agent: diamondCoalition) {
					diamondCapacity += knownAgentCharacteristics.get(agent).get(1);
					//System.out.println("[CalculateDistribution] diamondCapacity: "+diamondCapacity);
				}
				int totalTreasure = Math.min(goldCapacity, totalGold) + Math.min(diamondCapacity, totalDiamond);
				//System.out.println("[CalculateDistribution] totalTreasure: " + totalTreasure);

				if (totalTreasure > bestValue) {
					bestGoldCoalition = goldCoalition;
					bestDiamondCoalition = diamondCoalition;
					bestValue = totalTreasure;
				}
			}
			this.myAgent.setGoldAgents(bestGoldCoalition);
			this.myAgent.setDiamondAgents(bestDiamondCoalition);
			if (bestGoldCoalition.contains(this.myAgent.getLocalName())) {
				this.myAgent.setType("Gold");
			} else {
				this.myAgent.setType("Diamond");
			}
		}

		List<List<List<String>>> goldPartitions = partitions(this.myAgent.getGoldAgents());
		List<List<List<String>>> diamondPartitions = partitions(this.myAgent.getDiamondAgents());

		HashMap<String, List<String>> treasureAttributions = new HashMap<String, List<String>>();

		switch (this.myAgent.getType()) {

			case "Gold":
				
				List<String> goldDictKeys = new ArrayList<String>(goldDict.keySet());
				List<List<String>> bestGoldPartition = this.bestPartition(goldPartitions, goldDict, goldDictKeys);
								
				for (String nodeId: goldDictKeys) {
					if (!bestGoldPartition.isEmpty()) {
						List<String> currGroup = bestGoldPartition.remove(0);
						if (currGroup.contains(this.myAgent.getLocalName())) {
							int total = goldDict.get(nodeId);
							int ownCap = knownAgentCharacteristics.get(this.myAgent.getLocalName()).get(0);
	
							int nodeValue = valueToPick(currGroup, total, ownCap, 0);
							Pair<String, Integer> toPick = new Pair<>(nodeId, nodeValue);
							this.myAgent.setCurrTreasureToPick(toPick);
						}
						treasureAttributions.put(nodeId, currGroup);
					}
				}
				this.myAgent.setTreasureAttributions(treasureAttributions);
				this.computed = true;

				updateKnowledge(goldDict, treasureAttributions, 0);

				break;

			case "Diamond":
				List<String> diamondDictKeys = new ArrayList<String>(diamondDict.keySet());
				List<List<String>> bestDiamondPartition = this.bestPartition(diamondPartitions, diamondDict, diamondDictKeys);
				for (String nodeId : diamondDictKeys) {
					if (!bestDiamondPartition.isEmpty()) {
						List<String> currGroup = bestDiamondPartition.remove(0);
						if (currGroup.contains(this.myAgent.getLocalName())){
							int total = diamondDict.get(nodeId);
							int ownCap = knownAgentCharacteristics.get(this.myAgent.getLocalName()).get(1);
	
							int nodeValue = valueToPick(currGroup, total, ownCap, 1);
							Pair<String, Integer> toPick = new Pair<>(nodeId,nodeValue);
							this.myAgent.setCurrTreasureToPick(toPick);
						}
						treasureAttributions.put(nodeId, currGroup);
					}
				}

				this.myAgent.setTreasureAttributions(treasureAttributions);
				this.computed = true;

				updateKnowledge(diamondDict, treasureAttributions, 1);

				break;

			default:
				System.out.println("[CalculateDistribution] Type : " + this.myAgent.getType());
				System.out.println("[CalculateDistribution] Error in setting up the treasure attribution : " + this.myAgent.getLocalName()+ " doesn't appear to have a type");

		}
	}

	public List<List<List<String>>> partitions(List<String> listToPartition) {
		List<List<List<String>>> res = new ArrayList<>();
		if (listToPartition.isEmpty()) {
			res.add(new ArrayList<>());
			return res;
		}

		int limit = 1 << (listToPartition.size() - 1);

		for (int j = 0; j < limit; ++j) {
			List<List<String>> parts = new ArrayList<>();
			List<String> part1 = new ArrayList<>();
			List<String> part2 = new ArrayList<>();
			parts.add(part1);
			parts.add(part2);
			int i = j;
			for (String item : listToPartition) {
				parts.get(i & 1).add(item);
				i >>= 1;
			}
			for (List<List<String>> b : partitions(part2)) {
				List<List<String>> holder = new ArrayList<>();
				holder.add(part1);
				holder.addAll(b);
				res.add(holder);
			}
		}
		return res;
	}

	public List<List<String>> bestPartition(List<List<List<String>>> partitions, HashMap<String, Integer> treasureDict, List<String> treasureDictKeys) {
		
		List<List<String>> bestPartition = new ArrayList<List<String>>();
		int bestValue = 0;
		
		String agentType = this.myAgent.getType();
		int treasureType;
		if (Objects.equals(agentType, "Gold")) {
			treasureType = 0;
		} else {
			treasureType = 1;
		}
		
		for (List<List<String>> p : partitions) {
			// For each partition, we generate the permutations
//			List<List<List<String>>> partitionPermutations = new ArrayList<List<List<String>>>();
			int nbNodes = treasureDict.size();

//			Generator.combination(p)
//					.simple(nbNodes)
//					.stream()
//					.forEach(combination -> Generator.permutation(combination)
//							.simple()
//							.forEach(partitionPermutations::add));
			List<List<List<String>>> partitionPermutations = this.combinations(p, nbNodes);
			// Finding the best permutation for this particular partition
			int permutationValue;
			for (List<List<String>> permutation: partitionPermutations) { // permutation: for each treasure node, which agents should go there

				permutationValue = 0;
				for (int nodePos=0; nodePos<nbNodes; nodePos++) {
					int totalCapacity = 0;
					for (String agentName: permutation.get(nodePos)) {
						totalCapacity += this.myAgent.getKnownAgentCharacteristics(agentName).get(treasureType);
					}
					int nodeValue = treasureDict.get(treasureDictKeys.get(nodePos));
					if (nodeValue > totalCapacity) {
						permutationValue += totalCapacity;
					} else {
						permutationValue += nodeValue;
					}
				}
				if (permutationValue > bestValue) {
					bestValue = permutationValue;
					bestPartition = permutation;
				}
			}
		}
		return bestPartition;
	}

	public List<List<List<String>>> combinations(List<List<String>> p, int dictSize){
		while (p.size() < dictSize) {
			p.add(new ArrayList<String>());
		}
		List<List<List<String>>> partitionPermutations = new ArrayList<List<List<String>>>();
		Generator.combination(p)
				.simple(dictSize)
				.stream()
				.forEach(combination -> Generator.permutation(combination)
						.simple()
						//.forEach(System.out::println));
						.forEach(partitionPermutations::add));
		return partitionPermutations;
	}

	/*
	* Computes the max amount of ore of the node assigned to the agent that it can pick up, otherwise it will wait and try again later
	*/
	public int valueToPick(List<String> currGroup, int total, int ownCap, int type) {
		for (String a : currGroup) {
			if (!Objects.equals(this.myAgent.getLocalName(), a)) {
				int cap = knownAgentCharacteristics.get(a).get(type);
				if (cap > ownCap) {
					total -= cap;
				}
			}
		}
		// total is the maximum amount of ore that the spot has to reach if the agent is to pick it up, can be less!
		return total;
	}
	
	/* Updates the agent's knowledge with hypothetical values for the remaining treasure and other agents' remaining capacities.
	* Type 0: gold
	* Type 1: diamond
	*/
	public void updateKnowledge(HashMap<String, Integer> treasureDict, HashMap<String, List<String>> treasureAttributions, int type) {
		
		// update the treasureDict for next round
		HashMap<String, Integer> newTreasureDict = new HashMap<>();
		for (String nodeId: treasureDict.keySet()){
			if (treasureAttributions.containsKey(nodeId)) {
				int newValue = treasureDict.get(nodeId);
				for (String v: treasureAttributions.get(nodeId)) {
					newValue -= knownAgentCharacteristics.get(v).get(type);
				}
				if (newValue < 0) {
					newValue = 0;
				}
				newTreasureDict.put(nodeId, newValue);
			} else {
				newTreasureDict.put(nodeId, treasureDict.get(nodeId));
			}
		}
		this.myAgent.setGoldDict(newTreasureDict);

		// update knownAgentsCharacteristics
		HashMap<String, ArrayList<Integer>> newKnownAgentCharacteristics = new HashMap<>();
		
		// For each node containing treasure of this type
		for (String nodeId: treasureDict.keySet()) {
			if (treasureAttributions.containsKey(nodeId)) {
				// We create a dictionary to sort the agents assigned to this node according to their capacities
				HashMap<Integer, List<String>> sortedAgents = new HashMap<>(); // key: capacity, value: list of agents that have this capacity
				for (String agentId : treasureAttributions.get(nodeId)) {
					int capacity = knownAgentCharacteristics.get(agentId).get(type);
					if (sortedAgents.containsKey(capacity)) {
						List<String> agents = sortedAgents.get(capacity);
						agents.add(agentId);
						sortedAgents.put(capacity, agents);
					}
					ArrayList<String> agents = new ArrayList<>();
					agents.add(agentId);
					sortedAgents.put(capacity, agents);
				}
				
//				if (type == 0) {
					List<Integer> listOfCapacities = new ArrayList<>(sortedAgents.keySet());
					listOfCapacities.sort(Collections.reverseOrder()); // should be in descending order
					int currTreasure = treasureDict.get(nodeId);
					for (Integer i : listOfCapacities) {
						for (String s : sortedAgents.get(i)) {
							if (currTreasure <= 0) {
								currTreasure = 0;
							}
							List<Integer> knownValues = knownAgentCharacteristics.get(s);
							int newTreasureVal = knownValues.get(type) - currTreasure;
							if (newTreasureVal < 0) {
								newTreasureVal = 0;
							}
							currTreasure -= i;
							ArrayList<Integer> newChars = new ArrayList<>();
							
							if (type == 0) {
								newChars.add(newTreasureVal);
								newChars.add(knownValues.get(1));
								newChars.add(knownValues.get(2));
							} else if (type == 1) {
								newChars.add(knownValues.get(0));
								newChars.add(newTreasureVal);
								newChars.add(knownValues.get(2));
							}

							newKnownAgentCharacteristics.put(s, newChars);
						}
					}
//				} else if (type == 1) {
//					List<Integer> sorted = new ArrayList<>(sortedAgents.keySet());
//					sorted.sort(Collections.reverseOrder()); // should be in descending order
//					int currDia = treasureDict.get(nodeId);
//					for (Integer i : sorted) {
//						for (String s : sortedAgents.get(i)) {
//							if (currDia <= 0) {
//								currDia = 0;
//							}
//							List<Integer> knownValues = knownAgentCharacteristics.get(s);
//							int newDiaVal = knownValues.get(type) - currDia;
//							if (newDiaVal < 0) {
//								newDiaVal = 0;
//							}
//							currDia -= i;
//							ArrayList<Integer> newChars = new ArrayList<>();
//							
//
//							newKnownAgentCharacteristics.put(s, newChars);
//						}
//					}
//				}
			}
		}
		this.myAgent.setKnownAgentCharacteristics(newKnownAgentCharacteristics);
	}
	
	@Override
	public boolean done() {
		System.out.println("[CalculateDistribution] Computed : " + this.computed);
		return this.computed;
	}
	
	@Override
	public int onEnd() {
		System.out.println("[CalculateDistribution] onEnd : " + this.collectingPhaseOver);
		return collectingPhaseOver;
	}
}
