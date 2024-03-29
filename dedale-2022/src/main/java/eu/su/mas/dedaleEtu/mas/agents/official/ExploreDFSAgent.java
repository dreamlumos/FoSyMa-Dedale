package eu.su.mas.dedaleEtu.mas.agents.official;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import eu.su.mas.dedale.env.EntityCharacteristics;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.startMyBehaviours;
import eu.su.mas.dedaleEtu.mas.behaviours.RandomWalkBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.official.*;
import eu.su.mas.dedaleEtu.mas.knowledge.FullMapRepresentation;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.lang.acl.ACLMessage;
import javafx.util.Pair;


/**
 * <pre>
 * 
 *  </pre>
 *  
 */

// TODO fix the getPartialMap() error with addNode

// TODO plan for situations where the wumpus has moved gold (or for some reason an agent has already picked up some gold/all the gold)
// TODO one of the things we haven't done is actually manage things when we send out multiple pings
// TODO in the nodeToShare dict, only add the nodes that we are visiting for the first time MAYBE ?
// TODO if agent 1 finishes exploration and calculates the plan, then meets agent 2 that hasn't finished exploring, agent 1 send the plan to agent 2 along with the map?

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
	private int unsuccessfulMovesExplo;

	private String type = null;
//	private HashMap<String, Integer> currTreasureToPick = null; // <nodeId, expected treasure value> // biggest cap first
	private Pair<String, Integer> currTreasureToPick = null; // <nodeId, expected treasure value> // biggest cap first

	private HashMap<String, List<String>> treasureAttribution = new HashMap<>();
	private List<String> goldAgents = null;
	private List<String> diamondAgents = null;
	
	private boolean restartExplo = false;
	private boolean exploDone = false;

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
	private static final String Final = "Final";
	private static final String RandomWalkWait = "Random Walk Wait";
	//private static final String RandomWalkFinal = "Random Walk Final";

	private List<Behaviour> listBehavTemp;
	
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
			System.out.println("[ExploreDFSAgent::setup] Problem searching AMS: " + e);
			e.printStackTrace();
		}
		
		List<String> unnecessaryAgentsList = List.of("sniffeur", "GK", "rma", "ams", "df", this.getLocalName());
		for (int i = 0; i < agentsDescriptionCatalog.length; i++){  // modified agentsDescriptionCatalog
			AID agentID = agentsDescriptionCatalog[i].getName();
			String agentName = agentID.getLocalName();
			if (!unnecessaryAgentsList.contains(agentName)) {
					agentsNames.add(agentName);
			}
		}

		unsuccessfulMovesExplo = 0;
		
		// Initialising dictionary containing the list of nodes to be shared with each agent		
		this.nodesToShare = new HashMap<String, ArrayList<String>>();
		
		for (String agent: agentsNames) {
			this.nodesToShare.put(agent, new ArrayList<String>());
		}
		
		// Agent behaviours
		List<Behaviour> lb = new ArrayList<Behaviour>();
		
		/************************************************
		 * 
		 * ADD the behaviours of the Agent
		 * 
		 ************************************************/
		
		/* --------- FSM TO CHECK FOR MESSAGES --------- */
		FSMBehaviour FSMPingPong = new FSMBehaviour();
		
		//BEHAVIOURS
		FSMPingPong.registerFirstState(new CheckForPingBehaviour(this), CheckForPing);
		FSMPingPong.registerState(new ReceiveMapBehaviour(this), ReceiveMap);
		FSMPingPong.registerState(new ReceiveCharacteristics(this), ReceiveCharacteristics);
		
		//TRANSITIONS
		FSMPingPong.registerDefaultTransition(CheckForPing, CheckForPing); //K: if it works as I expect, this line is useless, and the default could just be CFPing -> ReceiveMap
		FSMPingPong.registerTransition(CheckForPing, ReceiveMap, 1);
		FSMPingPong.registerTransition(CheckForPing, ReceiveCharacteristics, 2);
		
		FSMPingPong.registerTransition(ReceiveCharacteristics, CheckForPing, 0);
		FSMPingPong.registerTransition(ReceiveCharacteristics, ReceiveMap, 1);
		
		FSMPingPong.registerDefaultTransition(ReceiveMap, CheckForPing);

		lb.add(FSMPingPong);
		
		/* --------- FSM FOR EXPLORATION AND COLLECTION --------- */
		FSMBehaviour FSMExploCollect = new FSMBehaviour(this);
		
		//BEHAVIOURS
		FSMExploCollect.registerFirstState(new ObserveEnvBehaviour(this), ObserveEnv);
		FSMExploCollect.registerState(new PingBehaviour(this), Ping);
		FSMExploCollect.registerState(new StepBehaviour2(this), Step);
		FSMExploCollect.registerState(new SharePartialMapBehaviour(this), SharePartialMap);
		FSMExploCollect.registerState(new CheckForPongUnknown(this), CheckForPong);
		FSMExploCollect.registerState(new ShareCharacteristics(this), ShareCharacteristics);
		FSMExploCollect.registerState(new CollectAssignedTreasure(this), CollectTreasure);
		FSMExploCollect.registerState(new CalculateDistributionBehaviour(this), CalculateDistribution);
		FSMExploCollect.registerState(new RandomWalkWait(this), RandomWalkWait);
		//FSMExploCollect.registerLastState(new RandomWalkBehaviour(this), RandomWalkFinal);
		FSMExploCollect.registerLastState(new FinalBehaviour(this), Final);
		
		//TRANSITIONS
		FSMExploCollect.registerDefaultTransition(ObserveEnv, Step);
		FSMExploCollect.registerTransition(Step, Ping, 0);
		FSMExploCollect.registerDefaultTransition(Ping, CheckForPong);
		
		FSMExploCollect.registerTransition(CheckForPong, ObserveEnv, 0);
		FSMExploCollect.registerTransition(CheckForPong, SharePartialMap, 1);
		FSMExploCollect.registerTransition(CheckForPong, ShareCharacteristics, 2);

		FSMExploCollect.registerDefaultTransition(ShareCharacteristics, CheckForPong);
		
		FSMExploCollect.registerDefaultTransition(SharePartialMap, ObserveEnv);

		// In treasure collecting phase,
		// 0. Inside step, if we finished visiting the map (no more open nodes) or time is out, we don't make a step but instead return a specific onEnd value to indicate that we need to move into CalculateDistributionBehaviour
		// 1. Calculate Distribution
		// 2. Move towards treasure
		// 3. (Attempt to) pick up treasure
		// 4. Update our knowledge (treasure values and agent capacities) - not sure how we can do this, might need a strategy to visit all treasure nodes but if the treasure nodes are far this would be disastrous
		// 5. Go back to Calculate Distribution if backpack capacity not maxed and there is still remaining treasure (taking into consideration the agent types)	

		FSMExploCollect.registerTransition(Step, CalculateDistribution, 1);
		FSMExploCollect.registerTransition(Step, Step, 2);
		FSMExploCollect.registerTransition(Step, CollectTreasure, 3);
		
		FSMExploCollect.registerTransition(CalculateDistribution, Step, 0);
		FSMExploCollect.registerTransition(CalculateDistribution, Final, 1); // FINAL
		//FSMExploCollect.registerTransition(CalculateDistribution, RandomWalkFinal, 1);
		
		FSMExploCollect.registerDefaultTransition(CollectTreasure, Step);
		FSMExploCollect.registerTransition(CollectTreasure, RandomWalkWait, 1);
		//FSMExploCollect.registerDefaultTransition(CollectTreasure, CalculateDistribution);

		FSMExploCollect.registerDefaultTransition(RandomWalkWait, CollectTreasure);
		
		// TODO: might need states/transitions where we deal with cases like moving but failing, trying to pick up treasure but the treasure not being there
		
		lb.add(FSMExploCollect);

		listBehavTemp = lb;
		String s = FSMExploCollect.stringifyTransitionTable();
		System.out.println(s);
		
		/***
		 * MANDATORY TO ALLOW YOUR AGENT TO BE DEPLOYED CORRECTLY
		 */
		addBehaviour(new startMyBehaviours(this, lb));
		
		System.out.println("[ExploreDFSAgent::setup] The  agent "+this.getLocalName()+ " is started");
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
			if (!nodesList.contains(nodeId)) {
				nodesList.add(nodeId);
			}
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
		System.out.println("[ExploreDFSAgent::setCurrTreasureToPick] Current treasure to pick: "+toPick);
	}

	public Pair<String, Integer> getCurrTreasureToPick(){
		return this.currTreasureToPick;
	}

	public void setType(String type){
		System.out.println("[ExploreDFSAgent::setType] Setting Agent "+this.getLocalName()+"'s treasure type: "+type);
		if (this.type == null) {
			this.type = type;
		}
	}

	public String getType(){
		return this.type;
	}

	public ACLMessage getCurrentPong() {
		return this.currentPong;
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

	public int getUnsuccessfulMovesExplo(){
		return this.unsuccessfulMovesExplo;
	}
	public void updateUnsuccessfulMovesExplo(int newMove){
		this.unsuccessfulMovesExplo += newMove;
	}
	public void setUnsuccessfulMovesExplo(){
		this.unsuccessfulMovesExplo = 0;
	}
	
	public void setRestartExplo(boolean value) {
		this.restartExplo = value;
	}
	public boolean getRestartExplo() {
		return this.restartExplo;
	}
	
	public boolean getExploDone() {
		return this.exploDone;
	}
	
	public void setExploDone(boolean value) {
		this.exploDone = value;
	}
	
	@Override
	public void takeDown() {
		super.takeDown();
	}
	
//	public Pair<String, Integer> calculateCoalition(int size, ArrayList<String> agents, HashMap<String, Integer> gold){
//	// im not using the size param bc i don't know how to compute all the combination possible of size k parmi n...
//
//	HashMap<String, Integer> goldCapDict = new HashMap<>(); // key : agents in the coal, value : gold capacity of agents
//	for(String a : agents) {
//		int cap = (this.knownAgentCharacteristics.get(a)).get(0);
//		goldCapDict.put(a, cap);
//	}
//
//	// All of this is for ONE coalition...
//	int currCoalCap = 0;
//	ArrayList<String> currCoal = new ArrayList<>(); // temp TODO need to use a combination function... tried a lib but no luck
//	for(String a : currCoal){
//		currCoalCap += goldCapDict.get(a);
//	}
//	String bestNode = "";
//	int bestGold = 0;
//	ArrayList<String> goldList = new ArrayList<>(gold.keySet());
//	for(String i : goldList){
//		if(gold.get(i) <= currCoalCap && gold.get(i)>bestGold){
//			// here to make a thoughtful choice we would need to check the position of the two different gold spots and see which is closest to currentPos
//			bestNode = i;
//			bestGold = gold.get(i);
//		}
//	}
//	return new Pair<>(bestNode, bestGold);
//}
//
////public ArrayList<String> combinations(int size ArrayList<String> agents){
////
////}
//
//public String pairToString(Pair<String, Integer> coal){
//
//	String msg = coal.toString();
//
//	msg = msg.replace("[", "")
//			.replace("]", " ")
//			.replace(",", "");
//	msg += coal.toString();
//
//	return msg;
//}
}