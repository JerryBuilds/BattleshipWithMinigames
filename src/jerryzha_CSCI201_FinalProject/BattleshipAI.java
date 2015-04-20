package chauvu_CSCI201_FinalProject;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;


public abstract class BattleshipAI extends Thread{
	private static int aiCount = 1;
	
	//IO
	protected PrintWriter pw;
	protected BufferedReader br;
	protected Scanner scan;
	protected Random rand;
	
	//DATA	
	protected int cellX;
	protected int cellY = 10;
	protected String mode; //attack or defense
	protected String status = "initializing";
	protected int aiID;
	protected String myID;
	protected String[] oppID;
	protected boolean chanceTaken = false;
	protected boolean chanceStatus = false;
	protected int chanceMode = -1;
	protected int consecutive = -1;
	protected boolean dead = false;
	protected boolean isRunning = true;
	protected int r;
	protected int level;
	
	//GRID
	protected boolean myGrid[][];
	protected ArrayList<Integer> myHitXList, myHitYList;
	protected HashMap<String,OppGrid> oppGridList;
	
	//SHIP PLACEMENT
	protected int currDrawShipCount, currDrawShipIdx, shipSize;
	protected Color shipColor;
	protected String whichShip;
	protected int countShip;
	
	//SHIP LIST
	protected ArrayList<Integer> drawShipXList, drawShipYList;
	protected ArrayList<Integer> drawShipWidthList, drawShipHeightList;
	protected ArrayList<Integer> drawHitX, drawHitY;
	protected ArrayList<Color> drawShipColor;
	
	
	public BattleshipAI(int cell, String group, int level){
		super();
		rand = new Random();
		aiID = aiCount++;
		this.cellX = cell;
		this.level = level;
		myGrid = new boolean[cell][cell];
		for(int i=0; i<cell; i++){
			for(int j=0; j<cell; j++){
				myGrid[i][j] = false;
			}
		}
		myHitXList = new ArrayList<Integer>();
		myHitYList = new ArrayList<Integer>();
		oppGridList = new HashMap<String,OppGrid>();
		whichShip = "";
		currDrawShipIdx = 0;
		currDrawShipCount = 0;
		countShip = 0;
		
		drawShipXList = new ArrayList<Integer>();
		drawShipYList = new ArrayList<Integer>();
		drawShipWidthList = new ArrayList<Integer>();
		drawShipHeightList = new ArrayList<Integer>();
		drawHitX = new ArrayList<Integer>();
		drawHitY = new ArrayList<Integer>();
		drawShipColor = new ArrayList<Color>();
		
		try{
			Socket s = new Socket("localhost", 7777);
			pw = new PrintWriter(s.getOutputStream());
			br = new BufferedReader(new InputStreamReader(s.getInputStream()));
			pw.println("startup ai"); pw.flush();
			pw.println("login ai "+group+" "+aiID);
			pw.flush();
		}
		catch(IOException ioe){
			System.out.println("ioe in client: "+ioe.getMessage());
		}

		start();
	}
	
	public void run(){
		//read initial info
		try{
			String line = br.readLine();
			String[] parts = line.split(" ");
			if(!parts[0].equals("myID")) System.out.println("MYID ERROR");
			myID = parts[1];
			System.out.println("ID: "+myID);
			oppID = new String[Integer.parseInt(parts[2])-1];
			for(int i=0; i<Integer.parseInt(parts[2])-1; i++){
				String line2 = br.readLine();
				String[] parts2 = line2.split(" ");
				if(!parts2[0].equals("oppID")) System.out.println("OPPID ERROR");
				oppID[i] = parts2[1];
			}
			
			//create my and opp's grid
			createMyGrid();
			for(int i=0; i<oppID.length; i++){
				createOppGrid(oppID[i]);
			}
		}
		catch(IOException ioe){
			System.out.println("ioe: "+ioe.getMessage());
		}
		//read initial mode
		try{
			String line = br.readLine();
			String[] parts = line.split(" ");
			if(!parts[0].equals("mode")) System.out.println("MODE ERROR");
			mode = parts[1];
		}
		catch(IOException ioe){
			System.out.println("ioe: "+ioe.getMessage());
		}
		//check all players have placed ships
		try{
			String line = br.readLine();
			String[] parts;
			if(line.equals("drawdone")){
				while(isRunning){
					//print AI's board
//					System.out.println("AI'S BOARD");
//					for(int i=0; i<cell; i++){
//						for(int j=0; j<cell; j++){
//							if(myGrid[j][i]) System.out.print("O ");
//							else System.out.print("X ");
//						}
//						System.out.println();
//					}
					//System.out.println("mode: "+mode);
					
					//take chance? 2% probability of taking chance
					if(chanceStatus) chanceStatus = false;
					if(chanceTaken==false){
						r = rand.nextInt(50);
						if(r==0){
							chanceStatus = true;
							r = rand.nextInt(4);
						}
					}

					if(mode.equals("attack")){
						for(String i: oppGridList.keySet()) oppGridList.get(i).attackModeOn();
						int r = rand.nextInt(level+2);
						if(r==0) nextMove(); //AI attacks
						else{
							pw.println("attack none"); pw.flush();
						}
						setStatus(mode);
						line = br.readLine();
						parts = line.split(" ");
						if(parts[0].equals("win")){ //winner
							for(String i: oppGridList.keySet()) oppGridList.get(i).dead();
							setStatus(parts[0]);
							return;
						}
						if(parts[0].equals("dead")){
							oppGridList.get(parts[1]).dead();
							line = br.readLine();
							parts = line.split(" ");
						}
						else if(parts[0].equals("reflect")){
							line = br.readLine();
							parts = line.split(" ");
							if(parts[0].equals("attack")){
								int x = Integer.parseInt(parts[1]);
								int y = Integer.parseInt(parts[2]);
								String attacker = parts[3];
								if(checkAttack(x,y)){
									if(dead){
										pw.println("defense dead"); pw.flush();
										return;
									}
									else{
										pw.println("defense hit "+x+" "+y+" "+attacker); pw.flush();
									}
								}
								else{
									pw.println("defense miss "+x+" "+y+" "+attacker); pw.flush();
								}
							}
						}
						else if(parts[0].equals("missed")){//missed
							oppGridList.get(parts[3]).attackMissed(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
							line = br.readLine();
							parts = line.split(" ");
						}
						else if(parts[0].equals("hit")){//hit
							oppGridList.get(parts[3]).attackHit(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
							line = br.readLine();
							parts = line.split(" ");
						}
						if(parts[0].equals("mode")){
							if(parts[1].equals("attack")) mode = "attack";
							else if(parts[1].equals("defense")) mode = "defense";
						}
					}
					else if(mode.equals("defense")){
						for(String i: oppGridList.keySet()) oppGridList.get(i).attackModeOff();
						setStatus(mode);
						line = br.readLine();
						parts = line.split(" ");
						if(parts[0].equals("attack")){
							int x = Integer.parseInt(parts[1]);
							int y = Integer.parseInt(parts[2]);
							String attacker = parts[3];
							if(chanceMode==3){ //block current attack
								pw.println("defense block "+attacker); pw.flush();
								chanceMode = -1;
								chanceTaken = true; chanceStatus = false;
							}
							else if(chanceMode==4){ //reflect current attack
								pw.println("defense reflect "+attacker); pw.flush();
								sendAttack(x, y, attacker);
								line = br.readLine();
								parts = line.split(" ");
								if(parts[0].equals("win")){ //winner
									for(String i: oppGridList.keySet()) oppGridList.get(i).dead();
									setStatus("Win");
									return;
								}
								if(parts[0].equals("dead")) oppGridList.get(parts[1]).dead();
								else if(parts[0].equals("missed")) oppGridList.get(parts[3]).attackMissed(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
								else if(parts[0].equals("hit")) oppGridList.get(parts[3]).attackHit(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
								chanceMode = -1;
								chanceTaken = true; chanceStatus = false;
							}
							else if(checkAttack(x,y)){
								if(dead){
									pw.println("defense dead"); pw.flush();
									return;
								}
								else{
									pw.println("defense hit "+x+" "+y+" "+attacker); pw.flush();
								}
							}
							else{
								pw.println("defense miss "+x+" "+y+" "+attacker); pw.flush();
							}
						}
						if(parts[0].equals("dead")){
							oppGridList.get(parts[1]).dead();
						}
						line = br.readLine();
						parts = line.split(" ");
						if(parts[0].equals("dead")){
							oppGridList.get(parts[1]).dead();
							line = br.readLine();
							parts = line.split(" ");
						}
						if(parts[0].equals("mode")){
							if(parts[1].equals("attack")) mode = "attack";
							else if(parts[1].equals("defense")) mode = "defense";
						}
					}
				}
			}
		}
		catch(IOException ioe){
			System.out.println("ioe: "+ioe.getMessage());
		}
	}
	
	public abstract void nextMove(); //to be written in child classes
	public abstract void createMyGrid();
	
	public void createOppGrid(String oppid){
		OppGrid og = new OppGrid(oppid, cellX, this);
		oppGridList.put(oppid, og);
	}
	
	public void doneDrawShip(){
		setStatus("Waiting for Opponents");
		pw.println("drawdone");
		pw.flush();
	}
	
	public void setStatus(String s){
		status = s;
	}
	
	public boolean checkAttack(int x, int y){
		if(myGrid[x][y]){ //hit
			myGrid[x][y] = false;
			myHitXList.add(new Integer(x));
			myHitYList.add(new Integer(y));
			//check alive
			boolean alive = false;
			for(int i=0; i<cellX; i++){
				for(int j=0; j<cellY; j++){
					if(myGrid[i][j]){
						alive = true; break;
					}
				}
				if(alive) break;
			}
			if(!alive){
				deadStatus();
			}
			return true;
		}
		return false;
	}
	
	public void sendAttack(int hitX, int hitY, String victim){
		if(chanceMode==2){
			pw.println("attack "+hitX+" "+hitY+" "+victim+" consecutive 1");
			pw.flush();
			chanceMode = -1;
			chanceTaken = true;
			chanceStatus = false;
		}
		else if(chanceMode==1){
			pw.println("attack "+hitX+" "+hitY+" "+victim+" consecutive "+consecutive);
			pw.flush();
			try{
				Thread.sleep(50);
			}
			catch(InterruptedException ie){
				System.out.println("InterruptedException: "+ie.getMessage());
			}
			if(consecutive<=1){
				chanceMode = -1;
				chanceTaken = true;
				chanceStatus = false;
			}
		}
		else{
			pw.println("attack "+hitX+" "+hitY+" "+victim);
			pw.flush();
		}
	}
	
	public void deadStatus(){
		dead = true; isRunning = false;
	}
	
	public boolean getChanceStatus(){
		return chanceStatus;
	}
	public int getChanceMode(){
		return chanceMode;
	}
}

class AILevel1 extends BattleshipAI{
	private Random rand = new Random();
	private int r;
	public AILevel1(int cell, String group, int level){
		super(cell, group, level);
	}
	public void createMyGrid(){ //random ship placements
		while(currDrawShipIdx<(Globals.shipTypeNum)){
			shipSize = Globals.shipSize[currDrawShipIdx];
			shipColor = Globals.shipColor[currDrawShipIdx];
			boolean overlap = false;
			//random horizontal/vertical
			int shipX, shipY;
			r = rand.nextInt(2);
			if(r==0){
				whichShip = "horizontal";
				shipX = rand.nextInt(cellX-shipSize+1);
				shipY = rand.nextInt(cellY);
				for(int i=0; i<shipSize; i++){
					if(myGrid[shipX+i][shipY]){ //overlap
						overlap = true; break;
					}
				}
				if(overlap) continue;
				for(int i=0; i<shipSize; i++){
					myGrid[shipX+i][shipY] = true;
				}
				drawShipXList.add(shipX);
				drawShipYList.add(shipY);
				drawShipWidthList.add(shipSize);
				drawShipHeightList.add(1);
			}
			else{
				whichShip = "vertical";
				shipY = rand.nextInt(cellY-shipSize+1);
				shipX = rand.nextInt(cellX);
				for(int i=0; i<shipSize; i++){
					if(myGrid[shipX][shipY+i]){ //overlap
						overlap = true; break;
					}
				}
				if(overlap) continue;
				for(int i=0; i<shipSize; i++){
					myGrid[shipX][shipY+i] = true;
				}
				drawShipXList.add(shipX);
				drawShipYList.add(shipY);
				drawShipWidthList.add(1);
				drawShipHeightList.add(shipSize);
			}
			drawShipColor.add(shipColor);
			countShip++;
			currDrawShipCount++;
			if(Globals.shipNumber[currDrawShipIdx]==currDrawShipCount){
				currDrawShipCount = 0;
				currDrawShipIdx++;
			}

		}
		doneDrawShip();
	}
	public void nextMove(){ //random next move
		//random opponent
		r = rand.nextInt(oppGridList.size());
		int idx = 0;
		OppGrid og = null; String ogIdx = "";
		for(String i: oppGridList.keySet()){
			if(idx==r){
				og = oppGridList.get(i); ogIdx = i;
			}
			idx++;
		}
		//random attack on OG
		ArrayList<OppGridCoord> unhitcoords = og.getUnHitCoordList();
		r = rand.nextInt(unhitcoords.size());
		int attackX = unhitcoords.get(r).getX(); int attackY = unhitcoords.get(r).getY();
		sendAttack(attackX, attackY, ogIdx);
		if(getChanceStatus() && getChanceMode()==1){ //chance to attack 4 directions
			if(attackX-1>=0){ //left
				sendAttack(attackX-1, attackY, ogIdx);
			}
			if(attackX+1<cellX){ //right
				sendAttack(attackX+1, attackY, ogIdx);
			}
			if(attackY-1>=0){ //above
				sendAttack(attackX, attackY-1, ogIdx);
			}
			if(attackY+1<cellY){ //below
				sendAttack(attackX, attackY+1, ogIdx);
			}
			chanceMode = -1;
			chanceTaken = true;
			chanceStatus = false;
		}
	}
}


class AILevel2 extends BattleshipAI{
	private Random rand = new Random();
	private int r;
	public AILevel2(int cell, String group, int level){
		super(cell, group, level);
	}
	public void createMyGrid(){ //random ship placements
		while(currDrawShipIdx<(Globals.shipTypeNum)){
			shipSize = Globals.shipSize[currDrawShipIdx];
			shipColor = Globals.shipColor[currDrawShipIdx];
			//random horizontal/vertical
			int shipX, shipY;
			r = rand.nextInt(2);
			boolean overlap = false;
			if(r==0){
				whichShip = "horizontal";
				shipX = rand.nextInt(cellX-shipSize+1);
				shipY = rand.nextInt(cellY);
				for(int i=0; i<shipSize; i++){
					if(myGrid[shipX+i][shipY]){ //overlap
						overlap = true; break;
					}
				}
				if(overlap) continue;
				for(int i=0; i<shipSize; i++){
					myGrid[shipX+i][shipY] = true;
				}
				drawShipXList.add(shipX);
				drawShipYList.add(shipY);
				drawShipWidthList.add(shipSize);
				drawShipHeightList.add(1);
			}
			else{
				whichShip = "vertical";
				shipY = rand.nextInt(cellY-shipSize+1);
				shipX = rand.nextInt(cellX);
				for(int i=0; i<shipSize; i++){
					if(myGrid[shipX][shipY+i]){ //overlap
						overlap = true; break;
					}
				}
				if(overlap) continue;
				for(int i=0; i<shipSize; i++){
					myGrid[shipX][shipY+i] = true;
				}
				drawShipXList.add(shipX);
				drawShipYList.add(shipY);
				drawShipWidthList.add(1);
				drawShipHeightList.add(shipSize);
			}
			drawShipColor.add(shipColor);
			countShip++;
			currDrawShipCount++;
			if(Globals.shipNumber[currDrawShipIdx]==currDrawShipCount){
				currDrawShipCount = 0;
				currDrawShipIdx++;
			}

		}
		doneDrawShip();
	}
	public void nextMove(){ //random next move
		//random opponent
		r = rand.nextInt(oppGridList.size());
		int idx = 0;
		OppGrid og = null; String ogIdx = "";
		for(String i: oppGridList.keySet()){
			if(idx==r){
				og = oppGridList.get(i); ogIdx = i;
			}
			idx++;
		}
		//attack on OG
		int attackX = -1; int attackY = -1;
		int targetX = -1; int targetY = -1;
		boolean flag = false;
		ArrayList<OppGridCoord> unhitcoords = og.getUnHitCoordList();
		ArrayList<OppGridCoord> hitcoords = og.getHitCoordList();
		OppGridCoord coord = null;
		
		for(int i=0; i<hitcoords.size(); i++){
			if(!hitcoords.get(i).getConsidered()){
				flag = false;
				coord = hitcoords.get(i); 
				targetX = coord.getX(); targetY = coord.getY()-1; //above
				//System.out.print(og.checkIfAlreadyHit(targetX, targetY)+""+og.checkIfAlreadyMissed(targetX, targetY));
				if(!flag && !og.checkIfAlreadyHit(targetX, targetY) && !og.checkIfAlreadyMissed(targetX, targetY)){
					if(targetY>=0){
						attackX = targetX; attackY = targetY; flag = true;
					}
				}
				targetY = coord.getY()+1; //below
				//System.out.print(og.checkIfAlreadyHit(targetX, targetY)+""+og.checkIfAlreadyMissed(targetX, targetY));
				if(!flag && !og.checkIfAlreadyHit(targetX, targetY) && !og.checkIfAlreadyMissed(targetX, targetY)){
					if(targetY<cellY){
						attackX = targetX; attackY = targetY; flag = true;
					}
				}
				targetY = coord.getY(); targetX = coord.getX()-1; //left
				//System.out.print(og.checkIfAlreadyHit(targetX, targetY)+""+og.checkIfAlreadyMissed(targetX, targetY));
				if(!flag && !og.checkIfAlreadyHit(targetX, targetY) && !og.checkIfAlreadyMissed(targetX, targetY)){
					if(targetX>=0){
						attackX = targetX; attackY = targetY; flag = true;
					}
				}
				targetX = coord.getX()+1; //right
				//System.out.print(og.checkIfAlreadyHit(targetX, targetY)+""+og.checkIfAlreadyMissed(targetX, targetY));
				if(!flag && !og.checkIfAlreadyHit(targetX, targetY) && !og.checkIfAlreadyMissed(targetX, targetY)){
					if(targetX<cellX){
						attackX = targetX; attackY = targetY; flag = true;
					}
				}
				if(flag){
					break;
				}
				else{
					//System.out.println("considered: "+coord.getX()+" "+coord.getY());
					coord.setConsidered();
				}
			}
		}	
		if(!flag){
			r = rand.nextInt(unhitcoords.size());
			attackX = unhitcoords.get(r).getX(); attackY = unhitcoords.get(r).getY();
		}
		sendAttack(attackX, attackY, ogIdx);
		if(getChanceStatus() && getChanceMode()==1){ //chance to attack 4 directions
			if(attackX-1>=0){ //left
				sendAttack(attackX-1, attackY, ogIdx);
			}
			if(attackX+1<cellX){ //right
				sendAttack(attackX+1, attackY, ogIdx);
			}
			if(attackY-1>=0){ //above
				sendAttack(attackX, attackY-1, ogIdx);
			}
			if(attackY+1<cellY){ //below
				sendAttack(attackX, attackY+1, ogIdx);
			}
			chanceMode = -1;
			chanceTaken = true;
			chanceStatus = false;
		}
	}
}


class AILevel3 extends BattleshipAI{ //horizontal ship placement mostly
	private Random rand = new Random();
	private int r;
	public AILevel3(int cell, String group, int level){
		super(cell, group, level);
	}
	
	public void createMyGrid(){
		while(currDrawShipIdx<(Globals.shipTypeNum)){
			shipSize = Globals.shipSize[currDrawShipIdx];
			shipColor = Globals.shipColor[currDrawShipIdx];
			//random horizontal/vertical
			int shipX, shipY;
			r = rand.nextInt(10);
			boolean overlap = false;
			if(r>2){
				whichShip = "horizontal";
				shipX = rand.nextInt(cellX-shipSize+1);
				shipY = rand.nextInt(cellY);
				for(int i=0; i<shipSize; i++){
					if(myGrid[shipX+i][shipY]){ //overlap
						overlap = true; break;
					}
				}
				if(overlap) continue;
				for(int i=0; i<shipSize; i++){
					myGrid[shipX+i][shipY] = true;
				}
				drawShipXList.add(shipX);
				drawShipYList.add(shipY);
				drawShipWidthList.add(shipSize);
				drawShipHeightList.add(1);
			}
			else{
				whichShip = "vertical";
				shipY = rand.nextInt(cellY-shipSize+1);
				shipX = rand.nextInt(cellX);
				for(int i=0; i<shipSize; i++){
					if(myGrid[shipX][shipY+i]){ //overlap
						overlap = true; break;
					}
				}
				if(overlap) continue;
				for(int i=0; i<shipSize; i++){
					myGrid[shipX][shipY+i] = true;
				}
				drawShipXList.add(shipX);
				drawShipYList.add(shipY);
				drawShipWidthList.add(1);
				drawShipHeightList.add(shipSize);
			}
			drawShipColor.add(shipColor);
			countShip++;
			currDrawShipCount++;
			if(Globals.shipNumber[currDrawShipIdx]==currDrawShipCount){
				currDrawShipCount = 0;
				currDrawShipIdx++;
			}

		}
		doneDrawShip();
	}
	
	public void nextMove(){ //random next move
		//random opponent
		r = rand.nextInt(oppGridList.size());
		int idx = 0;
		OppGrid og = null; String ogIdx = "";
		for(String i: oppGridList.keySet()){
			if(idx==r){
				og = oppGridList.get(i); ogIdx = i;
			}
			idx++;
		}
		//attack on OG
		int attackX = -1; int attackY = -1;
		int targetX = -1; int targetY = -1;
		boolean flag = false;
		ArrayList<OppGridCoord> unhitcoords = og.getUnHitCoordList();
		ArrayList<OppGridCoord> hitcoords = og.getHitCoordList();
		OppGridCoord coord = null;
		
		for(int i=0; i<hitcoords.size(); i++){
			if(!hitcoords.get(i).getConsidered()){
				flag = false;
				coord = hitcoords.get(i); 
				targetX = coord.getX(); targetY = coord.getY()-1; //above
				//System.out.print(og.checkIfAlreadyHit(targetX, targetY)+""+og.checkIfAlreadyMissed(targetX, targetY));
				if(!flag && !og.checkIfAlreadyHit(targetX, targetY) && !og.checkIfAlreadyMissed(targetX, targetY)){
					if(targetY>=0){
						attackX = targetX; attackY = targetY; flag = true;
					}
				}
				targetY = coord.getY()+1; //below
				//System.out.print(og.checkIfAlreadyHit(targetX, targetY)+""+og.checkIfAlreadyMissed(targetX, targetY));
				if(!flag && !og.checkIfAlreadyHit(targetX, targetY) && !og.checkIfAlreadyMissed(targetX, targetY)){
					if(targetY<cellY){
						attackX = targetX; attackY = targetY; flag = true;
					}
				}
				targetY = coord.getY(); targetX = coord.getX()-1; //left
				//System.out.print(og.checkIfAlreadyHit(targetX, targetY)+""+og.checkIfAlreadyMissed(targetX, targetY));
				if(!flag && !og.checkIfAlreadyHit(targetX, targetY) && !og.checkIfAlreadyMissed(targetX, targetY)){
					if(targetX>=0){
						attackX = targetX; attackY = targetY; flag = true;
					}
				}
				targetX = coord.getX()+1; //right
				//System.out.print(og.checkIfAlreadyHit(targetX, targetY)+""+og.checkIfAlreadyMissed(targetX, targetY));
				if(!flag && !og.checkIfAlreadyHit(targetX, targetY) && !og.checkIfAlreadyMissed(targetX, targetY)){
					if(targetX<cellX){
						attackX = targetX; attackY = targetY; flag = true;
					}
				}
				if(flag){
					break;
				}
				else{
					//System.out.println("considered: "+coord.getX()+" "+coord.getY());
					coord.setConsidered();
				}
			}
		}	
		if(!flag){
			r = rand.nextInt(unhitcoords.size());
			attackX = unhitcoords.get(r).getX(); attackY = unhitcoords.get(r).getY();
		}
		sendAttack(attackX, attackY, ogIdx);
		if(getChanceStatus() && getChanceMode()==1){ //chance to attack 4 directions
			if(attackX-1>=0){ //left
				sendAttack(attackX-1, attackY, ogIdx);
			}
			if(attackX+1<cellX){ //right
				sendAttack(attackX+1, attackY, ogIdx);
			}
			if(attackY-1>=0){ //above
				sendAttack(attackX, attackY-1, ogIdx);
			}
			if(attackY+1<cellY){ //below
				sendAttack(attackX, attackY+1, ogIdx);
			}
			chanceMode = -1;
			chanceTaken = true;
			chanceStatus = false;
		}
	}
}

class AILevel4 extends BattleshipAI{ //vertical ship placement mostly
	private Random rand = new Random();
	private int r;
	public AILevel4(int cell, String group, int level){
		super(cell, group, level);
	}
	
	public void createMyGrid(){
		while(currDrawShipIdx<(Globals.shipTypeNum)){
			shipSize = Globals.shipSize[currDrawShipIdx];
			shipColor = Globals.shipColor[currDrawShipIdx];
			//random horizontal/vertical
			int shipX, shipY;
			r = rand.nextInt(10);
			boolean overlap = false;
			if(r>8){
				whichShip = "horizontal";
				shipX = rand.nextInt(cellX-shipSize+1);
				shipY = rand.nextInt(cellY);
				for(int i=0; i<shipSize; i++){
					if(myGrid[shipX+i][shipY]){ //overlap
						overlap = true; break;
					}
				}
				if(overlap) continue;
				for(int i=0; i<shipSize; i++){
					myGrid[shipX+i][shipY] = true;
				}
				drawShipXList.add(shipX);
				drawShipYList.add(shipY);
				drawShipWidthList.add(shipSize);
				drawShipHeightList.add(1);
			}
			else{
				whichShip = "vertical";
				shipY = rand.nextInt(cellY-shipSize+1);
				shipX = rand.nextInt(cellX);
				for(int i=0; i<shipSize; i++){
					if(myGrid[shipX][shipY+i]){ //overlap
						overlap = true; break;
					}
				}
				if(overlap) continue;
				for(int i=0; i<shipSize; i++){
					myGrid[shipX][shipY+i] = true;
				}
				drawShipXList.add(shipX);
				drawShipYList.add(shipY);
				drawShipWidthList.add(1);
				drawShipHeightList.add(shipSize);
			}
			drawShipColor.add(shipColor);
			countShip++;
			currDrawShipCount++;
			if(Globals.shipNumber[currDrawShipIdx]==currDrawShipCount){
				currDrawShipCount = 0;
				currDrawShipIdx++;
			}

		}
		doneDrawShip();
	}
	
	public void nextMove(){ //random next move
		//random opponent
		r = rand.nextInt(oppGridList.size());
		int idx = 0;
		OppGrid og = null; String ogIdx = "";
		for(String i: oppGridList.keySet()){
			if(idx==r){
				og = oppGridList.get(i); ogIdx = i;
			}
			idx++;
		}
		//attack on OG
		int attackX = -1; int attackY = -1;
		int targetX = -1; int targetY = -1;
		boolean flag = false;
		ArrayList<OppGridCoord> unhitcoords = og.getUnHitCoordList();
		ArrayList<OppGridCoord> hitcoords = og.getHitCoordList();
		OppGridCoord coord = null;
		
		for(int i=0; i<hitcoords.size(); i++){
			if(!hitcoords.get(i).getConsidered()){
				flag = false;
				coord = hitcoords.get(i); 
				targetX = coord.getX(); targetY = coord.getY()-1; //above
				//System.out.print(og.checkIfAlreadyHit(targetX, targetY)+""+og.checkIfAlreadyMissed(targetX, targetY));
				if(!flag && !og.checkIfAlreadyHit(targetX, targetY) && !og.checkIfAlreadyMissed(targetX, targetY)){
					if(targetY>=0){
						attackX = targetX; attackY = targetY; flag = true;
					}
				}
				targetY = coord.getY()+1; //below
				//System.out.print(og.checkIfAlreadyHit(targetX, targetY)+""+og.checkIfAlreadyMissed(targetX, targetY));
				if(!flag && !og.checkIfAlreadyHit(targetX, targetY) && !og.checkIfAlreadyMissed(targetX, targetY)){
					if(targetY<cellY){
						attackX = targetX; attackY = targetY; flag = true;
					}
				}
				targetY = coord.getY(); targetX = coord.getX()-1; //left
				//System.out.print(og.checkIfAlreadyHit(targetX, targetY)+""+og.checkIfAlreadyMissed(targetX, targetY));
				if(!flag && !og.checkIfAlreadyHit(targetX, targetY) && !og.checkIfAlreadyMissed(targetX, targetY)){
					if(targetX>=0){
						attackX = targetX; attackY = targetY; flag = true;
					}
				}
				targetX = coord.getX()+1; //right
				//System.out.print(og.checkIfAlreadyHit(targetX, targetY)+""+og.checkIfAlreadyMissed(targetX, targetY));
				if(!flag && !og.checkIfAlreadyHit(targetX, targetY) && !og.checkIfAlreadyMissed(targetX, targetY)){
					if(targetX<cellX){
						attackX = targetX; attackY = targetY; flag = true;
					}
				}
				if(flag){
					break;
				}
				else{
					//System.out.println("considered: "+coord.getX()+" "+coord.getY());
					coord.setConsidered();
				}
			}
		}	
		if(!flag){
			r = rand.nextInt(unhitcoords.size());
			attackX = unhitcoords.get(r).getX(); attackY = unhitcoords.get(r).getY();
		}
		sendAttack(attackX, attackY, ogIdx);
		if(getChanceStatus() && getChanceMode()==1){ //chance to attack 4 directions
			if(attackX-1>=0){ //left
				sendAttack(attackX-1, attackY, ogIdx);
			}
			if(attackX+1<cellX){ //right
				sendAttack(attackX+1, attackY, ogIdx);
			}
			if(attackY-1>=0){ //above
				sendAttack(attackX, attackY-1, ogIdx);
			}
			if(attackY+1<cellY){ //below
				sendAttack(attackX, attackY+1, ogIdx);
			}
			chanceMode = -1;
			chanceTaken = true;
			chanceStatus = false;
		}
	}
}

class AILevel5 extends BattleshipAI{ //clustered ship placement
	private Random rand = new Random();
	private int r;
	public AILevel5(int cell, String group, int level){
		super(cell, group, level);
	}
	
	public void createMyGrid(){
		//r: 0 is top left, 1 is top right, 2 is bottom left, 3 is bottom right
		int quad = rand.nextInt(4); 
		while(currDrawShipIdx<(Globals.shipTypeNum)){
			shipSize = Globals.shipSize[currDrawShipIdx];
			shipColor = Globals.shipColor[currDrawShipIdx];
			//random horizontal/vertical
			int shipX = -1; int shipY = -1;
			r = rand.nextInt(2);
			boolean overlap = false;
			if(r==0){
				whichShip = "horizontal";
				r = rand.nextInt(5);
				if(r!=0){
					if(quad==0){
						shipX = rand.nextInt(cellX/2-shipSize+1);
						shipY = rand.nextInt(cellY/2);
					}
					if(quad==1){
						shipX = rand.nextInt(cellX/2-shipSize+1)+cellX/2;
						shipY = rand.nextInt(cellY/2);
					}
					if(quad==2){
						shipY = rand.nextInt(cellY/2) + cellY/2;
						shipX = rand.nextInt(cellX/2-shipSize+1);
					}
					if(quad==3){
						shipX = rand.nextInt(cellX/2-shipSize+1)+cellX/2;
						shipY = rand.nextInt(cellY/2) + cellY/2;
					}
				}
				else{
					shipX = rand.nextInt(cellX-shipSize+1);
					shipY = rand.nextInt(cellY);
				}
				for(int i=0; i<shipSize; i++){
					if(myGrid[shipX+i][shipY]){ //overlap
						overlap = true; break;
					}
				}
				if(overlap) continue;
				for(int i=0; i<shipSize; i++){
					myGrid[shipX+i][shipY] = true;
				}
				drawShipXList.add(shipX);
				drawShipYList.add(shipY);
				drawShipWidthList.add(shipSize);
				drawShipHeightList.add(1);
			}
			else{
				whichShip = "vertical";
				if(r!=0){
					if(quad==0){
						shipX = rand.nextInt(cellX/2);
						shipY = rand.nextInt(cellY/2-shipSize+1);
					}
					if(quad==1){
						shipX = rand.nextInt(cellX/2)+cellX/2;
						shipY = rand.nextInt(cellY/2-shipSize+1);
					}
					if(quad==2){
						shipX = rand.nextInt(cellX/2);
						shipY = rand.nextInt(cellY/2-shipSize+1) + cellY/2;
					}
					if(quad==3){
						shipX = rand.nextInt(cellX/2)+cellX/2;
						shipY = rand.nextInt(cellY/2-shipSize+1) + cellY/2;
					}
				}
				else{
					shipX = rand.nextInt(cellX-shipSize+1);
					shipY = rand.nextInt(cellY);
				}
				for(int i=0; i<shipSize; i++){
					if(myGrid[shipX][shipY+i]){ //overlap
						overlap = true; break;
					}
				}
				if(overlap) continue;
				for(int i=0; i<shipSize; i++){
					myGrid[shipX][shipY+i] = true;
				}
				drawShipXList.add(shipX);
				drawShipYList.add(shipY);
				drawShipWidthList.add(1);
				drawShipHeightList.add(shipSize);
			}
			drawShipColor.add(shipColor);
			countShip++;
			currDrawShipCount++;
			if(Globals.shipNumber[currDrawShipIdx]==currDrawShipCount){
				currDrawShipCount = 0;
				currDrawShipIdx++;
			}

		}
		doneDrawShip();
	}
	
	public void nextMove(){ //random next move
		//random opponent
		r = rand.nextInt(oppGridList.size());
		int idx = 0;
		OppGrid og = null; String ogIdx = "";
		for(String i: oppGridList.keySet()){
			if(idx==r){
				og = oppGridList.get(i); ogIdx = i;
			}
			idx++;
		}
		//attack on OG
		int attackX = -1; int attackY = -1;
		int targetX = -1; int targetY = -1;
		boolean flag = false;
		ArrayList<OppGridCoord> unhitcoords = og.getUnHitCoordList();
		ArrayList<OppGridCoord> hitcoords = og.getHitCoordList();
		OppGridCoord coord = null;
		
		for(int i=0; i<hitcoords.size(); i++){
			if(!hitcoords.get(i).getConsidered()){
				flag = false;
				coord = hitcoords.get(i); 
				targetX = coord.getX(); targetY = coord.getY()-1; //above
				//System.out.print(og.checkIfAlreadyHit(targetX, targetY)+""+og.checkIfAlreadyMissed(targetX, targetY));
				if(!flag && !og.checkIfAlreadyHit(targetX, targetY) && !og.checkIfAlreadyMissed(targetX, targetY)){
					if(targetY>=0){
						attackX = targetX; attackY = targetY; flag = true;
					}
				}
				targetY = coord.getY()+1; //below
				//System.out.print(og.checkIfAlreadyHit(targetX, targetY)+""+og.checkIfAlreadyMissed(targetX, targetY));
				if(!flag && !og.checkIfAlreadyHit(targetX, targetY) && !og.checkIfAlreadyMissed(targetX, targetY)){
					if(targetY<cellY){
						attackX = targetX; attackY = targetY; flag = true;
					}
				}
				targetY = coord.getY(); targetX = coord.getX()-1; //left
				//System.out.print(og.checkIfAlreadyHit(targetX, targetY)+""+og.checkIfAlreadyMissed(targetX, targetY));
				if(!flag && !og.checkIfAlreadyHit(targetX, targetY) && !og.checkIfAlreadyMissed(targetX, targetY)){
					if(targetX>=0){
						attackX = targetX; attackY = targetY; flag = true;
					}
				}
				targetX = coord.getX()+1; //right
				//System.out.print(og.checkIfAlreadyHit(targetX, targetY)+""+og.checkIfAlreadyMissed(targetX, targetY));
				if(!flag && !og.checkIfAlreadyHit(targetX, targetY) && !og.checkIfAlreadyMissed(targetX, targetY)){
					if(targetX<cellX){
						attackX = targetX; attackY = targetY; flag = true;
					}
				}
				if(flag){
					break;
				}
				else{
					//System.out.println("considered: "+coord.getX()+" "+coord.getY());
					coord.setConsidered();
				}
			}
		}	
		if(!flag){
			r = rand.nextInt(unhitcoords.size());
			attackX = unhitcoords.get(r).getX(); attackY = unhitcoords.get(r).getY();
		}
		sendAttack(attackX, attackY, ogIdx);
		if(getChanceStatus() && getChanceMode()==1){ //chance to attack 4 directions
			if(attackX-1>=0){ //left
				sendAttack(attackX-1, attackY, ogIdx);
			}
			if(attackX+1<cellX){ //right
				sendAttack(attackX+1, attackY, ogIdx);
			}
			if(attackY-1>=0){ //above
				sendAttack(attackX, attackY-1, ogIdx);
			}
			if(attackY+1<cellY){ //below
				sendAttack(attackX, attackY+1, ogIdx);
			}
			chanceMode = -1;
			chanceTaken = true;
			chanceStatus = false;
		}
	}
}

class OppGrid{
	private String id;
	private int cellX;
	private int cellY = 10;
	private BattleshipAI ai;
	private boolean alive = true;
	private boolean attackMode = false;
	private ArrayList<Integer> hitXList, hitYList, missedXList, missedYList;
	private ArrayList<OppGridCoord> unHitCoords, HitCoords;
	public OppGrid(String id, int cell, BattleshipAI ai){
		this.id = id;
		this.ai = ai;
		this.cellX = cell;
		hitXList = new ArrayList<Integer>();
		hitYList = new ArrayList<Integer>();
		missedXList = new ArrayList<Integer>();
		missedYList = new ArrayList<Integer>();
		unHitCoords = new ArrayList<OppGridCoord>();
		HitCoords = new ArrayList<OppGridCoord>();
		for(int i=0; i<cellX; i++){
			for(int j=0; j<cellY; j++){
				unHitCoords.add(new OppGridCoord(i,j));
			}
		}
	}
	public void attackModeOn(){
		attackMode = true;
	}
	public void attackModeOff(){
		attackMode = false;
	}
	public void attackHit(int hitX, int hitY){
		hitXList.add(hitX);
		hitYList.add(hitY);
		for(int i=0; i<unHitCoords.size(); i++){
			if(unHitCoords.get(i).equal(hitX, hitY)){
				HitCoords.add(unHitCoords.get(i));
				unHitCoords.remove(i); 
				break;
			}
		}
	}
	public void attackMissed(int missedX, int missedY){
		missedXList.add(missedX);
		missedYList.add(missedY);
		for(int i=0; i<unHitCoords.size(); i++){
			if(unHitCoords.get(i).equal(missedX, missedY)){
				unHitCoords.remove(i); break;
			}
		}
	}
	public void dead(){
		alive = false;
	}
	public boolean checkIfAlreadyHit(int x, int y){
		if(hitXList.contains(x) && hitYList.contains(y)) return true;
		return false;
	}
	public boolean checkIfAlreadyMissed(int x, int y){
		if(missedXList.contains(x) && missedYList.contains(y)) return true;
		return false;
	}
	public ArrayList<OppGridCoord> getUnHitCoordList(){
		return unHitCoords;
	}
	public ArrayList<OppGridCoord> getHitCoordList(){
		return HitCoords;
	}
}

class OppGridCoord{
	private int x, y;
	private boolean considered = false;
	public OppGridCoord(int x, int y){
		this.x = x; this.y = y;
	}
	public int getX(){
		return x;
	}
	public int getY(){
		return y;
	}
	public boolean equal(int x, int y){
		if(this.x == x && this.y == y) return true;
		return false;
	}
	public void setConsidered(){
		considered = true;
	}
	public boolean getConsidered(){
		return considered;
	}
}
