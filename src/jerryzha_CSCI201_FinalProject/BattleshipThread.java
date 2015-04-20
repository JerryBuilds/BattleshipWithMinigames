package chauvu_CSCI201_FinalProject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;


public class BattleshipThread extends Thread{
	private String username;
	private Socket s;
	private static BattleshipServer bs;
	private PrintWriter pw;
	private BufferedReader br;
	
	//data
	private boolean donedraw = false;
	private int status = 1; //alive
	private String group;
	private boolean isRunning = true;
	
	public BattleshipThread(Socket s, BattleshipServer bs){
		super();
		this.s = s;
		this.bs = bs;
		try{
			pw = new PrintWriter(s.getOutputStream());
			br = new BufferedReader(new InputStreamReader(s.getInputStream()));
			
			//start up info
			while(true){
				String line = br.readLine();
				String[] parts = line.split(" ");
				if(parts[0].equals("startup")){
					if(parts[1].equals("login")){
						if(!bs.usernameAlreadyExists(parts[2])){
							pw.println("startup login nouser"); pw.flush();
							continue;
						}
						if(!bs.loginAttempt(parts[2], parts[3])){
							pw.println("startup login wrongpass"); pw.flush();
							continue;
						}
					}
					else if(parts[1].equals("signup")){
						if(bs.usernameAlreadyExists(parts[2])){
							pw.println("startup signup usertaken"); pw.flush();
							continue;
						}
						if(!bs.usernameValid(parts[2])){
							pw.println("startup signup userinvalid"); pw.flush();
							continue;
						}
						try{
							bs.createNewUser(parts[2], parts[3]);
						}
						catch(SQLException sqle){
							System.out.println("sqle: "+sqle.getMessage());
						}
					}
					else if(parts[1].equals("ai")){
						break;
					}
					pw.println("startup approved"); pw.flush();
					break;
				}
				else{
					System.out.println("STARTUP ERROR");
				}
			}
			
			//log in info
			String line = br.readLine();
			String[] parts = line.split(" ");
			if(parts[0].equals("login")){
				if(parts[1].equals("start")){ //start new game
					group = parts[2];
					username = parts[2];
					bs.addNewGroup(this, parts[2], Integer.parseInt(parts[3]), Integer.parseInt(parts[4]), Integer.parseInt(parts[5]), Integer.parseInt(parts[6]));
				}
				else if(parts[1].equals("groups")){ //join existing game
					Map<String, Integer> groupNames = bs.getGroupAvailable();
					String str = "groups ";
					for(String i: groupNames.keySet()){
						if(groupNames.get(i)==1){
							str += i+" ";
						}
					}
					pw.println(str); pw.flush();
					line = br.readLine();
					parts = line.split(" ");
					if(parts[1].equals("join")){
						group = parts[2];
						username = parts[3];
						bs.joinExistingGroup(this, parts[2], parts[3]);
						pw.println("cells "+bs.getNumCells(group)+" "+bs.getLevel(group)); pw.flush();
					}
				}
				else if(parts[1].equals("ai")){
					group = parts[2];
					username = parts[3];
					bs.joinExistingGroupAI(this, parts[2], parts[3]);
				}
			}
			else{
				System.out.println("LOG IN ERROR");
			}
		}
		catch(IOException ioe){
			System.out.println("ioe in ChatThread: "+ioe.getMessage());
		}
	}
	
	public void run(){
		//check if player is done
		try{
			String line = br.readLine();
			if(line.equals("drawdone")){
				donedraw = true;
				bs.drawDone(this);
			}
		}
		catch(IOException ioe){
			System.out.println("ioe: "+ioe.getMessage());
		}
		try{
			String line = ""; String[] parts;
			while(isRunning){
				line = br.readLine();
				parts = line.split(" ");
				if(parts[0].equals("forfeit")){
					System.out.println("forfeit");
					bs.receiveDead(group, username);
					return;
				}
				if(parts[0].equals("quit")){
					System.out.println("quit");
					bs.receiveQuit(group, username, parts[1]);
					return;
				}
				if(parts[0].equals("defense")){
					if(parts[1].equals("dead")){
						status = 0;
						bs.receiveDead(group, username);
					}
					else if(parts[1].equals("block")){
						bs.receiveDefense(group, false, true, 0, 0, parts[2], username);
					}
					else if(parts[1].equals("reflect")){
						bs.receiveReflect(parts[2]);
					}
					else{
						bs.receiveDefense(group, parts[1].equals("hit"), false, Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), parts[4], username);
					}
				}
				else if(parts[0].equals("attack")){
					if(parts.length > 4 && parts[4].equals("consecutive")){
						bs.setConsecutive(true);
						bs.setConsecutiveCount(Integer.parseInt(parts[5]));
					}
					if(!parts[1].equals("none")) bs.sendAttack(group, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), username, parts[3]);
					else bs.sendNothing(group, username);
				}
			}
		}
		catch(IOException ioe){
			System.out.println("ioe: "+ioe.getMessage());
		}
	}
	
	public void drawDone(){
		pw.println("drawdone");
		pw.flush();
	}
	
	public void sendNothing(){
		pw.println("nothing");
		pw.flush();
	}
	
	public void sendAttack(int hitX, int hitY, String attacker){
		pw.println("attack "+ hitX+" "+hitY+" "+attacker);
		pw.flush();
	}
	
	public void attackMissed(int hitX, int hitY, String victim){
		pw.println("missed "+hitX+" "+hitY+" "+victim);
		pw.flush();
	}
	
	public void attackHit(int hitX, int hitY, String victim){
		pw.println("hit "+hitX+" "+hitY+" "+victim);
		pw.flush();
	}
	
	public void sendReflect(){
		pw.println("reflect");
		pw.flush();
	}
	
	public void sendDead(String victim){
		pw.println("dead "+victim);
		pw.flush();
	}
	
	public void sendWin(){
		pw.println("win");
		pw.flush();
	}
	
	public void sendMode(String mode){
		pw.println("mode "+mode);
		pw.flush();
	}
	
	public void sendMyID(String myID, int totalSize){
		pw.println("myID "+myID+" "+totalSize);
		pw.flush();
	}
	public void sendOppID(String oppID){
		pw.println("oppID "+oppID);
		pw.flush();
	}
	
	//setters and getters
	public boolean getDoneDraw(){
		return donedraw;
	}
	public int getStatus(){
		return status;
	}
	public String getGroup(){
		return group;
	}
	public String getUsername(){
		return username;
	}
}

