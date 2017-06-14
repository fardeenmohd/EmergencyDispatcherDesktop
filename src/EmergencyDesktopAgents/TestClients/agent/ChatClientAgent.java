
package EmergencyDesktopAgents.TestClients.agent;

import EmergencyDesktopAgents.ontology.Ontology;
import EmergencyDesktopAgents.manager.EmergencyManagerAgent;
import EmergencyDesktopAgents.ontology.Joined;
import EmergencyDesktopAgents.ontology.Left;
import jade.content.ContentManager;
import jade.content.Predicate;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;
import jade.util.leap.Iterator;
import jade.util.leap.Set;
import jade.util.leap.SortedSetImpl;
import EmergencyDesktopAgents.TestClients.ChatGui;
/*#MIDP_INCLUDE_BEGIN
import chat.TestClients.MIDPChatGui;
#MIDP_INCLUDE_END*/
//#MIDP_EXCLUDE_BEGIN
import EmergencyDesktopAgents.TestClients.AWTChatGui;
//#MIDP_EXCLUDE_END

import java.util.List;


public class ChatClientAgent extends Agent {
	private static final long serialVersionUID = 1594371294421614291L;

	private Logger logger = Logger.getMyLogger(this.getClass().getName());

	private static final String CHAT_ID = "__chat__";
	private static final String CHAT_MANAGER_NAME = "manager";

	private ChatGui myGui;
	private Set participants = new SortedSetImpl();
	private Codec codec = new SLCodec();
	private jade.content.onto.Ontology onto = Ontology.getInstance();
	private ACLMessage spokenMsg;
	private java.util.HashMap<AID,String> participantAgents = new java.util.HashMap<>();
	private String clientType = EmergencyManagerAgent.POLICE;
	protected void setup() {
		// Register language and ontology
		ContentManager cm = getContentManager();
		cm.registerLanguage(codec);
		cm.registerOntology(onto);
		cm.setValidationMode(false);

		// Add initial behaviours
		addBehaviour(new ParticipantsManager(this));
		addBehaviour(new ChatListener(this));

		// Initialize the message used to convey spoken sentences
		spokenMsg = new ACLMessage(ACLMessage.INFORM);
		spokenMsg.setConversationId(CHAT_ID);

		// Activate the GUI
		//#MIDP_EXCLUDE_BEGIN
		myGui = new AWTChatGui(this);
		//#MIDP_EXCLUDE_END

		/*#MIDP_INCLUDE_BEGIN
		myGui = new MIDPChatGui(this);
		#MIDP_INCLUDE_END*/
	}

	protected void takeDown() {
		if (myGui != null) {
			myGui.dispose();
		}
	}

	private void notifyParticipantsChanged() {
		myGui.notifyParticipantsChanged(getParticipantNames());
	}

	private void notifySpoken(String speaker, String sentence) {
		myGui.notifySpoken(speaker, sentence);
	}
	
	/**
	 * Inner class ParticipantsManager. This behaviour registers as a chat
	 * participant and keeps the list of participants up to date by managing the
	 * information received from the ChatManager agent.
	 */
	class ParticipantsManager extends CyclicBehaviour {
		private static final long serialVersionUID = -4845730529175649756L;
		private MessageTemplate template;

		ParticipantsManager(Agent a) {
			super(a);
		}

		public void onStart() {
			// Subscribe as a chat participant to the ChatManager agent
			ACLMessage subscription = new ACLMessage(ACLMessage.SUBSCRIBE);
			subscription.setLanguage(codec.getName());
			subscription.setOntology(onto.getName());
			subscription.setContent(clientType);
			String convId = "C-" + myAgent.getLocalName();
			subscription.setConversationId(convId);
			subscription
					.addReceiver(new AID(CHAT_MANAGER_NAME, AID.ISLOCALNAME));
			myAgent.send(subscription);
			// Initialize the template used to receive notifications
			// from the EmergencyManagerAgent
			template = MessageTemplate.MatchConversationId(convId);
		}

		public void action() {
			// Receives information about people joining and leaving
			// the chat from the ChatManager agent
			ACLMessage msg = myAgent.receive(template);
			if (msg != null) {
				if (msg.getPerformative() == ACLMessage.INFORM) {
					try {
						Predicate p = (Predicate) myAgent.getContentManager().extractContent(msg);
						if(p instanceof Joined) {
							Joined joined = (Joined) p;
							List<AID> aid = (List<AID>) joined.getWho();
							for(AID a : aid)
							{
								String[] codedMsg = a.getName().split("_");
								String agentName = codedMsg[0];
								String agentType = codedMsg[1];
								logger.log(Logger.INFO,agentName + agentType);
								a.setName(agentName);
								participantAgents.put(a,agentType);
								participants.add(a);
							}

							notifyParticipantsChanged();
						}
						if(p instanceof Left) {
							Left left = (Left) p;
							List<AID> aid = (List<AID>) left.getWho();
							for(AID a : aid)
							{
								participants.remove(a);
								participantAgents.remove(a);
							}

							notifyParticipantsChanged();
						}
					} catch (Exception e) {
						Logger.println(e.toString());
						e.printStackTrace();
					}
				} else {
					handleUnexpected(msg);
				}
			} else {
				block();
			}
		}
	} // END of inner class ParticipantsManager

	/**
	 * Inner class ChatListener. This behaviour registers as a chat participant
	 * and keeps the list of participants up to date by managing the information
	 * received from the ChatManager agent.
	 */
	class ChatListener extends CyclicBehaviour {
		private static final long serialVersionUID = 741233963737842521L;
		private MessageTemplate template = MessageTemplate
				.MatchConversationId(CHAT_ID);

		ChatListener(Agent a) {
			super(a);
		}

		public void action() {
			ACLMessage msg = myAgent.receive(template);
			if (msg != null) {
				System.out.println("Got message from: " + msg.getSender() + " with content: " + msg.getContent());
					notifySpoken(msg.getSender().getLocalName(),
							msg.getContent());
			}
			else {
				block();
			}
		}
	} // END of inner class ChatListener

	/**
	 * Inner class ChatSpeaker. INFORMs other participants about a spoken
	 * sentence
	 */
	private class ChatSpeaker extends OneShotBehaviour {
		private static final long serialVersionUID = -1426033904935339194L;
		private String sentence;

		private ChatSpeaker(Agent a, String s) {
			super(a);
			sentence = s;
		}

		public void action() {
			spokenMsg.clearAllReceiver();
			Iterator it = participants.iterator();
			while (it.hasNext()) {
				spokenMsg.addReceiver((AID) it.next());
			}
			spokenMsg.setContent(sentence);
			notifySpoken(myAgent.getLocalName(), sentence);
			if(sentence.contains("REQUESTING")){
				//for testing purposes
				spokenMsg.setPerformative(ACLMessage.REQUEST);
			}
			else if(sentence.contains("Helpaccepted")){
				spokenMsg.setPerformative(ACLMessage.AGREE);
			}
			send(spokenMsg);
			spokenMsg.setPerformative(ACLMessage.INFORM);
		}
	} // END of inner class ChatSpeaker

	// ///////////////////////////////////////
	// Methods called by the interface
	// ///////////////////////////////////////
	public void handleSpoken(String s) {
		// Add a ChatSpeaker behaviour that INFORMs all participants about
		// the spoken sentence
		addBehaviour(new ChatSpeaker(this, s));
	}
	
	public String[] getParticipantNames() {
		String[] pp = new String[participants.size()];
		Iterator it = participants.iterator();
		int i = 0;
		while (it.hasNext()) {
			AID id = (AID) it.next();
			pp[i++] = id.getLocalName() + "_" + participantAgents.get(id);
		}
		return pp;
	}

	// ///////////////////////////////////////
	// Private utility method
	// ///////////////////////////////////////
	private void handleUnexpected(ACLMessage msg) {
		if (logger.isLoggable(Logger.WARNING)) {
			logger.log(Logger.WARNING, "Unexpected message received from "
					+ msg.getSender().getName());
			logger.log(Logger.WARNING, "Content is: " + msg.getContent());
		}
	}

}
