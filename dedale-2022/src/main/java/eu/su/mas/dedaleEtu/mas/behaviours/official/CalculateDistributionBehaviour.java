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

	public CalculateDistributionBehaviour(Agent agent) {
		super(agent);
	}

	@Override
	public void action() {
		System.out.println("THIS IS CalculateDistribution BEHAVIOUR");

		HashMap<String, Integer> goldDict = ((ExploreDFSAgent) this.myAgent).getGoldDict();
		HashMap<String, Integer> diamondDict = ((ExploreDFSAgent) this.myAgent).getDiamondDict();

		int totalGold = 0;
		int totalDiamond = 0;
		for (Integer goldAmount : goldDict.values()) {
//			System.out.println("Computing the gold amount");
			totalGold += goldAmount;
		}
		System.out.println("Total gold : " + totalGold);
		for (Integer diamondAmount : diamondDict.values()) {
			totalDiamond += diamondAmount;
		}

		List<String> goldAgents = ((ExploreDFSAgent) myAgent).getGoldAgents();
		List<String> diamondAgents = ((ExploreDFSAgent) myAgent).getDiamondAgents();

		knownAgentCharacteristics = ((ExploreDFSAgent) this.myAgent).getKnownAgentCharacteristics();

		if (goldAgents != null || diamondAgents != null){

			int totalGoldCapacity = 0;
			int totalDiamondCapacity = 0;
			if(goldAgents != null) {
				System.out.println("But we go here tho?");
				System.out.println("Length : " + goldAgents.size());
				for (String agent : goldAgents) {
					totalGoldCapacity += knownAgentCharacteristics.get(agent).get(0);
					System.out.println("Agent chars : " + knownAgentCharacteristics.get(agent));
				}
				System.out.println("Total Gold Cap : " + totalGoldCapacity);
			}
			if(diamondAgents != null){

				for (String agent : diamondAgents) {
					totalDiamondCapacity += knownAgentCharacteristics.get(agent).get(1);
				}
			}

			if ((totalGold <= 0 || totalGoldCapacity <= 0) && (totalDiamond <= 0 || totalDiamondCapacity <= 0)) {
				System.out.println("Are we passing here?");
				this.collectingPhaseOver = 1;
				return;
			}

		} else {

			// Generating all possible coalitions
			// A coalition here refers to the group of agents that will be in charge of collecting a certain type of treasure
			ArrayList<List<String>> goldCoalitions = new ArrayList<List<String>>(); // diamondCoalitions will be inferred as the complement of goldCoalitions
			Set<String> knownAgents = knownAgentCharacteristics.keySet();
			int nbAgents = knownAgents.size();
			for (int i = 0; i < nbAgents; i++) {
				Generator.combination(knownAgents)
						.simple(i)
						.stream()
						.forEach(goldCoalitions::add);
			}


			// Calculating the best combination of gold and diamond coalitions
			int bestValue = 0;
			List<String> bestGoldCoalition = new ArrayList<String>();
			List<String> bestDiamondCoalition = new ArrayList<String>();
			for (List<String> goldCoalition : goldCoalitions) {
				// Obtaining the diamond coalition (complement of the gold coalition)
				List<String> diamondCoalition = new ArrayList<String>(knownAgents);
				diamondCoalition.removeAll(goldCoalition);
				// Calculating total capacity for this combination of coalitions
				int goldCapacity = 0;
				for (String agent : goldCoalition) {
					goldCapacity += knownAgentCharacteristics.get(agent).get(0);
				}
				int diamondCapacity = 0;
				for (String agent : diamondCoalition) {
					diamondCapacity += knownAgentCharacteristics.get(agent).get(1);
				}
				int totalTreasure = Math.min(goldCapacity, totalGold) + Math.min(diamondCapacity, totalDiamond);
				if (totalTreasure > bestValue) {
					bestGoldCoalition = goldCoalition;
					bestDiamondCoalition = diamondCoalition;
				}
			}
			((ExploreDFSAgent) this.myAgent).setGoldAgents(bestGoldCoalition);
			((ExploreDFSAgent) this.myAgent).setDiamondAgents(bestDiamondCoalition);
			if (bestGoldCoalition.contains(((ExploreDFSAgent) this.myAgent).getLocalName())) {
				((ExploreDFSAgent) this.myAgent).setType("Gold");
			} else {
				((ExploreDFSAgent) this.myAgent).setType("Diamond");
			}

		}

		// Kiara's version

		List<List<List<String>>> goldPartitions = partitions(((ExploreDFSAgent) this.myAgent).getGoldAgents());
		List<List<List<String>>> diamondPartitions = partitions(((ExploreDFSAgent) this.myAgent).getDiamondAgents());

//		List<List<String>> bestGoldPartition = new ArrayList<List<String>>();
//		List<List<String>> bestDiamondPartition = new ArrayList<List<String>>();

		HashMap<String, List<String>> treasureAttributions = new HashMap<String, List<String>>();

		switch (((ExploreDFSAgent) this.myAgent).getType()) {

			/* Gold */
			case "Gold":
				List<List<String>> bestGoldPartition = this.bestPartition(goldPartitions, goldDict);
				for (String nodeId : goldDict.keySet()) {
					List<String> currGroup = bestGoldPartition.remove(0);

					if(currGroup.contains(this.myAgent.getLocalName())){
						int total = diamondDict.get(nodeId);
						int ownCap = knownAgentCharacteristics.get(this.myAgent.getLocalName()).get(0);

						int nodeValue = valueToPick(currGroup, total, ownCap, 0);
						Pair<String, Integer> toPick = new Pair<>(nodeId, nodeValue);
						((ExploreDFSAgent)this.myAgent).setCurrTreasureToPick(toPick);
					}
					treasureAttributions.put(nodeId, currGroup);
				}
				((ExploreDFSAgent) this.myAgent).setTreasureAttributions(treasureAttributions);
				this.computed = true;

				// update the goldDict for next round
				HashMap<String, Integer> newGoldDict = new HashMap<>();
				for(String nodeId: goldDict.keySet()){
					if(treasureAttributions.containsKey(nodeId)){
						int newValue = goldDict.get(nodeId);
						for(String v: treasureAttributions.get(nodeId)){
							newValue -= knownAgentCharacteristics.get(v).get(0);
						}
						if(newValue < 0){
							newValue = 0;
						}
						newGoldDict.put(nodeId, newValue);
					}else{
						newGoldDict.put(nodeId, goldDict.get(nodeId));
					}
				}
				((ExploreDFSAgent)this.myAgent).setGoldDict(newGoldDict);

				// update knownAgentsCharacteristics
				HashMap<String, ArrayList<Integer>> newKnownAgentCharacteristics = new HashMap<>();
				for(String nodeId: goldDict.keySet()) {
					if (treasureAttributions.containsKey(nodeId)) {
						HashMap<Integer, List<String>> sortedAgents = new HashMap<>();
						for (String v : treasureAttributions.get(nodeId)) {
							int val = knownAgentCharacteristics.get(v).get(0);
							if(sortedAgents.containsKey(val)){
								List<String> agents = sortedAgents.get(val);
								agents.add(v);
								sortedAgents.put(val, agents);
							}
							ArrayList<String> agents = new ArrayList<>();
							agents.add(v);
							sortedAgents.put(val, agents);
						}
						List<Integer> sorted = new ArrayList<>(sortedAgents.keySet());
						sorted.sort(Collections.reverseOrder()); // should be in descending order
						int currGold = goldDict.get(nodeId);
						for(Integer i: sorted){
							for(String s: sortedAgents.get(i)) {
								if(currGold <= 0){
									currGold = 0;
								}
								List<Integer> knownValues = knownAgentCharacteristics.get(s);
								int newGoldVal = knownValues.get(0) - currGold;
								currGold -= i;
								ArrayList<Integer> newChars = new ArrayList<>();
								newChars.add(newGoldVal);
								newChars.add(knownValues.get(1));
								newChars.add(knownValues.get(2));

								newKnownAgentCharacteristics.put(s, newChars);
							}

						}
					}
				}
				((ExploreDFSAgent)this.myAgent).setKnownAgentCharacteristics(newKnownAgentCharacteristics);

				break;
//		for (List<List<String>> partition: goldPartitions) {
//
//			// For each partition, we generate the permutations
//			List<List<List<String>>> partitionPermutations = new ArrayList<List<List<String>>>();
//			int nbGoldNodes = goldDict.size();
//			Generator.combination(partition)
//		       .simple(nbGoldNodes)
//		       .stream()
//		       .forEach(combination -> Generator.permutation(combination)
//		          .simple()
//		          .forEach(partitionPermutations::add));
//
//			// Finding the best permutation for this particular partition
//			int permutationValue;
//			for (List<List<String>> permutation: partitionPermutations) {
//				permutationValue = 0;
//				for (Integer goldAmount: goldDict.values()) {
//					permutationValue += goldAmount;
//				}
//				if (permutationValue > bestGoldValue) {
//					bestGoldValue = permutationValue;
//					bestGoldPartition = permutation;
//				}
//			}
//		}
			/* Diamond */
			case "Diamond":
				List<List<String>> bestDiamondPartition = this.bestPartition(diamondPartitions, diamondDict);
				for (String nodeId : diamondDict.keySet()) {
					List<String> currGroup = bestDiamondPartition.remove(0);

					if(currGroup.contains(this.myAgent.getLocalName())){
						int total = diamondDict.get(nodeId);
						int ownCap = knownAgentCharacteristics.get(this.myAgent.getLocalName()).get(1);

					int nodeValue = valueToPick(currGroup, total, ownCap, 1);
					Pair<String, Integer> toPick = new Pair<>(nodeId,nodeValue);
					((ExploreDFSAgent)this.myAgent).setCurrTreasureToPick(toPick);
					}
					treasureAttributions.put(nodeId, currGroup);
				}

				((ExploreDFSAgent) this.myAgent).setTreasureAttributions(treasureAttributions);
				this.computed = true;

				// update the diamondDict for next round
				HashMap<String, Integer> newDiamondDict = new HashMap<>();
				for(String nodeId: diamondDict.keySet()){
					if(treasureAttributions.containsKey(nodeId)){
						int newValue = diamondDict.get(nodeId);
						for(String v: treasureAttributions.get(nodeId)){
							newValue -= knownAgentCharacteristics.get(v).get(0);
						}
						newDiamondDict.put(nodeId, newValue);
					}else{
						newDiamondDict.put(nodeId, diamondDict.get(nodeId));
					}
				}
				((ExploreDFSAgent)this.myAgent).setDiamondDict(newDiamondDict);

				break;

			default:
				System.out.println("Type : " + ((ExploreDFSAgent) this.myAgent).getType());
				System.out.println("Error in setting up the treasure attribution : " + this.myAgent.getLocalName()+ " doesn't appear to have a type");
//		for (List<List<String>> partition: diamondPartitions) {
//
//			// For each partition, we generate the permutations
//			List<List<List<String>>> partitionPermutations = new ArrayList<List<List<String>>>();
//			int nbDiamondNodes = diamondDict.size();
//			Generator.combination(partition)
//		       .simple(nbDiamondNodes)
//		       .stream()
//		       .forEach(combination -> Generator.permutation(combination)
//		          .simple()
//		          .forEach(partitionPermutations::add));
//
//			// Finding the best permutation for this particular partition
//			int permutationValue;
//			for (List<List<String>> permutation: partitionPermutations) {
//				permutationValue = 0;
//				for (Integer diamondAmount: diamondDict.values()) {
//					permutationValue += diamondAmount;
//				}
//				if (permutationValue > bestDiamondValue) {
//					bestDiamondValue = permutationValue;
//					bestDiamondPartition = permutation;
//				}
//			}
//		}
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

	public List<List<String>> bestPartition(List<List<List<String>>> partitions, HashMap<String, Integer> dict) {
		List<List<String>> bestPartition = new ArrayList<List<String>>();
		int bestValue = 0;
		for (List<List<String>> p : partitions) {
			// For each partition, we generate the permutations
//			List<List<List<String>>> partitionPermutations = new ArrayList<List<List<String>>>();
			int nbNodes = dict.size();
//			Generator.combination(p)
//					.simple(nbNodes)
//					.stream()
//					.forEach(combination -> Generator.permutation(combination)
//							.simple()
//							.forEach(partitionPermutations::add));
			List<List<List<String>>> partitionPermutations = this.combinations(p, nbNodes);
			// Finding the best permutation for this particular partition
			int permutationValue;
			for (List<List<String>> permutation : partitionPermutations) {
				permutationValue = 0;
				for (Integer oreAmount : dict.values()) {
					permutationValue += oreAmount;
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
		List<List<List<String>>> partitionPermutations = new ArrayList<List<List<String>>>();
		Generator.combination(p)
				.simple(dictSize)
				.stream()
				.forEach(combination -> Generator.permutation(combination)
						.simple()
						.forEach(partitionPermutations::add));
		return partitionPermutations;
	}

	/*
	* Computes the max amount of ore of the node assigned to the agent that it can pick up, otherwise it will wait and try again later
	*  */
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

	@Override
	public boolean done() {
		System.out.println("computed : " + this.computed);
		return this.computed;
	}
	
	@Override
	public int onEnd() {
		System.out.println("oneEnd : " + this.collectingPhaseOver);
		return collectingPhaseOver;
	}
}
