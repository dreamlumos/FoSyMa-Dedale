package eu.su.mas.dedaleEtu.mas.behaviours.official;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.FullMapRepresentation;
import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class ExploCoopFullMapBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = 4574851290639462554L;

	private boolean finished = false;

	/**
	 * Current knowledge of the agent regarding the environment
	 */
	private FullMapRepresentation myMap;

	private List<String> list_agentNames;

	/**
	 * 
	 * @param myagent
	 * @param myMap known map of the world the agent is living in
	 * @param agentNames name of the agents to share the map with
	 */
	public ExploCoopFullMapBehaviour(final AbstractDedaleAgent myagent, FullMapRepresentation myMap, List<String> agentNames) {
		super(myagent);
		this.myMap = myMap;
		this.list_agentNames = agentNames;		
	}

	@Override
	public void action() {

		if (this.myMap == null) {
			this.myMap = new FullMapRepresentation(true);
		}

		//0) Retrieve the current position
		String myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();

		if (myPosition != null){
			//List of observable from the agent's current position
			List<Couple<String, List<Couple<Observation,Integer>>>> lobs = ((AbstractDedaleAgent) this.myAgent).observe();//myPosition

			/**
			 * Just added here to let you see what the agent is doing, otherwise he will be too quick
			 */
			try {
				this.myAgent.doWait(1000);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			List<Couple<Observation,Integer>> lObservations = lobs.get(0).getRight(); // observations at current position

			//1) remove the current node from openlist and add it to closedNodes.
			this.myMap.addNode(myPosition, lObservations, System.currentTimeMillis(), -1);

			//2) get the surrounding nodes and, if not in closedNodes, add them to open nodes.
			String nextNode = null;
			
			int nbObs = lobs.size();
			for (int i = 1; i<nbObs; i++) {
				String nodeId = lobs.get(i).getLeft();
				boolean isNewNode = this.myMap.addNode(nodeId, lobs.get(i).getRight(), -1, System.currentTimeMillis());
				// the node may exist, but not necessarily the edge
				this.myMap.addEdge(myPosition, nodeId);
				if (nextNode == null && isNewNode) {
					nextNode = nodeId;
				}
			}

			//3) while openNodes is not empty, continues.
			if (!this.myMap.hasOpenNode()) {
				//Explo finished
				finished=true;
				System.out.println(this.myAgent.getLocalName()+" - Exploration successufully done, behaviour removed.");
			} else {
				//4) select next move.
				//4.1 If there exist one open node directly reachable, go for it,
				//	 otherwise choose one from the openNode list, compute the shortestPath and go for it
				if (nextNode == null) {
					//no directly accessible openNode
					//chose one, compute the path and take the first step.
					nextNode = this.myMap.getShortestPathToClosestOpenNode(myPosition).get(0);//getShortestPath(myPosition,this.openNodes.get(0)).get(0);
					//System.out.println(this.myAgent.getLocalName()+"-- list= "+this.myMap.getOpenNodes()+"| nextNode: "+nextNode);
				} else {
					//System.out.println("nextNode notNUll - "+this.myAgent.getLocalName()+"-- list= "+this.myMap.getOpenNodes()+"\n -- nextNode: "+nextNode);
				}
				
				//4) At each time step, the agent blindly send all its graph to its surrounding to illustrate how to share its knowledge (the topology currently) with the the others agents. 	
				// If it was written properly, this sharing action should be in a dedicated behaviour set, the receivers be automatically computed, and only a subgraph would be shared.
				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				msg.setProtocol("SHARE-TOPO");
				msg.setSender(this.myAgent.getAID());
				if (this.myAgent.getLocalName().equals("1stAgent")) {
					msg.addReceiver(new AID("2ndAgent",false));
				} else {
					msg.addReceiver(new AID("1stAgent",false));
				}
				SerializableSimpleGraph<String, HashMap<String, Object>> sg = this.myMap.getSerializableGraph();
				try {					
					msg.setContentObject(sg);
				} catch (IOException e) {
					e.printStackTrace();
				}
				((AbstractDedaleAgent)this.myAgent).sendMessage(msg);

				//5) At each time step, the agent check if he received a graph from a teammate. 	
				// If it was written properly, this sharing action should be in a dedicated behaviour set.
				MessageTemplate msgTemplate = MessageTemplate.and(
						MessageTemplate.MatchProtocol("SHARE-TOPO"),
						MessageTemplate.MatchPerformative(ACLMessage.INFORM));
				ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
				if (msgReceived != null) {
					SerializableSimpleGraph<String, HashMap<String, Object>> sgreceived = null;
					try {
						sgreceived = (SerializableSimpleGraph<String, HashMap<String, Object>>) msgReceived.getContentObject();
					} catch (UnreadableException e) {
						e.printStackTrace();
					}
					this.myMap.mergeMap(sgreceived);
				}

				((AbstractDedaleAgent)this.myAgent).moveTo(nextNode);
			}

		}
	}

	@Override
	public boolean done() {
		return finished;
	}

}
