package eu.su.mas.dedaleEtu.mas.behaviours.official;

import eu.su.mas.dedaleEtu.mas.agents.official.ExploreDFSAgent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class ReceiveCharacteristics extends SimpleBehaviour {

    private static final long serialVersionUID = -1122887021789014975L;

    private ExploreDFSAgent myAgent;
    private int infoReceived;
    private long timeoutDate;
    private boolean timedOut;
    private boolean toReinitialise = true;

    public ReceiveCharacteristics(ExploreDFSAgent agent) {
        super(agent);
        this.myAgent = agent;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void action() {

    	if (this.toReinitialise == true) {
    		this.timeoutDate = System.currentTimeMillis() + 500;
    		this.infoReceived = 0;
    		this.timedOut = false;
            System.out.println("[ReceiveCharacteristicsBehaviour] "+this.myAgent.getLocalName()+" is waiting for agent characteristics.");
            this.toReinitialise = false;
    	}
    	
        // The agent checks if he received characteristics from a teammate.

        MessageTemplate msgTemplate = MessageTemplate.and(
                MessageTemplate.MatchProtocol("SHARE-TOPO"),
                MessageTemplate.MatchPerformative(ACLMessage.AGREE));
        ACLMessage infoMsg = this.myAgent.receive(msgTemplate);

        if (infoMsg != null) {
        	System.out.println("[ReceiveCharacteristicsBehaviour] "+this.myAgent.getLocalName()+" has received"+infoMsg.getSender().getLocalName()+"'s characteristics");
            this.infoReceived = 1;
            this.toReinitialise = true;

            this.myAgent.updateKnownCharacteristics(infoMsg.getSender().getLocalName(), infoMsg.getContent());

            // myAgent now sends a pong as an acknowledgment, and we enter the normal share map protocol
            ACLMessage pong = infoMsg.createReply();
            pong.setSender(this.myAgent.getAID());
            pong.setPerformative(ACLMessage.ACCEPT_PROPOSAL);

            //msg.setContent("1");
            byte[] b = {1};
            pong.setByteSequenceContent(b);

            this.myAgent.sendMessage(pong);

        } else {
            this.infoReceived = 0;

            if (System.currentTimeMillis() > this.timeoutDate) {
            	this.timedOut = true;
                this.toReinitialise = true;
            }
        }
    }

    @Override
    public boolean done() {
        if (this.infoReceived == 1 || this.timedOut) { // Kiara: idk if this is necessary, have to test how it works with FSM
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
