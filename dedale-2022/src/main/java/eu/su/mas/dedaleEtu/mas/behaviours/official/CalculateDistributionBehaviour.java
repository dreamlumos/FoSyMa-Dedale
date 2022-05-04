package eu.su.mas.dedaleEtu.mas.behaviours.official;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.paukov.combinatorics3.Generator;

import eu.su.mas.dedaleEtu.mas.agents.official.ExploreDFSAgent;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;

public class CalculateDistributionBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = -687749337806691639L;

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
		
		// Generating all possible coalitions
		ArrayList<List<String>> goldCoalitions = new ArrayList<List<String>>(); // diamondCoalitions will be inferred as the complement of goldCoalitions
		HashMap<String, ArrayList<Integer>> knownAgentCharacteristics = ((ExploreDFSAgent) this.myAgent).getKnownAgentCharacteristics();
		Set<String> knownAgents = knownAgentCharacteristics.keySet();
		int nbAgents = knownAgents.size();
		for (int i=1; i<nbAgents; i++) {
			Generator.combination(knownAgents)
		       .simple(i)
		       .stream()
		       .forEach(goldCoalitions::add);
		}
		
		// Calculating the best combination of gold and diamond coalitions
		int bestValue = 0;
		List<String> bestGoldCoalition;
		List<String> bestDiamondCoalition;
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
		
		// Generating best possible distribution of gold
		// TODO
				
	}
	
	public List<List<List<String>>> partitions(List<String> listToPartition){
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
                parts.get(i&1).add(item);
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
		// TODO Auto-generated method stub
		return false;
	}

}
