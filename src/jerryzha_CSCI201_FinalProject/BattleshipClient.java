package chauvu_CSCI201_FinalProject;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.TimerTask;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.awt.*;

import javax.swing.*;
import javax.swing.border.LineBorder;



class countTask extends TimerTask{
	JLabel label;
	BattleshipClient client;
	public countTask(JLabel lab, BattleshipClient bc){
		label = lab;
		client = bc;
	}
	public void run(){
		Integer curr = Integer.parseInt(label.getText()) - 1;
		label.setText(curr.toString());
		if(curr == 0){
			if(client.getGameOn()){
				return;
			}else{
				client.defaultStartGame();
			}
		}
	}
}

class Counter extends Thread{
	JLabel label;
	Integer currT;
	Timer t;
	BattleshipClient client;
	
	public Counter(JLabel l, BattleshipClient bc){
		label = l;
		client = bc;
		currT = Integer.parseInt(l.getText());
		t = new Timer();
	}
	
	public void run(){
		for(int i = 1;  i <= currT; i++){
			countTask ct = new countTask(label, client);
			t.schedule(ct, 1000*i);
		}
	}
}

class BGPanelLogin extends JPanel{
	private BattleshipClient bc;
	public BGPanelLogin(BattleshipClient bc){
		super();
		this.bc = bc;
	}
	public void paintComponent(Graphics g){
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		Image img = Toolkit.getDefaultToolkit().getImage("Image/background.jpg");
		g2.drawImage(img, 0, 0, this);
		g2.finalize();
	}
}

class BGPanelGame extends JPanel{
	public BGPanelGame(){
		super();
	}
	public void paintComponent(Graphics g){
		super.paintComponent(g);
		g.setColor(Color.white);
		g.fillRect(0, 0, getPreferredSize().width, getPreferredSize().width);
	}
}

public class BattleshipClient extends Thread{
	//IO
	private Socket s, s2;
	private PrintWriter pw, pw2;
	private BufferedReader br, br2;
	private Scanner scan;
	private Random rand;
	
	//DATA
	private String hostname;
	private String username;
	private boolean newgame;
	private boolean loggedin = false;
	private int numberPlayers, numberAIs, numberCells, level;
	private String group;
	private int size = 50;
	private int cellX;
	private int cellY = 10;
	private String mode; //attack or defense
	private String myID;
	private String[] oppID;
	private int chanceMode = -1;
	private int consecutive = -1;
	private boolean dead = false;
	private boolean isRunning = true;
	private boolean gameOn;
	private String[] numPlayersList;
	private String[] boardSizeList;
	private String[] numAIsList;
	private String[] levelList;
	private int money = 0;
	private boolean yield = false;
	private Point currPoint;
	private String currOpp;
	
	//GUI COMPONENTS
	private JFrame myFrame;
	private myBoard myPanel;
	private JPanel cardPanel;
	private CardLayout cardLayout;
	private JTabbedPane tabbedPane;
	private BGPanelGame mainPanel;
	private BGPanelLogin loginPanel, startPanel, newGamePanel, joinGamePanel, waitPanel;
	private JLabel myStatus, Timer, timerMsg;
	private HashMap<String, oppBoard> oppPanelList = new HashMap<String, oppBoard>();
	private JFrame menuFrame;
	private JTextField usernameText, passwordText;
	private JPanel groupList, sidePanel, chatPanel;
	private JComboBox<String> numPlayers;
	private JComboBox<String> boardSize;
	private JComboBox<String> numAI;
	private JComboBox<String> gameLevels;
	private ArrayList<JLabel> groupLabels;
	private Counter count;
	private JLabel moneyLabel;
	private JMenuItem yieldTurn;
	private JMenuBar menubar;
	private JMenu menu, power;
	private JMenuItem power1, power2, power3, power4, quit;
	private ChatWindow chatwindow;
	
	//final static
	final static String LOGIN = "LOGIN";
	final static String STARTGAME = "STARTGAME";
	final static String NEWGAME = "NEWGAME";
	final static String JOINGAME = "JOINGAME";
	final static String WAIT = "WAIT";
	
	//locks
	private Lock lock = new ReentrantLock();
	private Condition condition = lock.newCondition();

	
	public BattleshipClient(String hostname, int port){
		super();
		this.hostname = hostname;
		scan = new Scanner(System.in);
		rand = new Random();
		try{
			s = new Socket(hostname, port);
			pw = new PrintWriter(s.getOutputStream());
			br = new BufferedReader(new InputStreamReader(s.getInputStream()));
		}
		catch(IOException ioe){
			System.out.println("ioe in client: "+ioe.getMessage());
		}
		initializeGUIComponents();
		loginGUI();
	}
	
	public void initializeGUIComponents(){
		menuFrame = new JFrame("Start Battleship");
		menuFrame.setSize(500, 300);
		menuFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		cardLayout = new CardLayout();
		cardPanel = new JPanel(cardLayout);
		menuFrame.add(cardPanel);
		loginPanel = new BGPanelLogin(this);
		usernameText = new JTextField("Username", 15);
		passwordText = new JTextField("Password", 15);
		cardPanel.add(loginPanel, LOGIN);
		startPanel = new BGPanelLogin(this);
		startPanel.setLayout(null);
		cardPanel.add(startPanel, STARTGAME);
		newGamePanel = new BGPanelLogin(this);
		cardPanel.add(newGamePanel, NEWGAME);
		joinGamePanel = new BGPanelLogin(this);
		cardPanel.add(joinGamePanel, JOINGAME);
		groupList = new JPanel();
		waitPanel = new BGPanelLogin(this);
		cardPanel.add(waitPanel,WAIT);
		timerMsg = new JLabel("Finish setting within: ");
		Timer = new JLabel("20");
		count = new Counter(Timer, this);
		Timer.setBounds(450, 70, 200, 90);
		Timer.setForeground(Color.white);
		timerMsg.setBounds(350, 50, 200, 70);
		timerMsg.setForeground(Color.white);
		menubar = new JMenuBar();
		menu = new JMenu("Menu");
		power = new JMenu("Powers");
		chatPanel = new JPanel();
	}

	public void loginGUI(){
		gameOn = false;
		cardLayout.show(cardPanel, LOGIN);
		loginPanel.setLayout(null);
		usernameText.setPreferredSize(usernameText.getPreferredSize());
		usernameText.setBounds(40, 80, usernameText.getPreferredSize().width, usernameText.getPreferredSize().height);
		passwordText.setPreferredSize(passwordText.getPreferredSize());
		passwordText.setBounds(40, 150, passwordText.getPreferredSize().width, passwordText.getPreferredSize().height);
		JLabel usernameLabel = new JLabel("Username:");
		usernameLabel.setForeground(Color.white);
		usernameLabel.setPreferredSize(usernameLabel.getPreferredSize());
		usernameLabel.setBounds(40,60,usernameLabel.getPreferredSize().width, usernameLabel.getPreferredSize().height);
		JLabel passwordLabel = new JLabel("Password:");
		passwordLabel.setForeground(Color.white);
		passwordLabel.setPreferredSize(passwordLabel.getPreferredSize());
		passwordLabel.setBounds(40,130,passwordLabel.getPreferredSize().width, passwordLabel.getPreferredSize().height);
		JButton login = new JButton("Log In");
		login.setBounds(40, 200, login.getPreferredSize().width, login.getPreferredSize().height);
		login.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				username = usernameText.getText();
				String password = passwordText.getText();
				pw.println("startup login "+username+" "+password);
				pw.flush();
				String line = ""; String parts[] = null;
				try{
					line = br.readLine();
					parts = line.split(" ");
					if(parts[0].equals("startup")){
						if(parts[1].equals("approved")){
							startChatting();
							startOrJoinGame();
							loggedin = true;
						}
						else{
							JOptionPane.showMessageDialog(myFrame, parts[2], "Warning", JOptionPane.ERROR_MESSAGE);
						}
					}
					else{
						System.out.println("STARTUP ERROR");
					}
				}
				catch(IOException ioe){
					System.out.println("ioe: "+ioe.getMessage());
				}
			}
		});
		JButton signup = new JButton("Sign Up");
		signup.setBounds(150, 200, signup.getPreferredSize().width, signup.getPreferredSize().height);
		signup.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae) {
				username = usernameText.getText();
				String password = passwordText.getText();
				pw.println("startup signup "+username+" "+password);
				pw.flush();
				String line = ""; String parts[] = null;
				try {
					line = br.readLine();
					parts = line.split(" ");
					if(parts[0].equals("startup")){
						if(parts[1].equals("approved")){
							startChatting();
							startOrJoinGame();
							loggedin = true;
						}
						else{
							JOptionPane.showMessageDialog(myFrame, parts[2], "Warning", JOptionPane.ERROR_MESSAGE);
						}
					}
					else{
						System.out.println("STARTUP ERROR");
					}
				}
				catch(IOException ioe){
					System.out.println("ioe: "+ioe.getMessage());
				}
			}
		});
		loginPanel.add(usernameText);
		loginPanel.add(passwordText);
		loginPanel.add(usernameLabel);
		loginPanel.add(passwordLabel);
		loginPanel.add(login);
		loginPanel.add(signup);
		menuFrame.setVisible(true);
	}
	
	private void startChatting() {
		try{
			s2 = new Socket(hostname, 6789);
			pw2 = new PrintWriter(s2.getOutputStream());
			br2 = new BufferedReader(new InputStreamReader(s2.getInputStream()));
			pw2.println(username); pw2.flush();
		}
		catch(IOException ioe){
			System.out.println("ioe in client: "+ioe.getMessage());
		}
		chatwindow = new ChatWindow(s2, username);
		ReceiveChatThread rcThread = new ReceiveChatThread();
		rcThread.start();
	}
	
	class ReceiveChatThread extends Thread {
		private String username;
		public void run() {
			try {
				username = br2.readLine();
				while (true) {
					String line = br2.readLine();
					String user = line.substring(0,line.indexOf(' '));
					String msg = line.substring(line.indexOf(' ')+1);
					chatwindow.receiveMessage(user, msg);
				}
			} catch (IOException ioe) {
				System.out.println("IOException in Client: " + ioe.getMessage());
			}
		}
	}
	
	public void startOrJoinGame(){
		count.start();
		startPanel.setLayout(null);
		JButton newGame = new JButton("Start New Game");
		newGame.setBounds(40,100, newGame.getPreferredSize().width, newGame.getPreferredSize().height);
		newGame.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				newgame = true;
				newGameDetailsGUI();
			}
		});
		JButton joinGame = new JButton("Join Existing Game");
		joinGame.setBounds(40,150, joinGame.getPreferredSize().width, joinGame.getPreferredSize().height);
		joinGame.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				newgame = false;
				joinGameDetailsGUI();
			}
		});
		startPanel.add(newGame);
		startPanel.add(joinGame);
		startPanel.add(timerMsg);
		startPanel.add(Timer);
		cardLayout.show(cardPanel, STARTGAME);
	}
	
	public void newGameDetailsGUI(){
		cardLayout.show(cardPanel, NEWGAME);
		newGamePanel.setLayout(null);
		
		numAI = new JComboBox<String>();
		numAI.setBounds(40, 100, numAI.getPreferredSize().width+3, numAI.getPreferredSize().height+3);
		JLabel numAILabel = new JLabel("Choose number of AI's: ");
		numAILabel.setBounds(40, 80, numAILabel.getPreferredSize().width, numAILabel.getPreferredSize().height);
		numPlayersList = new String[]{"2", "3", "4"};
		numPlayers = new JComboBox<String>(numPlayersList);
		numPlayers.setBounds(40, 50, numPlayers.getPreferredSize().width, numPlayers.getPreferredSize().height);
		numPlayers.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				numAIsList = new String[Integer.parseInt(numPlayersList[numPlayers.getSelectedIndex()])];
				for(int i=0; i<numAIsList.length; i++){
					numAIsList[i] = Integer.toString(i);
				}
				numAI.setModel(new DefaultComboBoxModel<String>(numAIsList));
				numAI.setSelectedIndex(0);
			}
		});
		numPlayers.setSelectedIndex(0);
		JLabel playerLabel = new JLabel("Choose number of opponents :  ");
		playerLabel.setBounds(40, 30, playerLabel.getPreferredSize().width, playerLabel.getPreferredSize().height);
		boardSizeList = new String[]{"10x10", "15x10", "20x10"};
		final int[] boardSizeInt = new int[]{10, 15, 20};
		boardSize = new JComboBox<String>(boardSizeList);
		boardSize.setBounds(40, 150, boardSize.getPreferredSize().width, boardSize.getPreferredSize().height);
		boardSize.setSelectedIndex(0);
		JLabel levelLabel = new JLabel("Choose game level:  ");
		levelLabel.setBounds(40, 170, levelLabel.getPreferredSize().width, levelLabel.getPreferredSize().height);
		levelList = new String[]{"Easy", "Medium", "Difficult"};
		gameLevels = new JComboBox<String>(levelList);
		gameLevels.setBounds(40, 190, gameLevels.getPreferredSize().width, gameLevels.getPreferredSize().height);
		gameLevels.setSelectedIndex(0);
		JLabel boardLabel = new JLabel("Choose size of the board :  ");
		boardLabel.setBounds(40, 130, boardLabel.getPreferredSize().width, boardLabel.getPreferredSize().height);
		JButton startGame = new JButton("Start Game");
		startGame.setBounds(40, 230, startGame.getPreferredSize().width, startGame.getPreferredSize().height);
		startGame.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				numberPlayers = Integer.parseInt((String) numPlayers.getSelectedItem());
				numberAIs = Integer.parseInt((String) numAI.getSelectedItem());
				numberCells = boardSizeInt[boardSize.getSelectedIndex()];
				cellX = numberCells;
				level = gameLevels.getSelectedIndex();
				pw.println("login start "+username+" "+numberPlayers+" "+numberAIs+" "+numberCells+" "+level);
				pw.flush();
				start();
				gameOn = true;
				waitingGUI();
			}
		});
		newGamePanel.add(numAILabel);
		newGamePanel.add(numAI);
		newGamePanel.add(numPlayers);
		newGamePanel.add(playerLabel);
		newGamePanel.add(boardLabel);
		newGamePanel.add(boardSize);
		newGamePanel.add(levelLabel);
		newGamePanel.add(gameLevels);
		newGamePanel.add(startGame);
		newGamePanel.add(timerMsg);
		newGamePanel.add(Timer);
	}
	
	public void joinGameDetailsGUI(){
		cardLayout.show(cardPanel, JOINGAME);
		joinGamePanel.setLayout(null);
		JLabel chooseGroup = new JLabel("Choose Group to Join: ");
		chooseGroup.setForeground(Color.white);
		chooseGroup.setBounds(40, 70, chooseGroup.getPreferredSize().width, chooseGroup.getPreferredSize().height);
		groupList.setLayout(new BoxLayout(groupList, BoxLayout.Y_AXIS));
		pw.println("login groups"); pw.flush();
		groupLabels = new ArrayList<JLabel>();
		try{
			String line = br.readLine();
			String[] parts = line.split(" ");
			if(parts[0].equals("groups")){
				for(int i=1; i<parts.length; i++){
					JLabel l = new JLabel(parts[i]);
					l.setPreferredSize(new Dimension(l.getPreferredSize().width, l.getPreferredSize().height));
					l.setOpaque(true);
					groupLabels.add(l);
					l.addMouseListener(new MouseAdapter(){
						public void mouseClicked(MouseEvent me){
							for(int j=0; j<groupLabels.size(); j++){
								String ls = groupLabels.get(j).getText();
								if(ls.contains("✔")){
									ls = ls.substring(0, ls.indexOf("✔")-1);
									groupLabels.get(j).setText(ls);
								}
							}
							String ls = ((JLabel) me.getSource()).getText();
							if(ls.contains("✔")){
								ls = ls.substring(0, ls.indexOf("✔")-1);
							}
							group = ls;
							((JLabel) me.getSource()).setText(((JLabel) me.getSource()).getText()+" ✔");
						}
					});
					groupList.add(l);
					groupList.add(Box.createRigidArea(new Dimension(100,10)));
				}
				groupList.add(Box.createGlue());
				group = groupLabels.get(0).getText();
			}
			JButton startGame2 = new JButton("Start Game");
			startGame2.setBounds(50, 220, startGame2.getPreferredSize().width, startGame2.getPreferredSize().height);
			startGame2.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent ae){
					pw.println("login join "+group+" "+username);
					pw.flush();
					try{
						String line = br.readLine();
						String[] parts = line.split(" ");
						if(parts[0].equals("cells")){
							cellX = Integer.parseInt(parts[1]);
							level = Integer.parseInt(parts[2]);
						}
					}
					catch(IOException ioe){
						System.out.println("ioe: "+ioe.getMessage());
					}
					start();
					gameOn = true;
					waitingGUI();
				}
			});
			JScrollPane scr = new JScrollPane(groupList);
			scr.setBounds(40, 100, 150, 100);
			scr.setOpaque(true);
			scr.setBackground(Color.white);
			joinGamePanel.add(chooseGroup);
			joinGamePanel.add(scr);
			joinGamePanel.add(startGame2);
			joinGamePanel.add(timerMsg);
			joinGamePanel.add(Timer);
		}
		catch(IOException ioe){
			System.out.println("ioe: "+ioe.getMessage());
		}
	}
	
	public void waitingGUI(){
		JLabel wait = new JLabel("WAITING FOR OPPONENTS");
		wait.setForeground(Color.white);
		wait.setBounds(50, 100, 200, 200);
		waitPanel.add(wait);
		cardLayout.show(cardPanel, WAIT);
	}
	
	public void defaultStartGame(){
		//start 2-player game with 1 AI
		pw.println("login start "+username+" 2 1 10 0");
		numberPlayers = 2;
		numberAIs = 1;
		numberCells = 10;
		cellX = 10;
		pw.flush();
		start();
	}
	
	public void createMyGUI(){
		final BattleshipClient bc = this;
		myFrame = new JFrame("Battleship Game");
		myFrame.setSize(size*cellX+300, size*cellY+300);
		myFrame.setMaximumSize(new Dimension(size*cellX+300, size*cellY+300));
		myFrame.setMinimumSize(new Dimension(size*cellX+300, size*cellY+300));
		myFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		tabbedPane = new JTabbedPane();
		sidePanel = new JPanel();
		sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
		myStatus = new JLabel("Status: Place Ships");
		quit = new JMenuItem("Forfeit Game");
		quit.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				SwingUtilities.invokeLater(new Runnable() {
					public void run () {
						forfeitGame();
					}
				});
			}
		});
		yieldTurn = new JMenuItem("Yield Turn");
		yieldTurn.setToolTipText("Yield turn and earn random amount of money");
		yieldTurn.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				noAttack();
				money += rand.nextInt(5);
				moneyLabel.setText("Money: "+Integer.toString(money));
			}
		});
		moneyLabel = new JLabel("Money: "+Integer.toString(money));
		power1 = new JMenuItem("Power 1");
		power1.setToolTipText("Attack 5 spots at once. costs 12 money");
		power1.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				Power1();
			}
		});
		power2 = new JMenuItem("Power 2");
		power2.setToolTipText("Attack twice. costs 10 money");
		power2.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				Power2();
			}
		});
		power3 = new JMenuItem("Power 3");
		power3.setToolTipText("Blocks next attack. costs 6 money");
		power3.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				Power3();
			}
		});
		
		power4 = new JMenuItem("Power 4");
		power4.setToolTipText("Reflects next turn. costs 8 money");
		power4.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				Power4();
			}
		});
		mainPanel = new BGPanelGame();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
		myPanel = new myBoard(size, cellX, "Redekopp", this);
		tabbedPane.addTab(myID+" Board", myPanel);
		mainPanel.add(tabbedPane);
		mainPanel.add(chatwindow);
		sidePanel.add(myStatus);
		sidePanel.add(moneyLabel);
		power.add(yieldTurn);
		power.add(power1);
		power.add(power2);
		power.add(power3);
		power.add(power4);
		menubar.add(menu);
		menubar.add(power);
		menu.add(quit);
		myFrame.setJMenuBar(menubar);
		myFrame.add(mainPanel);
		myFrame.add(sidePanel, BorderLayout.NORTH);
		myFrame.setVisible(true);
		menuFrame.setVisible(false);
	}
	
	public void createOppGUI(String oppid){
		oppBoard panel = new oppBoard(size, cellX, oppid, this);
		tabbedPane.addTab(oppid+" Board", panel);
		oppPanelList.put(oppid,panel);
	}
	
	public boolean getGameOn() {
		return gameOn;
	}
	
	public void doneDrawShip(){
		setStatus("Waiting for Opponents");
		pw.println("drawdone");
		pw.flush();
	}
	
	public void setStatus(String s){
		myStatus.setText("Status: "+s);
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
			//create GUI
			createMyGUI();
			for(int i=0; i<oppID.length; i++){
				createOppGUI(oppID[i]);
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
					if(mode.equals("attack")){
//						yieldTurn.setEnabled(true);
						for(String i: oppPanelList.keySet()) oppPanelList.get(i).attackModeOn();
						setStatus("Attack");
						line = br.readLine();
						parts = line.split(" ");
						if(parts[0].equals("win")){ //winner
							for(String i: oppPanelList.keySet()) oppPanelList.get(i).dead();
							setStatus("Win");
							return;
						}
						if(parts[0].equals("dead")){
							oppPanelList.get(parts[1]).dead();
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
								if(myPanel.checkAttack(x,y)){
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
							oppPanelList.get(parts[3]).attackMissed(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
							line = br.readLine();
							parts = line.split(" ");
						}
						else if(parts[0].equals("hit")){//hit
							oppPanelList.get(parts[3]).attackHit(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
							line = br.readLine();
							parts = line.split(" ");
						}
						if(parts[0].equals("mode")){
							if(parts[1].equals("attack")) mode = "attack";
							else if(parts[1].equals("defense")) mode = "defense";
						}
					}
					else if(mode.equals("defense")){
//						yieldTurn.setEnabled(false);
						for(String i: oppPanelList.keySet()) oppPanelList.get(i).attackModeOff();
						setStatus("Defense");
						line = br.readLine();
						parts = line.split(" ");
						if(parts[0].equals("attack")){
							int x = Integer.parseInt(parts[1]);
							int y = Integer.parseInt(parts[2]);
							String attacker = parts[3];
							if(chanceMode==3){ //block current attack
								pw.println("defense block "+attacker); pw.flush();
								disableChance();
							}
							else if(chanceMode==4){ //reflect current attack
								pw.println("defense reflect "+attacker); pw.flush();
								sendAttack(x, y, attacker);
								line = br.readLine();
								parts = line.split(" ");
								if(parts[0].equals("win")){ //winner
									for(String i: oppPanelList.keySet()) oppPanelList.get(i).dead();
									setStatus("Win");
									return;
								}
								if(parts[0].equals("dead")) oppPanelList.get(parts[1]).dead();
								else if(parts[0].equals("missed")) oppPanelList.get(parts[3]).attackMissed(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
								else if(parts[0].equals("hit")) oppPanelList.get(parts[3]).attackHit(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
								disableChance();
							}
							else if(myPanel.checkAttack(x,y)){
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
							oppPanelList.get(parts[1]).dead();
						}
						line = br.readLine();
						parts = line.split(" ");
						if(parts[0].equals("dead")){
							oppPanelList.get(parts[1]).dead();
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
	
	public void setCurrAttack(Point p, String s){
		currPoint = p;
		currOpp = s;
	}
	
	public void wonMinigame(){
		for(String i: oppPanelList.keySet()){
			if(i.equals(currOpp)){
				oppPanelList.get(i).sendAttack(currPoint);
				return;
			}
		}
	}
	
	public void sendAttack(int hitX, int hitY, String victim){
		if(chanceMode==2){
			pw.println("attack "+hitX+" "+hitY+" "+victim+" consecutive 1"); //2: stop consecutive
			pw.flush();
			disableChance();
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
				disableChance();
			}
		}
		else{
			pw.println("attack "+hitX+" "+hitY+" "+victim);
			pw.flush();
		}
	}
	
	public void noAttack(){
		yield = true;
		pw.println("attack none"); pw.flush();
	}
	
	public void deadStatus(){
		dead = true; isRunning = false;
		for(String i: oppPanelList.keySet()) oppPanelList.get(i).playerDead();
		setStatus("Dead");
	}
	
	public void Power1(){//attack 5 spots at once: center, north, south, east, west
		//money needed: 12
		if(money>=12){
			chanceMode = 1;
			money -= 12;
			moneyLabel.setText("Money: "+Integer.toString(money));
		}
		else{
			System.out.println("You do not have enough money to use this power");
		}
	}
	public void Power2(){//2 consecutive attack turns
		//money needed: 10
		if(money>=10){
			chanceMode = 2;
			money -= 10;
			moneyLabel.setText("Money: "+Integer.toString(money));
		}
		else{
			System.out.println("You do not have enough money to use this power");
		}
	}
	public void Power3(){//block next attack
		//money needed: 6
		if(money>=6){
			chanceMode = 3;
			money -= 6;
			moneyLabel.setText("Money: "+Integer.toString(money));
		}
		else{
			System.out.println("You do not have enough money to use this power");
		}
	}
	public void Power4(){//reflect next attack
		//money needed: 8
		if(money>=8){
			chanceMode = 4;
			money -= 8;
			moneyLabel.setText("Money: "+Integer.toString(money));
		}
		else{
			System.out.println("You do not have enough money to use this power");
		}
	}
	
	public int getChanceMode(){
		return chanceMode;
	}
	
	public void disableChance(){
		chanceMode = -1;
	}
	
	public void setConsecutive(int c){
		consecutive = c;
	}
	
	public int getConsecutive(){
		return consecutive;
	}
	
	public int getLevel(){
		return level;
	}
	
	public void forfeitGame(){
		if(dead){
			pw.println("quit lose"); 
			pw.flush();
		}
		else if(myStatus.getText().contains("Win")){
			pw.println("quit win");
			pw.flush();
		}
		else{
			pw.println("forfeit");
			pw.flush();
		}
		isRunning = false;
		System.exit(0);
	}
	
	public static void main(String[] args){
		new Globals();
		new BattleshipClient("localhost", 9641);
	}
}

class myBoard extends JPanel{
	private BattleshipClient bc;
	private Point MousePt;
	private String whichShip;
	private int countShip = 0;
	private int size;
	private int cellY = 10;
	private int cellX;
	private boolean isAlive = true;
	private Integer drawHorizontalShipX, drawHorizontalShipY, drawVerticalShipX, drawVerticalShipY;
	private Integer drawHorizontalShipWidth, drawHorizontalShipHeight, drawVerticalShipWidth, drawVerticalShipHeight;
	private ArrayList<Integer> drawShipXList, drawShipYList;
	private ArrayList<Integer> drawShipWidthList, drawShipHeightList;
	private ArrayList<Integer> drawHitX, drawHitY;
	private ArrayList<Integer> drawMissX, drawMissY;
	private ArrayList<Color> drawShipColor;
	private boolean[][] gridShip;
	
	//current ship
	private int currDrawShipCount, currDrawShipIdx, shipSize;
	private Color shipColor;
	private String fleetType;
	private boolean drawShip;
	private boolean insideShip;
	private String status;
	

	public myBoard(int s, int c, String f, BattleshipClient bc){
		super();
		this.bc = bc;
		size = s;
		cellX = c;
		fleetType = f;
		status = "defense";
		whichShip = "";
		currDrawShipIdx = 0;
		currDrawShipCount = 0;
		drawShip = true;
		gridShip = new boolean[cellX][cellY];
		for(int i=0; i<cellX; i++){
			for(int j=0; j<cellY; j++){
				gridShip[i][j] = false;
			}
		}
		setPreferredSize(new Dimension(size*cellX,size*cellY+250));
		
		drawShipXList = new ArrayList<Integer>();
		drawShipYList = new ArrayList<Integer>();
		drawShipWidthList = new ArrayList<Integer>();
		drawShipHeightList = new ArrayList<Integer>();
		drawHitX = new ArrayList<Integer>();
		drawHitY = new ArrayList<Integer>();
		drawMissX = new ArrayList<Integer>();
		drawMissY = new ArrayList<Integer>();
		drawShipColor = new ArrayList<Color>();
	
		drawShip();
		
		//mouse listener
		addMouseListener(new MouseAdapter(){
			public void mousePressed(MouseEvent me){
				MousePt = me.getPoint();
                if(MousePt.getX()>drawHorizontalShipX && MousePt.getX()<(drawHorizontalShipX+drawHorizontalShipWidth)){
                	if(drawShip){
                		if(MousePt.getY()>drawHorizontalShipY && MousePt.getY()<(drawHorizontalShipY+drawHorizontalShipHeight)){
                    		whichShip = "horizontal"; insideShip = true;
                    		repaint();
                    	}
                	}
                }
                if(MousePt.getX()>drawVerticalShipX && MousePt.getX()<(drawVerticalShipX+drawVerticalShipWidth)){
                	if(drawShip){
                		if(MousePt.getY()>drawVerticalShipY && MousePt.getY()<(drawVerticalShipY+drawVerticalShipHeight)){
                    		whichShip = "vertical"; insideShip = true;
                    		repaint();
                    	}
                	}
                }
			}
			public void mouseReleased(MouseEvent me){
				MousePt = me.getPoint();
				boolean inside = false;
				if(!insideShip) return;
				if(MousePt.getX()<size*cellX && MousePt.getY()<size*cellY){
					if(drawShip){
						inside = true;
						if(whichShip.equals("horizontal")){
							drawHorizontalShipX = (drawHorizontalShipX/size)*size;
							drawHorizontalShipY = (drawHorizontalShipY/size)*size;
						}
						if(whichShip.equals("vertical")){
							drawVerticalShipX = (drawVerticalShipX/size)*size;
							drawVerticalShipY = (drawVerticalShipY/size)*size;
						}
					}
				}
				if(drawShip && inside){
					addShip();
					drawShip();
					whichShip = "";
					repaint();
				}
			}
		});
		addMouseMotionListener(new MouseMotionAdapter(){
			public void mouseDragged(MouseEvent me){
				int dx = me.getX() - MousePt.x;
                int dy = me.getY() - MousePt.y;
                if(whichShip.equals("horizontal")){
                	drawHorizontalShipX += dx;
                	drawHorizontalShipY += dy;
                }
                if(whichShip.equals("vertical")){
                	drawVerticalShipX += dx;
                	drawVerticalShipY += dy;
                }
                MousePt = me.getPoint();
                repaint();
			}
		});
	}
	
	public void drawShip(){
		if(!drawShip) return;
		if(currDrawShipIdx>=(Globals.shipTypeNum)){
			drawShip = false; 
			bc.doneDrawShip();
			return;
		}
		shipSize = Globals.shipSize[currDrawShipIdx];
		shipColor = Globals.shipColor[currDrawShipIdx];
		currDrawShipCount++;
		
		whichShip = "";
		drawHorizontalShipWidth = new Integer((int)((shipSize)*size));
		drawHorizontalShipHeight = new Integer(size);
		drawHorizontalShipX = new Integer((int)(size*cellX/6));
		drawHorizontalShipY = new Integer(size*cellY+size);
		drawVerticalShipHeight = new Integer((int)((shipSize)*size));
		drawVerticalShipWidth = new Integer(size);
		drawVerticalShipX = new Integer((int)(size*cellX*3/4));
		drawVerticalShipY = new Integer(size*cellY+10);
		repaint(); revalidate();
		
		if(Globals.shipNumber[currDrawShipIdx]==currDrawShipCount){
			currDrawShipCount = 0;
			currDrawShipIdx++;
		}
	}
	
	public boolean addShip(){	
		if(whichShip.equals("horizontal")){
			for(int i=0; i<shipSize; i++){
				if(gridShip[drawHorizontalShipX/size+i][drawHorizontalShipY/size]){
					currDrawShipCount--;
					if(currDrawShipCount<0){
						currDrawShipIdx--; currDrawShipCount = Globals.shipNumber[currDrawShipIdx]-1;
					}
					return false;
				}
			}
			for(int i=0; i<shipSize; i++){
				gridShip[drawHorizontalShipX/size+i][drawHorizontalShipY/size] = true;
			}
			drawShipXList.add(drawHorizontalShipX);
			drawShipYList.add(drawHorizontalShipY);
			drawShipWidthList.add(drawHorizontalShipWidth);
			drawShipHeightList.add(drawHorizontalShipHeight);
		}
		if(whichShip.equals("vertical")){
			for(int i=0; i<shipSize; i++){
				if(gridShip[drawVerticalShipX/size][drawVerticalShipY/size+i]){
					currDrawShipCount--;
					if(currDrawShipCount<0){
						currDrawShipIdx--; currDrawShipCount = Globals.shipNumber[currDrawShipIdx]-1;
					}
					return false;
				}
			}
			for(int i=0; i<shipSize; i++){
				gridShip[drawVerticalShipX/size][drawVerticalShipY/size+i] = true;
			}
			drawShipXList.add(drawVerticalShipX);
			drawShipYList.add(drawVerticalShipY);
			drawShipWidthList.add(drawVerticalShipWidth);
			drawShipHeightList.add(drawVerticalShipHeight);
		}
		drawShipColor.add(shipColor);
		countShip++;
		return true;
	}
	
	public boolean checkAttack(int x, int y){
		if(gridShip[x][y]){
			//mark the hit
			drawHitX.add(new Integer(x));
			drawHitY.add(new Integer(y));
			gridShip[x][y] = false;
			//check if any more piece of ship still on board
			boolean alive = false;
			for(int i=0; i<cellX; i++){
				for(int j=0; j<cellY; j++){
					if(gridShip[i][j]){
						alive = true; break;
					}
				}
				if(alive) break;
			}
			if(!alive){
				isAlive = false;
				//process loser
				bc.deadStatus();
			}
			repaint(); revalidate();
			return true;
		}	
		else{
			drawMissX.add(new Integer(x));
			drawMissY.add(new Integer(y));
			return false;
		}
	}
	
	public void paintComponent(Graphics g){
		super.paintComponent(g);
		if(!isAlive){
			g.drawLine(0, 0, size*cellX, size*cellY);
			g.drawLine(0, size*cellX, size*cellY, 0);
			return;
		}
		
		//paint gradient background
		int initialX = 0; int initialY = 0;
		int currX = initialX; int currY = initialY;
		int delta = size/20;
		for(int i=0; i<cellY*size; i=i+size){
			for(int j=0; j<cellX*size; j=j+size){
				Graphics2D g2 = (Graphics2D) g;
				Image img = Toolkit.getDefaultToolkit().getImage("Image/tile_small.png");
				g2.drawImage(img, initialX+ j, initialY + i, this);
				g2.finalize();
				currX += delta; currY += delta;
			}
		}
		for(int i=0; i<cellY*size+1; i=i+size){
			g.setColor(new Color(240,240,255));
			g.drawLine(0, i, size*cellX, i);
		}
		for(int i=0; i<cellX*size+1; i=i+size){
			g.setColor(new Color(240,240,255));
			g.drawLine(i, 0, i, size*cellY);
		}

		//paint ship
		for(int i=0; i<drawShipXList.size(); i++){
			g.setColor(drawShipColor.get(i));
			Graphics2D g2 = (Graphics2D) g;
			String imgPath = null;
			if(drawShipWidthList.get(i) > drawShipHeightList.get(i)){
				imgPath = Globals.horizontalShipImagePaths.get(Globals.shipSize[i]);
			}else{
				imgPath = Globals.verticalShipImagePaths.get(Globals.shipSize[i]);
			}

			Image img = Toolkit.getDefaultToolkit().getImage(imgPath);
			g2.drawImage(img, drawShipXList.get(i), drawShipYList.get(i), this);
			g2.finalize();
		}
		if(drawShip){
			g.setColor(shipColor);
			Graphics2D g2 = (Graphics2D) g;
			
			String imgPath = Globals.horizontalShipImagePaths.get(Globals.shipSize[currDrawShipIdx - 1]);
			Image img = Toolkit.getDefaultToolkit().getImage(imgPath);
			g2.drawImage(img,drawHorizontalShipX, drawHorizontalShipY, this);
			g2.finalize();
			
			imgPath = Globals.verticalShipImagePaths.get(Globals.shipSize[currDrawShipIdx - 1]);
			img = Toolkit.getDefaultToolkit().getImage(imgPath);
			g2.drawImage(img, drawVerticalShipX , drawVerticalShipY, this);
			g2.finalize();
		}
		g.setColor(Color.black);
		
		//paint hits
		for(int i=0; i<drawHitX.size(); i++){
			Graphics2D g2 = (Graphics2D)g;
			Image img = Toolkit.getDefaultToolkit().getImage("Image/hit.png");
			g2.drawImage(img, drawHitX.get(i)*size, drawHitY.get(i)*size, this);
		}
		//paint misses
		for(int i=0; i<drawMissX.size(); i++){
			Graphics2D g2 = (Graphics2D)g;
			Image img = Toolkit.getDefaultToolkit().getImage("Image/miss.png");
			g2.drawImage(img, drawMissX.get(i)*size, drawMissY.get(i)*size, this);
		}
	}
}

class oppBoard extends JPanel{
	private Random rand;
	private int size;
	private int cellX;
	private int cellY = 10;
	private String id;
	private BattleshipClient bc;
	private ArrayList<Integer> drawHitXList, drawHitYList;
	private ArrayList<Integer> drawMissedXList, drawMissedYList;
	private Integer drawHitX, drawHitY;
	private boolean attackMode = false;
	private MouseAdapter ma;
	private boolean alive = true; //opponent in this board is alive
	private boolean isAlive = true; //player is alive
	private AtomicBoolean done, b;
	private Lock lock = new ReentrantLock();
	private Condition condition = lock.newCondition();
	
	public oppBoard(int s, int c, String Id, BattleshipClient Bc){
		rand = new Random();
		size = s;
		cellX = c;
		id = Id;
		bc = Bc;
		drawHitXList = new ArrayList<Integer>();
		drawHitYList = new ArrayList<Integer>();
		drawMissedXList = new ArrayList<Integer>();
		drawMissedYList = new ArrayList<Integer>();
		ma = new MouseAdapter(){
			public void mouseClicked(MouseEvent me){
				if(attackMode){
					bc.setCurrAttack(me.getPoint(),id);
					startMinigame();
				}
			}
		};
		addMouseListener(ma);
	}
	
	public void startMinigame(){
		done = new AtomicBoolean(false);
		b = new AtomicBoolean(false);
		int r = rand.nextInt(6);
		minigame m = null;
		if(r==0){
			m = new count(b,bc.getLevel(),done,bc,lock,condition);
		}
		else if(r==1){
			m = new targets(b,bc.getLevel(),done,bc,lock,condition);
		}
		else if(r==2){
			m = new clicks(b,bc.getLevel(),done,bc,lock,condition);
		}
		else if(r==3){
			m = new match(b,bc.getLevel(),done,bc,lock,condition);
		}
		else if(r==4){
			m = new maze(b,bc.getLevel(),done,bc,lock,condition);
		}
		else if(r==5){
			m = new pattern(b,bc.getLevel(),done,bc,lock,condition);
		}
		Thread t = new Thread(m);
		t.start();
	}
	
	public void paintComponent(Graphics g){
		/*chaaange*/ super.paintComponent(g);
		if(!alive || !isAlive){
			g.drawLine(0, 0, size*cellX, size*cellY);
			g.drawLine(0, size*cellY, size*cellX, 0);
			return;
		}
		//paint borders
		if(alive){
			for(int i=0; i<cellY*size+1; i=i+size){
				g.drawLine(0, i, size*cellX, i);
				
			}
			for(int i=0; i<cellX*size+1; i=i+size){
				g.drawLine(i, 0, i, size*cellY);
			}
			
			//draw hit
			g.setColor(Color.blue);
			for(int i=0; i<drawHitXList.size(); i++){
				/*chaaange*/Graphics2D g2 = (Graphics2D) g;
				/*chaaange*/Image img = Toolkit.getDefaultToolkit().getImage("Image/hit.png");
				/*chaaange*/g2.drawImage(img, drawHitXList.get(i)*size, drawHitYList.get(i)*size, this);
				/*chaaange*///g.fillRect(drawHitXList.get(i)*size, drawHitYList.get(i)*size, size, size);
			}
			//draw missed
			g.setColor(Color.red);
			for(int i=0; i<drawMissedXList.size(); i++){
				/*chaaange*/Graphics2D g2 = (Graphics2D) g;
				/*chaaange*/Image img = Toolkit.getDefaultToolkit().getImage("Image/miss.png");
				/*chaaange*/g2.drawImage(img, drawMissedXList.get(i)*size, drawMissedYList.get(i)*size, this);
				/*chaaange*///g.fillRect(drawMissedXList.get(i)*size, drawMissedYList.get(i)*size, size, size);
			}
		}
	}
	public void attackModeOn(){
		attackMode = true;
	}
	public void attackModeOff(){
		attackMode = false;
	}
	public void sendAttack(Point MousePt){
		drawHitX = new Integer((int) MousePt.getX()/size);
		drawHitY = new Integer((int) MousePt.getY()/size);
		if(bc.getChanceMode()==1){ //attack 4 cells next to this cell
			int count = 0;
			if(drawHitX-1>=0){ //left
				count++;
			}
			if(drawHitX+1<cellX){ //right
				count++;
			}
			if(drawHitY-1>=0){ //above
				count++;
			}
			if(drawHitY+1<cellY){ //below
				count++;
			}
			if(drawHitX-1>=0){ //left
				bc.setConsecutive(count--);
				bc.sendAttack(drawHitX-1, drawHitY, id);
			}
			if(drawHitX+1<cellX){ //right
				bc.setConsecutive(count--);
				bc.sendAttack(drawHitX+1, drawHitY, id);
			}
			if(drawHitY-1>=0){ //above
				bc.setConsecutive(count--);
				bc.sendAttack(drawHitX, drawHitY-1, id);
			}
			if(drawHitY+1<cellY){ //below
				bc.setConsecutive(count--);
				bc.sendAttack(drawHitX, drawHitY+1, id);
			}
			bc.disableChance();
		}
		bc.sendAttack(drawHitX, drawHitY, id);
		attackMode = false;
	}
	public void noAttack(){
		bc.noAttack();
	}
	public void attackHit(int hitX, int hitY){
		drawHitXList.add(hitX);
		drawHitYList.add(hitY);
		repaint(); revalidate();
	}
	public void attackMissed(int missedX, int missedY){
		drawMissedXList.add(missedX);
		drawMissedYList.add(missedY);
		repaint(); revalidate();
	}
	public void dead(){
		removeMouseListener(ma);
		alive = false;
		repaint(); revalidate();
	}
	public void playerDead(){
		isAlive = false;
		repaint(); revalidate();
	}
}

