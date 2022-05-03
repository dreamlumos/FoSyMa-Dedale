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

            // myAgent now sends a pong as an acknowledgment, and we enter the normal share map protocol
            ACLMessage pong = infoMsg.createReply();
            pong.setSender(this.myAgent.getAID());
            pong.setPerformative(ACLMessage.ACCEPT_PROPOSAL);

            //msg.setContent("1");
            byte[] b = {1};
            pong.setByteSequenceContent(b);

            ((AbstractDedaleAgent)this.myAgent).sendMessage(pong);

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
