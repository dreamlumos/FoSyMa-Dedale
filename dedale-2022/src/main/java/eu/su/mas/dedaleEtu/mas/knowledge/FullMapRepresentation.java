package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Edge;
import org.graphstream.graph.EdgeRejectedException;
import org.graphstream.graph.ElementNotFoundException;
import org.graphstream.graph.Graph;
import org.graphstream.graph.IdAlreadyInUseException;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.view.Viewer;
import dataStructures.serializableGraph.*;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import javafx.application.Platform;

/**
 * Graph and its content.</br>
 * The viewer methods are not independent of the data structure, and the dijkstra is recomputed every-time.
 * 
 * @author zdkg
 */

// TODO -  figuring out how to only send part of the map in the map sharing protocol
// difficulty being needing to code a function in FullMapRepresentation to extract a list of nodes and edges given a list

public class FullMapRepresentation implements Serializable {

	/**
	 * A node is open, closed, or agent
	 */
	public enum NodeStatus {	
		agent, open, closed;
	}

	private static final long serialVersionUID = -1333959882640838272L;

	/*********************************
	 * Parameters for graph rendering
	 ********************************/

	private String defaultNodeStyle= "node {"+"fill-color: black;"+" size-mode:fit;text-alignment:under; text-size:14;text-color:white;text-background-mode:rounded-box;text-background-color:black;}";
	private String nodeStyle_agent = "node.agent {"+"fill-color: forestgreen;"+"}";
	private String nodeStyle_open = "node.open {"+"fill-color: blue;"+"}";
	private String nodeStyle = defaultNodeStyle + nodeStyle_agent + nodeStyle_open;

	private Graph g; //data structure non serializable
	private Viewer viewer; //ref to the display,  non serializable
	private Integer nbEdges; //used to generate the edges ids

	private SerializableSimpleGraph<String, HashMap<String, Object>> sg; //used as a temporary dataStructure during migration


	public FullMapRepresentation() {
		//System.setProperty("org.graphstream.ui.renderer","org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		System.setProperty("org.graphstream.ui", "javafx");
		this.g = new SingleGraph("My world vision");
		this.g.setAttribute("ui.stylesheet", nodeStyle);

//		Platform.runLater(() -> {
//			openGui();
//		});
		//this.viewer = this.g.display();

		this.nbEdges=0;
	}

	/**
	 * Adds a new node or updates an existing node if there is more recent information on its attributes.
	 * Compares timestamps and ignores the information passed in the parameters if it is older than ours.
	 * @param nodeId
	 * @param lObservations list of observations of the visit
	 * @param lastVisitTimestamp timestamp at which the node was visited
	 * @param nbVisitTimestamp timestamp at which a neighbouring node was visited
	 * Either lastVisitTimestamp or nbVisitTimestamp should be -1, thereby indicating whether the observations that are being passed were obtained from visiting 
	 * - the node itself (nbVisitTimestamp = -1), 
	 * - a neighbour (lastVisitTimestamp = -1).
	 */
	public synchronized boolean addNode(String nodeId, 
									List<Couple<Observation, Integer>> lObservations, 
									long lastVisitTimestamp, 
									long nbVisitTimestamp){
		
		boolean newNode = false;
		Node n;
		
		if (this.g.getNode(nodeId) == null){ // Adding new node
			
			n = this.g.addNode(nodeId);
			newNode = true;
			
//			if (lastVisitTimestamp != -1) { // Special case: the node the agent starts on will be immediately closed
//				n.setAttribute("ui.class", NodeStatus.closed.toString());
//			} else {
//				n.setAttribute("ui.class", NodeStatus.open.toString());
//			}
//			
//			n.setAttribute("ui.label", nodeId);
//			n.setAttribute("timestamp", lastVisitTimestamp);
			
		} else { // Updating known node
			n = this.g.getNode(nodeId);
		}
		Object previousTimestamp = n.getAttribute("timestamp");
		Object previousStenchTimestamp = n.getAttribute("stenchTimestamp");

		if (previousTimestamp == null || lastVisitTimestamp > (long) previousTimestamp) {
			n.clearAttributes();
			n.setAttribute("ui.label", nodeId);
			n.setAttribute("timestamp", lastVisitTimestamp);
			if (lastVisitTimestamp != -1) {
				n.setAttribute("ui.class", NodeStatus.closed.toString());
			} else {
				n.setAttribute("ui.class", NodeStatus.open.toString());
			}
		}
		for (Couple<Observation, Integer> o: lObservations){
			Observation observationType = o.getLeft();
			Integer observationValue = o.getRight();
			
			switch (observationType) {
				case DIAMOND:
				case GOLD:
				case LOCKSTATUS:
					if (previousTimestamp == null || lastVisitTimestamp > (long) previousTimestamp) {
						n.setAttribute(observationType.toString(), observationValue);
					}
					break;
				case STENCH:
					if (previousStenchTimestamp == null || nbVisitTimestamp > (long) previousStenchTimestamp) {
						n.setAttribute(observationType.toString(), observationValue);
						n.setAttribute("stenchTimestamp", nbVisitTimestamp);
					}
					break;
				default:
					break;
			}
		}
		return newNode;
	}
	
	/**
	 * Add an undirect edge if not already existing.
	 * @param idNode1
	 * @param idNode2
	 */
	public synchronized void addEdge(String idNode1, String idNode2){
		this.nbEdges++;
		try {
			this.g.addEdge(this.nbEdges.toString(), idNode1, idNode2);
		} catch (IdAlreadyInUseException e1) {
			System.err.println("ID existing");
			System.exit(1);
		} catch (EdgeRejectedException e2) {
			this.nbEdges--;
		} catch(ElementNotFoundException e3){

		}
	}

	/**
	 * Compute the Map to send from ag1's map and a list of nodes.
	 *
	 * @param nodesId list of id of the open nodes from ag2
	 * @return part of ag1's Map to be sent to ag2 to update ag2's Map
	 */

	public FullMapRepresentation getPartialMap(ArrayList<String> nodesId) {
		FullMapRepresentation partialMap = new FullMapRepresentation();
		if (nodesId == null) {
			return partialMap;
		}
		ArrayList<String> nodes = new ArrayList<String>();
		for (String nodeId: nodesId) {
			Node oldNode = this.g.getNode(nodeId);
			Node newNode = partialMap.g.addNode(nodeId);
			nodes.add(nodeId);
			for (Object attribute : oldNode.attributeKeys().toArray()) {
				newNode.setAttribute((String) attribute, oldNode.getAttribute((String) attribute));
			}
		}
		for (String nodeId: nodesId) {
			Node oldNode = this.g.getNode(nodeId);
			for (Object edge : oldNode.edges().toArray()) {
				String edgeId = ((Edge) edge).getId();
				Edge oldEdge = this.g.getEdge(edgeId);
				String node0 = oldEdge.getNode0().getId();
				String node1 = oldEdge.getNode1().getId();
				if (nodes.contains(node0) && nodes.contains(node1)) {
					partialMap.addEdge(node0, node1);
				}
			}
		}
		return partialMap;
	}

	/**
	 * Compute the shortest Path from idFrom to IdTo. The computation is currently not very efficient
	 * 
	 * @param idFrom id of the origin node
	 * @param idTo id of the destination node
	 * @return the list of nodes to follow, null if the targeted node is not currently reachable
	 */
	public synchronized List<String> getShortestPath(String idFrom, String idTo) {
		List<String> shortestPath = new ArrayList<String>();

		Dijkstra dijkstra = new Dijkstra();//number of edge
		dijkstra.init(g);
		dijkstra.setSource(g.getNode(idFrom));
		dijkstra.compute();//compute the distance to all nodes from idFrom
		List<Node> path=dijkstra.getPath(g.getNode(idTo)).getNodePath(); //the shortest path from idFrom to idTo
		Iterator<Node> iter=path.iterator();
		while (iter.hasNext()) {
			shortestPath.add(iter.next().getId());
		}
		dijkstra.clear();
		if (shortestPath.isEmpty()) {//The openNode is not currently reachable
			return null;
		} else {
			shortestPath.remove(0);//remove the current position
		}
		return shortestPath;
	}

	public List<String> getShortestPathToClosestOpenNode(String myPosition) {
		//1) Get all openNodes
		List<String> opennodes=getOpenNodes();

		//2) select the closest one
		List<Couple<String,Integer>> lc=
				opennodes.stream()
				.map(on -> (getShortestPath(myPosition,on)!=null)? new Couple<String, Integer>(on,getShortestPath(myPosition,on).size()): new Couple<String, Integer>(on,Integer.MAX_VALUE))//some nodes my be unreachable if the agents do not share at least one common node.
				.collect(Collectors.toList());

		Optional<Couple<String,Integer>> closest=lc.stream().min(Comparator.comparing(Couple::getRight));
		//3) Compute shorterPath

		return getShortestPath(myPosition,closest.get().getLeft());
	}

	public List<String> getOpenNodes(){
		return this.g.nodes()
				.filter(x ->x .getAttribute("ui.class") == NodeStatus.open.toString()) 
				.map(Node::getId)
				.collect(Collectors.toList());
	}

	/**
	 * Before the migration we kill all non serializable components and store their data in a serializable form
	 */
	public void prepareMigration(){
		serializeGraphTopology();
		closeGui();
		this.g = null;
	}

	/**
	 * Before sending the agent knowledge of the map it should be serialized.
	 */
	private void serializeGraphTopology() {
		this.sg = new SerializableSimpleGraph<String, HashMap<String, Object>>();
		
		Iterator<Node> iter = this.g.iterator();
		while (iter.hasNext()){
			Node n = iter.next();
			
			HashMap<String, Object> map = new HashMap<String, Object>(); // map containing all the attributes of the node
			Object[] attributes = n.attributeKeys().toArray();
			for (Object att : attributes) {
				String key = (String) att;
				map.put(key, n.getAttribute(key));
			}

			sg.addNode(n.getId(), map);
		}
		
		Iterator<Edge> iterE = this.g.edges().iterator();
		while (iterE.hasNext()){
			Edge e = iterE.next();
			Node sn = e.getSourceNode();
			Node tn = e.getTargetNode();
			sg.addEdge(e.getId(), sn.getId(), tn.getId());
		}	
	}

	public synchronized SerializableSimpleGraph<String, HashMap<String, Object>> getSerializableGraph(){
		serializeGraphTopology();
		return this.sg;
	}

	/**
	 * After migration we load the serialized data and recreate the non serializable components (Gui,..)
	 */
	public synchronized void loadSavedData(){

		this.g = new SingleGraph("My world vision");
		this.g.setAttribute("ui.stylesheet", nodeStyle);

		openGui();

		Integer nbEd=0;
		for (SerializableNode<String, HashMap<String, Object>> n: this.sg.getAllNodes()){
			this.g.addNode(n.getNodeId()).setAttribute("ui.class", n.getNodeContent().toString());
			for(String s:this.sg.getEdges(n.getNodeId())){
				this.g.addEdge(nbEd.toString(),n.getNodeId(),s);
				nbEd++;
			}
		}
		System.out.println("Loading done");
	}

	/**
	 * Method called before migration to kill all non serializable graphStream components
	 */
	private synchronized void closeGui() {
		//once the graph is saved, clear non serializable components
		if (this.viewer!=null){
			//Platform.runLater(() -> {
			try{
				this.viewer.close();
			}catch(NullPointerException e){
				System.err.println("Bug graphstream viewer.close() work-around - https://github.com/graphstream/gs-core/issues/150");
			}
			//});
			this.viewer=null;
		}
	}

	/**
	 * Method called after a migration to reopen GUI components
	 */
	private synchronized void openGui() {
		this.viewer = new FxViewer(this.g, FxViewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);//GRAPH_IN_GUI_THREAD)
		viewer.enableAutoLayout();
		viewer.setCloseFramePolicy(FxViewer.CloseFramePolicy.CLOSE_VIEWER);
		viewer.addDefaultView(true);

		g.display();
	}
	/* takes most recent nodes*/
	public void mergeNode(SerializableNode<String, HashMap<String, Object>> n2) {
		
		// n1 : our node
		// n2 : other agent's node
		
		String id = n2.getNodeId();
		HashMap<String, Object> n2Attributes = n2.getNodeContent();
		
		Node n1 = this.g.getNode(id);
		if (n1 == null) {
			n1 = this.g.addNode(id);
		}
		
		Object n1Timestamp = n1.getAttribute("timestamp");
		Object n1StenchTimestamp = n1.getAttribute("stenchTimestamp");
		Object n2Timestamp = n2Attributes.get("timestamp");
		Object n2StenchTimestamp = n2Attributes.get("stenchTimestamp");

		if (n2Timestamp != null && (n1Timestamp == null || (long) n2Timestamp > (long) n1Timestamp)) {
			n1.clearAttributes();
		}
		
		for (String att: n2Attributes.keySet()) {
			
			if (att == "Stench" && (n1StenchTimestamp == null || (long) n2StenchTimestamp > (long) n1StenchTimestamp)) {
				
				n1.setAttribute("Stench", n2Attributes.get("Stench"));
				n1.setAttribute("stenchTimestamp", n2StenchTimestamp);
				
			} else { // att is one of [ui.label, ui.class, Diamond, Gold, LockIsOpen]
				
				if (n2Timestamp != null && (n1Timestamp == null || (long) n2Timestamp > (long) n1Timestamp)) {
					n1.setAttribute(att, n2Attributes.get(att));
				}
			}
		}	
	}
	
	public void mergeMap(SerializableSimpleGraph<String, HashMap<String, Object>> sgreceived) {

		// Nodes
		for (SerializableNode<String, HashMap<String, Object>> n: sgreceived.getAllNodes()){
			mergeNode(n);
		}

		// Edges
		for (SerializableNode<String, HashMap<String, Object>> n: sgreceived.getAllNodes()){
			for (String s: sgreceived.getEdges(n.getNodeId())){
				addEdge(n.getNodeId(), s);
			}
		}
		
		//System.out.println("Merge done");
	}

	/**
	 * 
	 * @return true if there exist at least one openNode on the graph 
	 */
	public boolean hasOpenNode() {
		return (this.g.nodes()
				.filter(n -> n.getAttribute("ui.class") == NodeStatus.open.toString())
				.findAny()).isPresent();
	}




}