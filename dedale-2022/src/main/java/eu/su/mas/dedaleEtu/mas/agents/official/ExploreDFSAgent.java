package eu.su.mas.dedaleEtu.mas.agents.official;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import eu.su.mas.dedale.env.EntityCharacteristics;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.startMyBehaviours;
import eu.su.mas.dedaleEtu.mas.behaviours.official.*;
import eu.su.mas.dedaleEtu.mas.knowledge.FullMapRepresentation;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.DataStore;
import jade.core.behaviours.FSMBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.lang.acl.ACLMessage;
import javafx.util.Pair;
import org.glassfish.pfl.basic.fsm.FSM;


/**
 * <pre>
 * ExploreCoop agent. 
 * Basic example of how to "collaboratively" explore the map
 *  - It explore the map using a DFS algorithm and blindly tries to share the topology with the agents within reach.
 *  - The shortestPath computation is not optimized
 *  - Agents do not coordinate themselves on the node(s) to visit, thus progressively creating a single file. It's bad.
 *  - The agent sends all its map, periodically, forever. Its bad x3.
 *  - You should give him the list of agents'name to send its map to in parameter when creating the agent.
 *   Object [] entityParameters={"Name1","Name2};
 *   ag=createNewDedaleAgent(c, agentName, ExploreCoopAgent.class.getName(), entityParameters);
 *  
 * It stops when all nodes have been visited.
 * 
 *  </pre>
 *  
 */

// TODO fix the getPartialMap() error with addNode
// TODO verify all the timeOut
// TODO : handle interlocking 5 tries and go away
// TODO check if they jump around to non neighbour nodes

// TODO plan for situations where the wumpus has moved gold (or for some reason an agent has already picked up some gold/all the gold)
// TODO one of the things we haven't done is actually manage things when we send out multiple pings
// TODO : in the nodeToShare dict, only add the nodes that we are visiting for the first time MAYBE ?
// TODO: if agent 1 finishes exploration and calculates the plan, then meets agent 2 that hasn't finished exploring, agent 1 send the plan to agent 2 along with the map?

// TODO find out how to end the agents
// TODO empty the mailbox?


public class ExploreDFSAgent extends AbstractDedaleAgent {

	private static final long serialVersionUID = -7969469610241668140L;

	private FullMapRepresentation myMap; 
	private EntityCharacteristics myCharacteristics;
	private HashMap<String, ArrayList<String>> nodesToShare; // key: agent name, value: list of IDs of the nodes to be shared next time we meet this agent
	private ArrayList<String> knownAgents;
//	private HashMap<String, EntityCharacteristics> knownAgentCharacteristics;
	private HashMap<String, ArrayList<Integer>> knownAgentCharacteristics = new HashMap<>(); // gold cap, dia cap, comm radius
	private ACLMessage currentPong = null;
	private String nextNodeId;

	private String type = null;
//	private HashMap<String, Integer> currTreasureToPick = null; // <nodeId, expected treasure value> // biggest cap first
	private Pair<String, Integer> currTreasureToPick = null; // <nodeId, expected treasure value> // biggest cap first


	private HashMap<String, List<String>> treasureAttribution = new HashMap<>();
	private List<String> goldAgents = null;
	private List<String> diamondAgents = null;


	private static final String ObserveEnv = "Observe Environment";
	private static final String Step = "Step";
	private static final String Ping = "Ping";
	private static final String CheckForPong = "Check For Pong";
	
	private static final String CheckForPing = "Check Mailbox for Ping";
	private static final String SharePartialMap = "Share Partial Map";
	private static final String ReceiveMap = "Receive Map";
	private static final String ReceiveCharacteristics = "Receive Characteristics";
	private static final String ShareCharacteristics = "Share Characteristics";
	private static final String CalculateDistribution = "Calculate Distribution";
	private static final String CollectTreasure = " Collect Treasure";

	private List<Behaviour> listBehavTemp;
	
	
	/**
	 * This method is automatically called when "agent".start() is executed.
	 * Consider that Agent is launched for the first time. 
	 * 			1) set the agent attributes 
	 *	 		2) add the behaviours
	 *          
	 */
	protected void setup(){

		super.setup();
		
		// Get the parameters added to the agent at creation (in this case, the EntityCharacteristics file)
		final Object[] args = getArguments();
				
		if (args.length == 0) {
			System.err.println("Error while creating the agent, entity configuration file expected.");
			System.exit(-1);
		} else {
			String entityCharFile = (String) args[2];
			myCharacteristics = (EntityCharacteristics) AbstractDedaleAgent.loadEntityCaracteristics(getLocalName(), entityCharFile)[0];
		}
		ArrayList<Integer> values = new ArrayList<>();
		values.add(myCharacteristics.getGoldCapacity());
		values.add(myCharacteristics.getDiamondCapacity());
		values.add(myCharacteristics.getCommunicationReach());
		knownAgentCharacteristics.put(this.getLocalName(), values);
		System.out.println("knownAgentChar:"+ knownAgentCharacteristics.get(this.getLocalName()));

		// Getting the list of all agents on the platform
		AMSAgentDescription[] agentsDescriptionCatalog = null;
		List <String> agentsNames = new ArrayList<String>();
		try {
			SearchConstraints c = new SearchConstraints();
			c.setMaxResults(Long.valueOf("-1")); // copied from example in slides, but not sure it is correct as it means to return a max of -1 results
			agentsDescriptionCatalog = AMSService.search(this, new AMSAgentDescription(), c);
		} catch (Exception e) {
			System.out.println("Problem searching AMS: " + e);
			e.printStackTrace();
		}
		
		List<String> unnecessaryAgentsList = List.of("sniffeur", "GK", "rma", "ams", "df", this.getLocalName());
		for (int i = 0; i < agentsDescriptionCatalog.length; i++){  // modified agentsDescriptionCatalog
			AID agentID = agentsDescriptionCatalog[i].getName();
			String agentName = agentID.getLocalName();
			if (!unnecessaryAgentsList.contains(agentName)) {
					agentsNames.add(agentName);
//				}
			}
		}
		
		// Initialising dictionary containing the list of nodes to be shared with each agent		
		this.nodesToShare = new HashMap<String, ArrayList<String>>();
		
		for (String agent: agentsNames) {
			this.nodesToShare.put(agent, new ArrayList<String>());
		}
		
		// Agent behaviours
		List<Behaviour> lb = new ArrayList<Behaviour>();
		
		/************************************************
		 * 
		 * ADD the behaviours of the Dummy Moving Agent
		 * 
		 ************************************************/
		
		FSMBehaviour FSMPingPong = new FSMBehaviour();
		
		FSMPingPong.registerFirstState(new CheckForPingBehaviour(this), CheckForPing);
		FSMPingPong.registerState(new ReceiveMapBehaviour(this), ReceiveMap);
		FSMPingPong.registerState(new ReceiveCharacteristics(this), ReceiveCharacteristics);
		
		FSMPingPong.registerDefaultTransition(CheckForPing, CheckForPing); //K: if it works as I expect, this line is useless, and the default could just be CFPing -> ReceiveMap
		FSMPingPong.registerTransition(CheckForPing, ReceiveMap, 1);
		FSMPingPong.registerDefaultTransition(ReceiveMap, CheckForPing);
		FSMPingPong.registerTransition(CheckForPing, ReceiveCharacteristics, 2);
		FSMPingPong.registerDefaultTransition(ReceiveCharacteristics, ReceiveMap);

		lb.add(FSMPingPong);
		
		FSMBehaviour FSMExploCollect = new FSMBehaviour(this);
		FSMExploCollect.registerFirstState(new ObserveEnvBehaviour(this), ObserveEnv);
		FSMExploCollect.registerState(new PingBehaviour(this), Ping);
		FSMExploCollect.registerState(new StepBehaviour(this), Step);
//		FSMExploCollect.registerState(new CheckForPongBehaviour(this), CheckForPong);
		FSMExploCollect.registerState(new SharePartialMapBehaviour(this, this.currentPong), SharePartialMap);
		FSMExploCollect.registerState(new CheckForPongUnknown(this), CheckForPong);
		FSMExploCollect.registerState(new ShareCharacteristics(this, this.currentPong), ShareCharacteristics);
		FSMExploCollect.registerState(new CollectAssignedTreasure(this), CollectTreasure);
		FSMExploCollect.registerState(new CalculateDistributionBehaviour(this), CalculateDistribution);
		//fsm.registerLastState(new ?(), ?);
		
		FSMExploCollect.registerDefaultTransition(ObserveEnv, Step);
		FSMExploCollect.registerDefaultTransition(Step, Ping);
		FSMExploCollect.registerDefaultTransition(Ping, CheckForPong);
		FSMExploCollect.registerDefaultTransition(CheckForPong, ObserveEnv);
//		FSMExploCollect.registerTransition(Step, Ping, 0);
		FSMExploCollect.registerTransition(CheckForPong, SharePartialMap, 1);
		FSMExploCollect.registerDefaultTransition(SharePartialMap, ObserveEnv); // Zoe: not sure on this !!
		FSMExploCollect.registerTransition(CheckForPong, ShareCharacteristics, 2);
		FSMExploCollect.registerTransition(ShareCharacteristics, CheckForPong, 1);

		// In treasure collecting phase,
		// 0. Inside step, if we finished visiting the map (no more open nodes) or time is out, we don't make a step but instead return a specific onEnd value to indicate that we need to move into CalculateDistributionBehaviour
		// 1. Calculate Distribution
		// 2. Move towards treasure (could probably use StepBehaviour) 
		// 3. (Attempt to) pick up treasure
		// 4. Update our knowledge (treasure values and agent capacities) - not sure how we can do this, might need a strategy to visit all treasure nodes but if the treasure nodes are far this would be disastrous
		// 5. Go back to Calculate Distribution if backpack capacity not maxed and there is still remaining treasure (taking into consideration the agent types)	

		FSMExploCollect.registerTransition(Step, CalculateDistribution, 1);
		FSMExploCollect.registerTransition(Step, Step, 2);
		FSMExploCollect.registerTransition(Step, CollectTreasure, 3);
		FSMExploCollect.registerTransition(CalculateDistribution, Step, 0);
		//FSMExploCollect.registerTransition(CalculateDistribution, ObserveEnv, 1);
//		FSMExploCollect.registerTransition(CalculateDistribution, FinalState, 1);

		// FSMExploCollect.registerTransition(CalculateDistribution, End, 1); // Idk if we need an end behaviour, idk how we call the function doDelete() on the agents once we're done

		
		// TODO: might need states/transitions where we deal with cases like moving but failing, trying to pick up treasure but the treasure not being there
		
		
		lb.add(FSMExploCollect);

		listBehavTemp = lb;
		String s = FSMExploCollect.stringifyTransitionTable();
		System.out.println(s);
		
		/***
		 * MANDATORY TO ALLOW YOUR AGENT TO BE DEPLOYED CORRECTLY
		 */
		addBehaviour(new startMyBehaviours(this, lb));
		
		System.out.println("the  agent "+this.getLocalName()+ " is started");
	}
	
	public void setNextNodeId(String nodeId) {
		this.nextNodeId = nodeId;
	}
	
	public String getNextNodeId() {
		return this.nextNodeId;
	}
	
	public FullMapRepresentation getMap() {
		return this.myMap;
	}

	public void setMap(FullMapRepresentation map) {
		this.myMap = map;
	}

	/* Get the list of nodes to share with a certain agent. */
	public ArrayList<String> getNodesToShare(String agentName){
		return this.nodesToShare.get(agentName);
	}
	
	public void addNodeToShare(String nodeId) {
		for (String agentName: this.nodesToShare.keySet()) {
			ArrayList<String> nodesList = this.nodesToShare.get(agentName);
			nodesList.add(nodeId);
			// this.nodesToShare.put(agentName, nodesList);
		}
	}
	
	public void clearNodesToShare(String agentId) {
		this.nodesToShare.put(agentId, new ArrayList<String>());
	}
	
	/* Get the list of agents whom we should ping (agents to whom we have new information to share). */
	public ArrayList<String> getAgentsToPing(){
		
		ArrayList<String> agentsToPing = new ArrayList<String>();
		
		// Obtaining the list of agents whose nodesToShare list is not empty
		for (String agent: this.nodesToShare.keySet()) {
			if (!this.nodesToShare.get(agent).isEmpty()){
				agentsToPing.add(agent);
			}
		}
		
		return agentsToPing;
	}

	public HashMap<String, Integer> getGoldDict(){
		return this.myMap.getGoldDict();
	}

	public void setGoldDict(HashMap<String, Integer> goldDict){
		this.myMap.setGoldDict(goldDict);
	}

	public void setDiamondDict(HashMap<String, Integer> diamondDict){
		this.myMap.setDiamondDict(diamondDict);
	}

	public HashMap<String, Integer> getDiamondDict(){
		return this.myMap.getDiamondDict();
	}
	
	public EntityCharacteristics getMyCharacteristics(){
		return myCharacteristics;
	}

	public ArrayList<String> getKnownAgents(){
		return knownAgents;
	}
	
	public ArrayList<Integer> getKnownAgentCharacteristics(String agentName){
		return knownAgentCharacteristics.get(agentName);
	}
	
	public HashMap<String, ArrayList<Integer>> getKnownAgentCharacteristics(){
		return knownAgentCharacteristics;
	}

	public void updateKnownCharacteristics(String agent, String msg){

		String[] splitArray = msg.split(" ");
		ArrayList<Integer> charac = new ArrayList<>();
		for (int j = 0; j < 3; j++) {
			charac.add(Integer.parseInt(splitArray[j])); // gold cap, dia cap, comm radius
		}
		this.knownAgentCharacteristics.put(agent, charac);
	}

	public void setKnownAgentCharacteristics(HashMap<String, ArrayList<Integer>> newKnown){
		this.knownAgentCharacteristics = newKnown;
	}

//	public Pair<String, Integer> calculateCoalition(int size, ArrayList<String> agents, HashMap<String, Integer> gold){
//		// im not using the size param bc i don't know how to compute all the combination possible of size k parmi n...
//
//		HashMap<String, Integer> goldCapDict = new HashMap<>(); // key : agents in the coal, value : gold capacity of agents
//		for(String a : agents) {
//			int cap = (this.knownAgentCharacteristics.get(a)).get(0);
//			goldCapDict.put(a, cap);
//		}
//
//		// All of this is for ONE coalition...
//		int currCoalCap = 0;
//		ArrayList<String> currCoal = new ArrayList<>(); // temp TODO need to use a combination function... tried a lib but no luck
//		for(String a : currCoal){
//			currCoalCap += goldCapDict.get(a);
//		}
//		String bestNode = "";
//		int bestGold = 0;
//		ArrayList<String> goldList = new ArrayList<>(gold.keySet());
//		for(String i : goldList){
//			if(gold.get(i) <= currCoalCap && gold.get(i)>bestGold){
//				// here to make a thoughtful choice we would need to check the position of the two different gold spots and see which is closest to currentPos
//				bestNode = i;
//				bestGold = gold.get(i);
//			}
//		}
//		return new Pair<>(bestNode, bestGold);
//	}
//
////	public ArrayList<String> combinations(int size ArrayList<String> agents){
////
////	}
//
//	public String pairToString(Pair<String, Integer> coal){
//
//		String msg = coal.toString();
//
//		msg = msg.replace("[", "")
//				.replace("]", " ")
//				.replace(",", "");
//		msg += coal.toString();
//
//		return msg;
//	}

	public String getListBehavTemp(){
		StringBuilder s = new StringBuilder();
		for(Behaviour b: listBehavTemp){
			s.append(b.getBehaviourName());
			s.append("; ");
		}
		return s.toString();
	}

	public void setCurrTreasureToPick(Pair<String, Integer> toPick){
		this.currTreasureToPick = toPick;
		System.out.println("Current treasure to pick: "+toPick);
	}

	public Pair<String, Integer> getCurrTreasureToPick(){
		return this.currTreasureToPick;
	}

	public void setType(String type){
		System.out.println("Setting Agent "+this.getLocalName()+"'s treasure type: "+type);
		if (this.type == null) {
			this.type = type;
		}
	}

	public String getType(){
		return this.type;
	}

	public void setCurrentPong(ACLMessage currentPong) {
		this.currentPong = currentPong;
	}
	
	public void setTreasureAttributions(HashMap<String, List<String>> treasureAttribution) {
		this.treasureAttribution = treasureAttribution;
	}
	
	public List<String> getGoldAgents() {
		return this.goldAgents;
	}
	
	public void setGoldAgents(List<String> goldAgents) {
		this.goldAgents = goldAgents;
	}
	
	public List<String> getDiamondAgents() {
		return this.diamondAgents;
	}
	
	public void setDiamondAgents(List<String> diamondAgents) {
		this.diamondAgents = diamondAgents;
	}
}