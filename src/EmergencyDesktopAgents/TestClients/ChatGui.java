
package EmergencyDesktopAgents.TestClients;


public interface ChatGui {
	void notifyParticipantsChanged(String[] names);
	void notifySpoken(String speaker, String sentence);
	void dispose();
}