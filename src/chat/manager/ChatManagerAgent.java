/*****************************************************************
JADE - Java Agent DEvelopment Framework is a framework to develop 
multi-agent systems in compliance with the FIPA specifications.
Copyright (C) 2000 CSELT S.p.A. 

GNU Lesser General Public License

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation, 
version 2.1 of the License. 

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the
Free Software Foundation, Inc., 59 Temple Place - Suite 330,
Boston, MA  02111-1307, USA.
 *****************************************************************/

package chat.manager;

//#J2ME_EXCLUDE_FILE

import jade.core.Agent;
import jade.core.AID;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.BasicOntology;
import jade.content.abs.*;

import jade.proto.SubscriptionResponder;
import jade.proto.SubscriptionResponder.SubscriptionManager;
import jade.proto.SubscriptionResponder.Subscription;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.FailureException;

import jade.domain.introspection.IntrospectionOntology;
import jade.domain.introspection.Event;
import jade.domain.introspection.DeadAgent;
import jade.domain.introspection.AMSSubscriber;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import chat.ontology.*;


public class ChatManagerAgent extends Agent implements SubscriptionManager {
	private Map<AID, Subscription> participants = new HashMap<AID, Subscription>();
	private Map<AID, String> participatingAgents = new HashMap<>();
	public static final String USER = "USER";
	public static final String POLICE = "POLICE";
	//public static final String FIRE_DEPARTMENT = "FIRE";
	private Codec codec = new SLCodec();
	private Ontology onto = ChatOntology.getInstance();
	private AMSSubscriber myAMSSubscriber;
	// These strings below will define the types of agents that can connect to our system
	protected void setup() {
		// Prepare to accept subscriptions from chat participants
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(onto);

		MessageTemplate sTemplate = MessageTemplate.and(
				MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE),
				MessageTemplate.and(
						MessageTemplate.MatchLanguage(codec.getName()),
						MessageTemplate.MatchOntology(onto.getName()) ) );
		addBehaviour(new SubscriptionResponder(this, sTemplate, this));

		// Register to the AMS to detect when chat participants suddenly die
		myAMSSubscriber = new AMSSubscriber() {
			protected void installHandlers(Map handlersTable) {
				// Fill the event handler table. We are only interested in the
				// DEADAGENT event
				handlersTable.put(IntrospectionOntology.DEADAGENT, new EventHandler() {
					public void handle(Event ev) {
						DeadAgent da = (DeadAgent)ev;
						AID id = da.getAgent();
						// If the agent was attending the chat --> notify all
						// other participants that it has just left.
						if (participants.containsKey(id)) {
							try {
								deregister((Subscription) participants.get(id));
							}
							catch (Exception e) {
								//Should never happen
								e.printStackTrace();
							}
						}
					}
				});
			}
		};
		addBehaviour(myAMSSubscriber);
	}

	protected void takeDown() {
		// Unsubscribe from the AMS
		send(myAMSSubscriber.getCancel());
		//FIXME: should inform current participants if any
	}

	///////////////////////////////////////////////
	// SubscriptionManager interface implementation
	///////////////////////////////////////////////
	public boolean register(Subscription s) throws RefuseException, NotUnderstoodException {
		String agentType = "";
		String agentName = "";
		try {
			AID newId = s.getMessage().getSender();
			agentType = s.getMessage().getContent();
			// Notify the new participant about the others (if any) and VV
			if (!participants.isEmpty()) {
				// The message for the new participant
				ACLMessage notif1 = s.getMessage().createReply();
				notif1.setPerformative(ACLMessage.INFORM);

				// The message for the old participants.
				// NOTE that the message is the same for all receivers (a part from the
				// conversation-id that will be automatically adjusted by Subscription.notify())
				// --> Prepare it only once outside the loop
				ACLMessage notif2 = (ACLMessage) notif1.clone();
				notif2.clearAllReceiver();
				Joined whoHasJustJoined = new Joined();
				Joined whoHasAlreadyJoined = new Joined();
				List<AID> whoHasJustConnected = new ArrayList<AID>(1);
				List<AID> whoHasAlreadyConnected = new ArrayList<AID>(1);
				agentName = newId.getName();
				newId.setName(newId.getName() + "_" + agentType);
				whoHasJustConnected.add(newId);
				whoHasJustJoined.setWho(whoHasJustConnected);
				getContentManager().fillContent(notif2, whoHasJustJoined);

				//whoHasJustConnected.clear();
				Iterator<AID> it = participants.keySet().iterator();
				while (it.hasNext()) {
					AID oldId = it.next();
					AID copyOfOldId = new AID();
					// Notify old participant
					Subscription oldS = (Subscription) participants.get(oldId);
					String oldAgentType = participatingAgents.get(oldId);
					String oldAgentName = oldId.getName();
					copyOfOldId.setName(oldAgentName + "_" + oldAgentType);
					System.out.println("Notifying old client with message: " + notif2.getContent());
					oldS.notify(notif2);
					whoHasAlreadyConnected.add(copyOfOldId);
				}
				whoHasAlreadyJoined.setWho(whoHasAlreadyConnected);
				// Notify new participant
				getContentManager().fillContent(notif1, whoHasAlreadyJoined);
				System.out.println("Notifying new client with message: " + notif1.getContent());
				s.notify(notif1);
				newId.setName(agentName);
			}
			
			// Add the new subscription
			participants.put(newId, s);
			participatingAgents.put(newId, agentType);
			System.out.println("New agent with name " + newId.getName() + " and type " + agentType + " has subscribed");
			return false;
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new RefuseException("Subscription error");
		}		
	}

	public boolean deregister(Subscription s) throws FailureException {
		AID oldId = s.getMessage().getSender();
		// Remove the subscription
		if (participants.remove(oldId) != null) {
			// Notify other participants if any
			if (!participants.isEmpty()) {
				try {
					ACLMessage notif = s.getMessage().createReply();
					notif.setPerformative(ACLMessage.INFORM);
					notif.clearAllReceiver();
					AbsPredicate p = new AbsPredicate(ChatOntology.LEFT);
					AbsAggregate agg = new AbsAggregate(BasicOntology.SEQUENCE);
					agg.add((AbsTerm) BasicOntology.getInstance().fromObject(oldId));
					p.set(ChatOntology.LEFT_WHO, agg);
					getContentManager().fillContent(notif, p);

					Iterator it = participants.values().iterator();
					while (it.hasNext()) {
						Subscription s1 = (Subscription) it.next();
						s1.notify(notif);
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return false;
	}
}
