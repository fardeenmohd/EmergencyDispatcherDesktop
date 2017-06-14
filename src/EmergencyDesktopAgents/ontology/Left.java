package EmergencyDesktopAgents.ontology;

import java.util.List;

import jade.content.Predicate;
import jade.core.AID;

//#J2ME_EXCLUDE_FILE

@SuppressWarnings("serial")
public class Left implements Predicate {

	private List<AID> _who;

	public void setWho(List<AID> who) {
		_who = who;
	}

	public List<AID> getWho() {
		return _who;
	}

}