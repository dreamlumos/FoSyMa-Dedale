package eu.su.mas.dedaleEtu.mas.behaviours.official;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
        import java.util.HashMap;
        import java.util.List;

        import dataStructures.serializableGraph.SerializableSimpleGraph;
        import eu.su.mas.dedale.mas.AbstractDedaleAgent;
        import eu.su.mas.dedaleEtu.mas.agents.official.ExploreDFSAgent;
        import eu.su.mas.dedaleEtu.mas.knowledge.FullMapRepresentation;
        import jade.core.AID;
        import jade.core.Agent;
        import jade.core.behaviours.SimpleBehaviour;
        import jade.lang.acl.ACLMessage;
        import jade.lang.acl.MessageTemplate;
import javafx.util.Pair;

public class DistributedAlgorithm extends SimpleBehaviour {

    private static final long serialVersionUID = -5609039653282110622L;

    private int phase; //Phase 0: sending map, Phase 1: waiting for ack
    private ACLMessage pong;
    private boolean ackReceivedOrTimedOut;
    private long timeoutDate;
    private ArrayList<String> agentsCoalition;
    private int coalSize; // the size of the coalition that myAgent needs to combinate

    public DistributedAlgorithm(Agent agent, ArrayList<String>agentsCoal) {
        super(agent);
//        this.pong = pong;
        this.phase = 0;
        this.agentsCoalition = agentsCoal;
        this.coalSize = (int)(Math.random() * (this.agentsCoalition.size())); //Zoe : how to make sure they don't overlap...
    }

    @Override
    public void action() {

        System.out.println("Coalition!");
        ACLMessage bestCoalition = new ACLMessage(ACLMessage.INFORM);
        bestCoalition.setProtocol("SHARE-COALITION");
        bestCoalition.setSender(this.myAgent.getAID());

        ArrayList<String> receivers = agentsCoalition;
        for (String receiverName: receivers) {
            bestCoalition.addReceiver(new AID(receiverName, false));
        }

        HashMap<String, Integer> gold = ((ExploreDFSAgent)this.myAgent).getGoldDict();
//        ArrayList<Integer> gold = new ArrayList<>(); // temp Zoe : implement a gold and diamond list
        // Zoe : like HashMap<Integer, *pointer to the spot/nodeId> ?

        Pair<String, Integer> bestCoal = ((ExploreDFSAgent)this.myAgent).calculateCoalition(coalSize, agentsCoalition, gold);

        String sg = ((ExploreDFSAgent)this.myAgent).pairToString(bestCoal);

        bestCoalition.setContent(sg);

        ((AbstractDedaleAgent)this.myAgent).sendMessage(bestCoalition);
        this.timeoutDate = System.currentTimeMillis() + 1000; // 1s timeout

    }

    @Override
    public boolean done() {
        return false;
    }

}
