package chauvu_CSCI201_FinalProject;


public class BattleshipUser {
	private String username, password;
	private int numGamesPlayed, totalWins, totalLosses;
	public BattleshipUser(String username, String password, int numGamesPlayed, int totalWins, int totalLosses) {
		this.username = username;
		this.password = password;
		this.numGamesPlayed = numGamesPlayed;
		this.totalWins = totalWins;
		this.totalLosses = totalLosses;
	}
	public boolean isUsername(String attempt) {
		if (attempt.equals(this.username)) {
			return true;
		}
		else {
			return false;
		}
	}
	public boolean isPassword(String attempt) {
		if (attempt.equals(this.password)) {
			return true;
		}
		else {
			return false;
		}
	}
	public int getNumGamesPlayed() {
		return this.numGamesPlayed;
	}
	public int getTotalWins() {
		return this.totalWins;
	}
	public int getTotalLosses() {
		return this.totalLosses;
	}
	// find some way to update score in database
	
	/*	TO BE DELETED
	 *	
	 */
	public String getUsername() {
		return this.username;
	}
	public String getPassword() {
		return this.password;
	}
	
}

