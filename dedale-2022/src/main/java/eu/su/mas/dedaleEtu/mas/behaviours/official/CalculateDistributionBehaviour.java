package eu.su.mas.dedaleEtu.mas.behaviours.official;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.paukov.combinatorics3.Generator;

import eu.su.mas.dedaleEtu.mas.agents.official.ExploreDFSAgent;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;

import static java.util.Collections.max;

public class CalculateDistributionBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = -687749337806691639L;
	
	private int collectingPhaseOver = 0; // 1 when over, 0 otherwise

	public CalculateDistributionBehaviour(Agent agent) {
		super(agent);
	}

	@Override
	public void action() {

		HashMap<String, Integer> goldDict = ((ExploreDFSAgent) this.myAgent).getGoldDict();
		HashMap<String, Integer> diamondDict = ((ExploreDFSAgent) this.myAgent).getDiamondDict();

		int totalGold = 0;
		int totalDiamond = 0;
		for (Integer goldAmount: goldDict.values()) {
			totalGold += goldAmount;
		}
		for (Integer diamondAmount: diamondDict.values()) {
			totalDiamond += diamondAmount;
		}
		
		List<String> goldAgents = ((ExploreDFSAgent) myAgent).getGoldAgents();
		List<String> diamondAgents = ((ExploreDFSAgent) myAgent).getDiamondAgents();
		
		HashMap<String, ArrayList<Integer>> knownAgentCharacteristics = ((ExploreDFSAgent) this.myAgent).getKnownAgentCharacteristics();

		if (goldAgents != null && diamondAgents != null) {
			int totalGoldCapacity = 0;
			for (String agent: goldAgents) {
				totalGoldCapacity += knownAgentCharacteristics.get(agent).get(0);
			}
			int totalDiamondCapacity = 0;
			for (String agent: diamondAgents) {
				totalDiamondCapacity += knownAgentCharacteristics.get(agent).get(1);
			}
			
			if ((totalGold <= 0 || totalGoldCapacity <= 0) && (totalDiamond <= 0 || totalDiamondCapacity <= 0)) {
				this.collectingPhaseOver = 1;
				return;
			}
		}

		// Generating all possible coalitions
		// A coalition here refers to the group of agents that will be in charge of collecting a certain type of treasure
		ArrayList<List<String>> goldCoalitions = new ArrayList<List<String>>(); // diamondCoalitions will be inferred as the complement of goldCoalitions
		Set<String> knownAgents = knownAgentCharacteristics.keySet();
		int nbAgents = knownAgents.size();
		for (int i = 1; i < nbAgents; i++) {
			Generator.combination(knownAgents)
					.simple(i)
					.stream()
					.forEach(goldCoalitions::add);
		}

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
			for (String agent: goldCoalition) {
				goldCapacity += knownAgentCharacteristics.get(agent).get(0);
			}
			int diamondCapacity = 0;
			for (String agent: diamondCoalition) {
				diamondCapacity += knownAgentCharacteristics.get(agent).get(1);
			}
			int totalTreasure = Math.min(goldCapacity, totalGold) + Math.min(diamondCapacity, totalDiamond);
			if (totalTreasure > bestValue) {
				bestGoldCoalition = goldCoalition;
				bestDiamondCoalition = diamondCoalition;
			}
		}

//		// Generating best possible distribution of gold
//		// TODO
//		// Zoe : my code starts here
//		List<List<List<String>>> goldPartitions = partitions(bestGoldCoalition);
//		List<List<List<String>>> diamondPartitions = partitions(bestDiamondCoalition);
//
//		List<List<String>> bestGoldPartition;
//		List<String> goldToPick;
//		int bestTotalGold = 0;
//		for (List<List<String>> partition : goldPartitions) { // [ [a1, a2], [a3] ]
//			//TODO
//			List<Integer> partGoldCap = new ArrayList<>(); // [ a1.getCap + a2.goldCap, a3.goldCap]
//			for (List<String> p : partition) { // [a1, a2] || [a3]
//				ArrayList<String> goldList = new ArrayList<>(goldDict.keySet());
//				int goldCap = 0;
//				for (String a : p) {
//					goldCap += knownAgentCharacteristics.get(a).get(0);
//				}
//				partGoldCap.add(goldCap);
//				int currentTotalGold = 0;
//				List<String> currentNodeToPick = new ArrayList<>();
//				while (!partGoldCap.isEmpty()) { // assigning nodes to pick to the partition
//					int max = max(partGoldCap); // the group of agents with the biggest gold cap choose first
//					partGoldCap.remove(max);
//					int partBestGold = 0;
//					String nodeToPick = null;
//					for (String i : goldList) {
//						if (goldDict.get(i) <= max && goldDict.get(i) > partBestGold) {
//							partBestGold = goldDict.get(i);
//							nodeToPick = i;
//						}
//					}
//					currentTotalGold += partBestGold; // total amount of gold that the partition picks
//					currentNodeToPick.add(nodeToPick);
//					if (nodeToPick != null) {
//						goldList.remove(nodeToPick); // removing the node that the group with the biggest gold cap will pick
//					}
//				}
//				if (currentTotalGold > bestTotalGold) {
//					bestTotalGold = currentTotalGold;
//					bestGoldPartition = partition;
//					goldToPick = currentNodeToPick;
//				}
//			}
//		}
		/*
		this.myAgent.setGoldPartition(bestGoldPartition);
		this.myAgent.setNodeToPick(goldToPick);
		this.myAgent.setOrder(); // once we have the best partition we need to set the order in which the agents will pick up the gold
		//should create a hashmap with <partition: nodesToPick>
		also once the agent calculates the gold vs dia coalition, he knows which he is and thus doesn't need to compute the best partition for the other type of ore
		*/
		// Zoe : my code ends here
		
		
		// Kiara's version
		List<List<List<String>>> goldPartitions = partitions(bestGoldCoalition);
		List<List<List<String>>> diamondPartitions = partitions(bestDiamondCoalition);

		List<List<String>> bestGoldPartition = new ArrayList<List<String>>();
		int bestGoldValue = 0;
		List<List<String>> bestDiamondPartition = new ArrayList<List<String>>();
		int bestDiamondValue = 0;
		
		// Gold
		for (List<List<String>> partition: goldPartitions) {
			
			// For each partition, we generate the permutations
			List<List<List<String>>> partitionPermutations = new ArrayList<List<List<String>>>();
			int nbGoldNodes = goldDict.size();
			Generator.combination(partition)
		       .simple(nbGoldNodes)
		       .stream()
		       .forEach(combination -> Generator.permutation(combination)
		          .simple()
		          .forEach(partitionPermutations::add));
			
			// Finding the best permutation for this particular partition
			int permutationValue;
			for (List<List<String>> permutation: partitionPermutations) {
				permutationValue = 0;
				for (Integer goldAmount: goldDict.values()) {
					permutationValue += goldAmount;
				}
				if (permutationValue > bestGoldValue) {
					bestGoldValue = permutationValue;
					bestGoldPartition = permutation;
				}
			}
		}
		// Diamond
		for (List<List<String>> partition: diamondPartitions) {
			
			// For each partition, we generate the permutations
			List<List<List<String>>> partitionPermutations = new ArrayList<List<List<String>>>();
			int nbDiamondNodes = diamondDict.size();
			Generator.combination(partition)
		       .simple(nbDiamondNodes)
		       .stream()
		       .forEach(combination -> Generator.permutation(combination)
		          .simple()
		          .forEach(partitionPermutations::add));
			
			// Finding the best permutation for this particular partition
			int permutationValue;
			for (List<List<String>> permutation: partitionPermutations) {
				permutationValue = 0;
				for (Integer diamondAmount: diamondDict.values()) {
					permutationValue += diamondAmount;
				}
				if (permutationValue > bestDiamondValue) {
					bestDiamondValue = permutationValue;
					bestDiamondPartition = permutation;
				}
			}
		}
		
		HashMap<String, List<String>> treasureAttributions = new HashMap<String, List<String>>();
		
		for (String nodeId: goldDict.keySet()) {
			treasureAttributions.put(nodeId, bestGoldPartition.remove(0));
		}
		for (String nodeId: diamondDict.keySet()) {
			treasureAttributions.put(nodeId, bestDiamondPartition.remove(0));
		}
		
		((ExploreDFSAgent) this.myAgent).setTreasureAttributions(treasureAttributions);
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

	@Override
	public boolean done() {
		return true;
	}
	
	@Override
	public int onEnd() {
		return collectingPhaseOver;
	}
}
