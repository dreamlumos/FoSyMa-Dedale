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
    private boolean timedOut;
    private long timeoutDate;
    private int res = 0; // 0: timeout, 1: pong, 2: unknown
    private boolean toReinitialise = true;

    public CheckForPongUnknown(Agent a) {
        super(a);
    }

    @Override
    public void action() {
    	if (this.toReinitialise == true) {
    		this.timeoutDate = System.currentTimeMillis() + 500;
    		this.timedOut = false;
    		this.pongReceived = false;
    		this.unknownReceived = false;
            System.out.println("Agent "+this.myAgent.getLocalName()+" is checking for pong or unknown.");
            this.toReinitialise = false;
    	}
    	
        // The agent checks if he received a pong from a teammate.
        MessageTemplate msgTemplate = MessageTemplate.and(
                MessageTemplate.MatchProtocol("SHARE-TOPO"),
                MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL));
        ACLMessage pong = this.myAgent.receive(msgTemplate);

        if (pong != null) {
//        	System.out.println("test1");

            this.pongReceived = true;
            res = 1;
            this.toReinitialise = true;

            ((ExploreDFSAgent)this.myAgent).setCurrentPong(pong);
            System.out.println(this.myAgent.getLocalName()+" received a pong from "+pong.getSender().getLocalName());
            return;

        } else {
//        	System.out.println("test2");

            this.pongReceived = false;
        }

        // The agent checks if he received an "i don't know you" msge from a teammate.
        MessageTemplate msgTemplateUnknown = MessageTemplate.and(
                MessageTemplate.MatchProtocol("SHARE-TOPO"),
                MessageTemplate.MatchPerformative(ACLMessage.UNKNOWN));
        ACLMessage unknown = this.myAgent.receive(msgTemplateUnknown);

        if (unknown != null) {
//        	System.out.println("test3");

            this.unknownReceived = true;
            res = 2;
            this.toReinitialise = true;

            ((ExploreDFSAgent)this.myAgent).setCurrentPong(unknown);
            System.out.println(this.myAgent.getLocalName()+" received an 'unknown' message from "+unknown.getSender().getLocalName());

        } else {
//        	System.out.println("test4");

            this.unknownReceived = false;
            if (System.currentTimeMillis() > this.timeoutDate) {
//            	System.out.println("test5");
            	
            	this.res = 0;
            	this.timedOut = true;
            	this.toReinitialise = true;
            } else {
//            	System.out.println("test6");
            	this.timedOut = false;
            }
        }
    }
    
    @Override
    public boolean done() {
        return pongReceived || unknownReceived || timedOut;
    }

    @Override
    public int onEnd() {
    	// reset(); 
        return res;
    }
}
