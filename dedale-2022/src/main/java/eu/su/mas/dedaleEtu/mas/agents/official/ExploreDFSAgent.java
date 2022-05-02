package eu.su.mas.dedaleEtu.mas.agents.official;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import eu.su.mas.dedale.env.EntityCharacteristics;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.startMyBehaviours;
import eu.su.mas.dedaleEtu.mas.behaviours.official.CheckForPingBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.official.CheckForPongBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.official.ExploCoopFullMapBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.official.ObserveEnvBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.official.PingBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.official.ReceiveMapBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.official.StepBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.FullMapRepresentation;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;

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

public class ExploreDFSAgent extends AbstractDedaleAgent {

	private static final long serialVersionUID = -7969469610241668140L;
	
	private FullMapRepresentation myMap; 
	private EntityCharacteristics myCharacteristics;
	private HashMap<String, ArrayList<String>> nodesToShare; // key: agent name, value: list of IDs of the nodes to be shared next time we meet this agent
	private ArrayList<String> knownAgents;
//	private HashMap<String, EntityCharacteristics> knownAgentCharacteristics;
	private HashMap<String, ArrayList<Integer>> knownAgentCharacteristics;

	private String nextNodeId;
	
	private static final String ObserveEnv = "Observe Environment";
	private static final String Step = "Step";
	private static final String Ping = "Ping";
	private static final String CheckForPong = "CheckForPong";
	
	private static final String CheckForPing = "Check Mailbox for Ping";
	private static final String ReceiveMap = "Receive Map";
	
	
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
		
		List<String> unnecessaryAgentsList = List.of("sniffeur", "GK", "rma", "ams", "df");
		for (int i = 0; i< agentsDescriptionCatalog.length; i++){  // modified agentsDescriptionCatalog
			AID agentID = agentsDescriptionCatalog[i].getName();
			String agentName = agentID.getLocalName();
			if (!unnecessaryAgentsList.contains(agentName)) {
				agentsNames.add(agentName);
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
		
		FSMPingPong.registerDefaultTransition(CheckForPing, CheckForPing); //K: if it works as I expect, this line is useless, and the default could just be CFPing -> ReceiveMap
		FSMPingPong.registerTransition(CheckForPing, ReceiveMap, 1);
		FSMPingPong.registerDefaultTransition(ReceiveMap, CheckForPing);
		
		lb.add(FSMPingPong);
		
		FSMBehaviour FSMExploCollect = new FSMBehaviour(this);
	
		FSMExploCollect.registerFirstState(new ObserveEnvBehaviour(this), ObserveEnv);
		FSMExploCollect.registerState(new StepBehaviour(this), Step);
		FSMExploCollect.registerState(new PingBehaviour(this), Ping);
		FSMExploCollect.registerState(new CheckForPongBehaviour(this), CheckForPong);
		//fsm.registerState(new CollectTreasureBehaviour(), CollectTreasure);
		//fsm.registerLastState(new ?(), ?);
		
		FSMExploCollect.registerDefaultTransition(ObserveEnv, Step);
		FSMExploCollect.registerDefaultTransition(Step, Ping);
		FSMExploCollect.registerDefaultTransition(Ping, CheckForPong);
		FSMExploCollect.registerDefaultTransition(CheckForPong, ObserveEnv);
//		fsm.registerTransition(B, B, 2) ; //Cond 2
//		fsm.registerTransition(B, C, 1) ; //Cond 1
		
		lb.add(FSMExploCollect);
		
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

	public EntityCharacteristics getMyCharacteristics(){
		return myCharacteristics;
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
}