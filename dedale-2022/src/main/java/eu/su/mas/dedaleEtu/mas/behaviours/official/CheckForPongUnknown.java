package eu.su.mas.dedaleEtu.mas.behaviours.official;

import eu.su.mas.dedaleEtu.mas.agents.official.ExploreDFSAgent;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class CheckForPongUnknown extends SimpleBehaviour {

    private static final long serialVersionUID = 560497589376177200L;

    private boolean pongReceived;
    private boolean unknownReceived;
    private int res = 0;

    public CheckForPongUnknown(Agent a) {
        super(a);
    }


    @Override
    public void action() {
        System.out.println("Agent "+this.myAgent.getLocalName()+" is checking for pong or unknown.");

        // The agent checks if he received a pong from a teammate.
        MessageTemplate msgTemplate = MessageTemplate.and(
                MessageTemplate.MatchProtocol("SHARE-TOPO"),
                MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL));
        ACLMessage pong = this.myAgent.receive(msgTemplate);

        if (pong != null) {
            this.pongReceived = true;
            res = 1;

            // Sharing of the map in a separate behaviour which is added to the pool
//            this.myAgent.addBehaviour(new SharePartialMapBehaviour(this.myAgent, pong));
            ((ExploreDFSAgent)this.myAgent).setCurrentPong(pong);

        } else {
            this.pongReceived = false;
        }

        // The agent checks if he received an "i don't know you" msge from a teammate.
        MessageTemplate msgTemplateUnknown = MessageTemplate.and(
                MessageTemplate.MatchProtocol("SHARE-TOPO"),
                MessageTemplate.MatchPerformative(ACLMessage.UNKNOWN));
        ACLMessage unknown = this.myAgent.receive(msgTemplateUnknown);

        if (unknown != null) {
            this.unknownReceived = true;
            res = 2;

            // Sharing of the characteristics in a separate behaviour which is added to the pool
            ((ExploreDFSAgent)this.myAgent).setCurrentPong(unknown);

//            this.myAgent.addBehaviour(new ShareCharacteristics(this.myAgent, unknown));
//            String s = ((ExploreDFSAgent)(this.myAgent)).getListBehavTemp();
//            System.out.println(s);
        } else {
            this.unknownReceived = false;
        }

    }
    @Override
    public boolean done() {
        return pongReceived || unknownReceived;
    }

    @Override
    public int onEnd() {
        return res; //return 1;
    }
}
