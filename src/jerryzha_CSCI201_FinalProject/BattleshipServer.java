package chauvu_CSCI201_FinalProject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Vector;



public class BattleshipServer extends Thread{
	//data
	private Random rand = new Random();
	private boolean consecutive = false;
	private int consecutiveCount = 0;
	
	//each battleship thread
	private Map<BattleshipThread, String> bt2string = Collections.synchronizedMap(new HashMap<BattleshipThread, String>());
	private Map<String, BattleshipThread> string2bt = Collections.synchronizedMap(new HashMap<String, BattleshipThread>());
	
	//each group of game
	private List<String> groupNameList = Collections.synchronizedList(new ArrayList<String>());
	private Map<String, ArrayList<BattleshipThread>> gameGroupMap = Collections.synchronizedMap(new HashMap<String, ArrayList<BattleshipThread>>());
	private Map<String, Integer> currentAttackerMap = Collections.synchronizedMap(new HashMap<String, Integer>()); 
	private Map<String, Integer> numPlayersMap = Collections.synchronizedMap(new HashMap<String, Integer>());
	private Map<String, Integer> numCellsMap = Collections.synchronizedMap(new HashMap<String, Integer>());
	private Map<String, Integer> groupAvailableMap = Collections.synchronizedMap(new HashMap<String, Integer>()); //1: available, 0: full
	private Map<String, Integer> groupLevelMap = Collections.synchronizedMap(new HashMap<String, Integer>());
	
	public BattleshipServer(int port){
		this.port = port;
		startDatabase();
		start();
		startChatConnection(6789);
	}
	private int port;
	private Vector<Socket> chatSocket;
	private static final String DB_ADDRESS = "jdbc:mysql://localhost/";
	private static final String DB_NAME = "csci201fp_battleship";
	private static final String DRIVER = "com.mysql.jdbc.Driver";
	private static final String USER = "root";
	private static final String PASSWORD = "";
	private Connection dbConnection;
	private Statement dbStatement;
	private ResultSet dbResultSet;
	private Vector<BattleshipUser> battleshipDatabase;
	private int numClients;
	private String [] usernames;
	// Chat Info
	private Vector<ChatThread> ctVector;
	
	private void startDatabase() {
		// initialization
		numClients = 0;
		battleshipDatabase = new Vector<BattleshipUser>();
		ctVector = new Vector<ChatThread>();
		// setup database
		try {
			Class.forName(DRIVER);
			this.dbConnection = DriverManager.getConnection(DB_ADDRESS + DB_NAME, USER, PASSWORD);
			this.dbStatement = dbConnection.createStatement();
			this.dbResultSet = dbStatement.executeQuery("SELECT * FROM BattleshipLoginData");
			this.getUserData();
			//this.printDatabase();
		} catch (ClassNotFoundException cnfe) {
			cnfe.getMessage();
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}
	}
	private void getUserData() throws SQLException {
		while (this.dbResultSet.next()) {
			BattleshipUser bsUser = new BattleshipUser(this.dbResultSet.getString("username"),
													this.dbResultSet.getString("password"),
													this.dbResultSet.getInt("NumGamesPlayed"),
													this.dbResultSet.getInt("TotalWins"),
													this.dbResultSet.getInt("TotalLosses"));
			this.battleshipDatabase.add(bsUser);
		}
	}
	public void printDatabase() {
		for (BattleshipUser bsUser : battleshipDatabase) {
			System.out.println(bsUser.getUsername() + ","
							+ bsUser.getPassword() + ","
							+ bsUser.getNumGamesPlayed() + ","
							+ bsUser.getTotalWins() + ","
							+ bsUser.getTotalLosses());
		}
	}
	public void createNewUser(String username, String password) throws SQLException {
		String query = "INSERT INTO BattleshipLoginData(Username, Password, NumGamesPlayed, TotalWins, TotalLosses) VALUES (?,?,?,?,?)";
		PreparedStatement add2DBStmt = this.dbConnection.prepareStatement(query);
		add2DBStmt.setString(1, username);
		add2DBStmt.setString(2, password);
		add2DBStmt.setInt(3, 0);
		add2DBStmt.setInt(4, 0);
		add2DBStmt.setInt(5, 0);
		add2DBStmt.execute();
		
		// program data portion
		BattleshipUser bsUser = new BattleshipUser(username, password, 0, 0, 0);
		battleshipDatabase.add(bsUser);
	}
	public void deleteUser(String username) throws SQLException {
		String query = "DELETE FROM BattleshipLoginData WHERE Username = ?";
		PreparedStatement removeFromDBStmt = this.dbConnection.prepareStatement(query);
		removeFromDBStmt.setString(1, username);
		removeFromDBStmt.execute();
		for (int i=0; i < battleshipDatabase.size(); i++) {
			if (battleshipDatabase.elementAt(i).getUsername().equals(username)) {
				battleshipDatabase.removeElementAt(i);
				break;
			}
		}
	}
	public boolean usernameValid(String username) {
		boolean numbersOnlyFlag = true;
		for (int i=0; i < username.length(); i++) {
			if (!Character.isDigit(username.charAt(i))) {
				numbersOnlyFlag = false;
				break;
			}
		}
		if (numbersOnlyFlag) {
			return false;
		}
		else {
			return true;
		}
	}
	public boolean usernameAlreadyExists(String username) {
		for (BattleshipUser bsUser : battleshipDatabase) {
			if (bsUser.isUsername(username)) {
				return true;
			}
		}
		return false;
	}
	public boolean loginAttempt(String username, String password) {
		for (BattleshipUser bsUser : battleshipDatabase) {
			if (bsUser.isUsername(username)) {
				if (bsUser.isPassword(password)) {
					return true;
				}
				else {
					return false;
				}
			}
		}
		return false;
	}
	
	private void startChatConnection(int port) {
		try {
			ServerSocket ss = new ServerSocket(port);
			while (true) {
				Socket s = ss.accept();
				ChatThread ct = new ChatThread(s, this);
				ctVector.add(ct);
				ct.start();
			}
		} catch (IOException ioe) {
			System.out.println("IOException: " + ioe.getMessage());
		}
	}
	public void sendMessage(String message, ChatThread ct) {
		for (ChatThread c : ctVector) {
			if (!c.equals(ct)) {
				c.send(message);
			}
		}
	}
	public void removeChatThread(ChatThread ct) {
		ctVector.remove(ct);
	}
	class ChatThread extends Thread {
		private Socket socket;
		private BattleshipServer server;
		private BufferedReader br;
		private PrintWriter pw;

		public ChatThread(Socket socket, BattleshipServer server) {
			this.socket = socket;
			this.server = server;
			try {
				this.pw = new PrintWriter(socket.getOutputStream());
				this.br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			} catch (IOException ioe) {
				System.out.println("IOException in ChatThread: " + ioe.getMessage());
			}
		}
		public void send(String message) {
			pw.println(message);
			pw.flush();
		}
		public void run() {
			try {
				while (true) {
					String line = br.readLine();
					server.sendMessage(line, this);
				}
			} catch (IOException ioe) {
				server.removeChatThread(this);
				System.out.println("Client disconnected from " + socket.getInetAddress());
			}
		}
	}

	public void run(){
		try{
			ServerSocket ss = new ServerSocket(port);
			while(true){ //allow multiple clients to connect to this server
System.out.println("Waiting...");
				Socket s = ss.accept();
System.out.println("Accepted.");
				BattleshipThread bt = new BattleshipThread(s, this);
				bt.start();
				if(gameGroupMap.get(getGroupName(bt.getGroup())).size() == numPlayersMap.get(getGroupName(bt.getGroup()))){ //if all players have joined group
					sendInitialInfo(bt.getGroup());
					sendMode(bt.getGroup(), 0);
					groupAvailableMap.put(getGroupName(bt.getGroup()), 0);
				}
			}
		}
		catch(IOException ioe){
			System.out.println("ioe: "+ioe.getMessage());
		}
	}
	
	public void addNewGroup(BattleshipThread bt, String username, int num_player, int num_ai, int num_cell, int level){
		if(gameGroupMap.containsKey(username)){
			System.out.println("Group Name taken: "+username); //MORE PROCESSING
		}
		else{
			bt2string.put(bt, username);
			string2bt.put(username, bt);
			groupNameList.add(username);
			numPlayersMap.put(username, num_player);
			ArrayList<BattleshipThread> btarray = new ArrayList<BattleshipThread>();
			btarray.add(bt);
			gameGroupMap.put(username, btarray);
			groupAvailableMap.put(username, 1);
			numCellsMap.put(username, num_cell);
			groupLevelMap.put(username, level);
			for(int i=0; i<num_ai; i++){
				int r = rand.nextInt(5);
				if(r==0){
					new AILevel1(num_cell, username, level);
				}
				else if(r==1){
					new AILevel2(num_cell, username, level);
				}
				else if(r==2){
					new AILevel3(num_cell, username, level);
				}
				else if(r==3){
					new AILevel4(num_cell, username, level);
				}
				else if(r==4){
					new AILevel4(num_cell, username, level);
				}
			}
		}
	}
	
	public void joinExistingGroup(BattleshipThread bt, String group, String username){
		bt2string.put(bt, username);
		string2bt.put(username, bt);
		gameGroupMap.get(getGroupName(group)).add(bt);
	}
	
	public void joinExistingGroupAI(BattleshipThread bt, String group, String aiID){
		bt2string.put(bt, aiID);
		string2bt.put(aiID, bt);
		gameGroupMap.get(getGroupName(group)).add(bt);
	}
	
	public void drawDone(BattleshipThread bt){
		ArrayList<BattleshipThread> btList = gameGroupMap.get(getGroupName(bt.getGroup()));
		for(int i=0; i<btList.size(); i++){
			if(!btList.get(i).getDoneDraw()) return;
		}
		for(int i=0; i<btList.size(); i++){
			btList.get(i).drawDone();
		}
	}
	
	public void sendInitialInfo(String group){
		ArrayList<BattleshipThread> btList = gameGroupMap.get(getGroupName(group));
		for(int i=0; i<btList.size(); i++){
			btList.get(i).sendMyID(btList.get(i).getUsername(), btList.size());
			for(int j=0; j<btList.size(); j++){
				if(i!=j) btList.get(i).sendOppID(btList.get(j).getUsername());
			}
		}
	}
	
	public void getNextAttacker(String group){
		boolean flag = false;
		int currentAttacker = currentAttackerMap.get(getGroupName(group));
		ArrayList<BattleshipThread> btList = gameGroupMap.get(getGroupName(group));
		while(!flag){
			currentAttacker++;
			if(currentAttacker==btList.size()) currentAttacker = 0;
			//check if dead
			if(btList.get(currentAttacker).getStatus()==1) flag = true;
		}
		sendMode(group, currentAttacker);
	}
	
	public void sendMode(String group, int attacker){
		currentAttackerMap.put(getGroupName(group), attacker);
		ArrayList<BattleshipThread> btList = gameGroupMap.get(getGroupName(group));
		btList.get(attacker).sendMode("attack");
		for(int i=0; i<btList.size(); i++){
			if(i==attacker) continue;
			btList.get(i).sendMode("defense");
		}
	}
	
	public synchronized void sendAttack(String group, int hitX, int hitY, String attacker, String victim){
		//System.out.println("server send attack: "+hitX+" "+hitY+" "+attacker+" "+victim);
		ArrayList<BattleshipThread> btList = gameGroupMap.get(getGroupName(group));
		string2bt.get(victim).sendAttack(hitX, hitY, attacker);
		for(int i=0; i<btList.size(); i++){
			if(btList.get(i).getUsername().equals(attacker) || btList.get(i).getUsername().equals(victim)) continue;
			btList.get(i).sendNothing();
		}
		if(consecutive && consecutiveCount<1){
			consecutive = false;
		}
		if(consecutiveCount==1) consecutiveCount = 0;
	}
	
	public void sendNothing(String group, String attacker){
		ArrayList<BattleshipThread> btList = gameGroupMap.get(getGroupName(group));
		for(int i=0; i<btList.size(); i++){
			btList.get(i).sendNothing();
		}
		getNextAttacker(group);
	}
	
	public synchronized void receiveDefense(String group, boolean hit, boolean block, int hitX, int hitY, String attacker, String victim){
		int currentAttacker = currentAttackerMap.get(getGroupName(group));
		if(!hit && !block){//miss
			string2bt.get(attacker).attackMissed(hitX, hitY, victim);
		}
		else if(hit && !block){
			string2bt.get(attacker).attackHit(hitX, hitY, victim);
		}
		else if(block){
			string2bt.get(attacker).sendNothing();
		}
		if(!consecutive) getNextAttacker(group);
		else sendMode(group, currentAttacker);
	}
	
	public void receiveReflect(String attacker){
		string2bt.get(attacker).sendReflect();
	}
	
	public void receiveDead(String group, String victim){
		ArrayList<BattleshipThread> btList = gameGroupMap.get(getGroupName(group));
		String check = checkWin(getGroupName(group));
		if(!check.isEmpty()){
			string2bt.get(check).sendWin();
			return;
		}
		for(int i=0; i<btList.size(); i++){
			if(btList.get(i).getUsername().equals(victim)) continue;
			btList.get(i).sendDead(victim);
		}
		getNextAttacker(group);
	}
	
	public void receiveQuit(String group, String username, String status){ //already dead player or winner
		if(status.equals("win")){ //remove entire group
			gameGroupMap.remove(getGroupName(group));
			currentAttackerMap.remove(getGroupName(group));
			numPlayersMap.remove(getGroupName(group));
			groupAvailableMap.remove(getGroupName(group));
			numCellsMap.remove(getGroupName(group));
			groupNameList.remove(getGroupName(group));
		}
		if(status.equals("lose")){
			
		}
	}
	
	public void setConsecutive(boolean b){
		consecutive = b;
	}
	public void setConsecutiveCount(int c){
		consecutiveCount = c;
	}
	
	public String checkWin(String group){
		ArrayList<BattleshipThread> btList = gameGroupMap.get(getGroupName(group));
		int count = 0; String id = "";
		for(int i=0; i<btList.size(); i++){
			if(btList.get(i).getStatus()==1){
				count++; id = btList.get(i).getUsername();
			}
		}
		if(count==1) return id;
		return "";
	}
	
	public String getGroupName(String group){
		for(int i=0; i<groupNameList.size(); i++){
			if(groupNameList.get(i).equals(group)){
				return groupNameList.get(i);
			}
		}
		return null;
	}
	
	public int getNumCells(String group){
		return numCellsMap.get(getGroupName(group));
	}
	
	public int getLevel(String group){
		return groupLevelMap.get(getGroupName(group));
	}
	
	public Map<String, Integer> getGroupAvailable(){
		return groupAvailableMap;
	}
	
	public static void main(String[] args){
		BattleshipServer bsServer = new BattleshipServer(9641);
	}
}

