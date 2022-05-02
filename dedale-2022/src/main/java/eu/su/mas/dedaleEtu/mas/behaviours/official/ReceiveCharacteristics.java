package eu.su.mas.dedaleEtu.mas.behaviours.official;

import java.util.HashMap;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.official.ExploreDFSAgent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class ReceiveCharacteristics extends SimpleBehaviour {

    private static final long serialVersionUID = -1122887021789014975L;

    private int infoReceived;

    public ReceiveCharacteristics(ExploreDFSAgent agent) {
        super(agent);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void action() {

        // The agent checks if he received characteristics from a teammate.

        MessageTemplate msgTemplate = MessageTemplate.and(
                MessageTemplate.MatchProtocol("SHARE-TOPO"),
                MessageTemplate.MatchPerformative(ACLMessage.AGREE));
        ACLMessage infoMsg = this.myAgent.receive(msgTemplate);

        if (infoMsg != null) {
            this.infoReceived = 1;

            ((ExploreDFSAgent) this.myAgent).updateKnownCharacteristics(infoMsg.getSender().getLocalName(), infoMsg.getContent());

            // TODO Zoe : again, like in shareCharacteristics, here im not sure if i should just call for a Pong behaviour?

            ACLMessage mapReceivedAck = infoMsg.createReply();
            mapReceivedAck.setSender(this.myAgent.getAID());
            mapReceivedAck.setPerformative(ACLMessage.CONFIRM);

            //msg.setContent("1");
            byte[] b = {1};
            mapReceivedAck.setByteSequenceContent(b);

            ((AbstractDedaleAgent)this.myAgent).sendMessage(mapReceivedAck);

        } else {
            this.infoReceived = 0;
        }
    }

    @Override
    public boolean done() {
        if (this.infoReceived == 1) { // Kiara: idk if this is necessary, have to test how it works with FSM
            return true;
        }
        return false;
    }

    @Override
    public int onEnd() {
//		reset();
//		((FSMBehaviour) getParent()).registerState();
        return infoReceived;
    }
}
