package eu.su.mas.dedaleEtu.mas.behaviours.official;

import java.util.HashMap;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.official.ExploreDFSAgent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class EstablishEquityBehaviour extends SimpleBehaviour {
    // if here then myAgent has the whole map explored OR the time is up
    private int mapReceived;

    public EstablishEquityBehaviour(ExploreDFSAgent agent) {
        super(agent);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void action() {
        MessageTemplate msgTemplate = MessageTemplate.and(
                MessageTemplate.MatchProtocol("SHARE-TOPO"),
                MessageTemplate.MatchPerformative(ACLMessage.INFORM));
        ACLMessage mapMsg = this.myAgent.receive(msgTemplate);

        if (mapMsg != null) {
            this.mapReceived = 1;

            SerializableSimpleGraph<String, HashMap<String, Object>> sgreceived = null;
            try {
                sgreceived = (SerializableSimpleGraph<String, HashMap<String, Object>>) mapMsg.getContentObject();
            } catch (UnreadableException e) {
                e.printStackTrace();
            }
            ((ExploreDFSAgent) this.myAgent).getMap().mergeMap(sgreceived);

            ACLMessage mapReceivedAck = mapMsg.createReply();
            mapReceivedAck.setSender(this.myAgent.getAID());
            mapReceivedAck.setPerformative(ACLMessage.CONFIRM);

            //msg.setContent("1");
            byte[] b = {1};
            mapReceivedAck.setByteSequenceContent(b);

            ((AbstractDedaleAgent)this.myAgent).sendMessage(mapReceivedAck);

        } else {
            this.mapReceived = 0;
        }
    }

    @Override
    public boolean done() {
        if (this.mapReceived == 1) { // Kiara: idk if this is necessary, have to test how it works with FSM
            return true;
        }
        return false;
    }

    @Override
    public int onEnd() {
//		reset();
//		((FSMBehaviour) getParent()).registerState();
        return mapReceived;
    }
}
