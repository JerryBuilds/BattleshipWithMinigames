package chauvu_CSCI201_FinalProject;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Ellipse2D;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

abstract class minigame extends JFrame implements Runnable{
	AtomicBoolean b;
	AtomicBoolean done;
	AtomicBoolean lose = new AtomicBoolean(false);
	JPanel jp = new JPanel();
	JLabel count = new JLabel();
	int c = 3;
	int level = 0;
	String t = "";
	JLabel loc = new JLabel();
	Timer timer;
	BattleshipClient bc;
	Lock lock;
	Condition condition;
	public minigame(AtomicBoolean b1, String title, int l, AtomicBoolean d, BattleshipClient bc, Lock lock, Condition condition){
		super(title); 
		try {
	        UIManager.setLookAndFeel(
            UIManager.getCrossPlatformLookAndFeelClassName());
	    } 
		catch(UnsupportedLookAndFeelException e){
			System.out.println(e.getMessage());
		}
		catch (ClassNotFoundException e) {
			System.out.println(e.getMessage());
	    }
	    catch (InstantiationException e) {
	    	System.out.println(e.getMessage());
	    }
	    catch (IllegalAccessException e) {
	    	System.out.println(e.getMessage());
	    }
		this.bc = bc;
		this.lock = lock;
		this.condition = condition;
		t = title;
		setSize(600, 400); 
		setLocation(100, 100); 
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
		jp.setLayout(null);
		JLabel mini = new JLabel(title);
		mini.setFont(new Font("Serif", Font.PLAIN, 30));
		mini.setSize(300,50);
		mini.setLocation(150,0);
		count.setFont(new Font("Serif", Font.PLAIN, 50));
		count.setSize(50,50);
		count.setLocation(275,175);
		if(title.equals("Maze!")){
			loc.setText(transformStringToHtml("Place Mouse Here!"));
			loc.setFont(new Font("Serif", Font.BOLD, 16));
			loc.setBackground(Color.green);
			loc.setOpaque(true);
			loc.setLocation(10,15);
			loc.setSize(new Dimension(50,350));
			jp.add(loc);
		}
		jp.add(mini);
		jp.add(count);
		add(jp, BorderLayout.CENTER); 
		b = b1;
		done = d;
		level = l;
		setResizable(false);
		setVisible(true);
		repaint();
		timer = new Timer(1000, new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				timerAction();
			}
		});
	}
	public void run(){
		timer.setInitialDelay(0);
		timer.start();
	}
	public void timerAction(){
		if(bc==null) return;
		if(lose.get()){
			done.set(true);
			setVisible(false);
			if(!b.get()){
				bc.noAttack();
			}
			else{
				bc.wonMinigame();
			}
			bc = null;
			timer.stop();
			return;
		}
		if(done.get()){
			setVisible(false);
			if(!b.get()){
				bc.noAttack();
			}
			else{
				bc.wonMinigame();
			}
			bc = null;
			timer.stop();
			return;
		}
		if(c>-1){
			count.setText("" + c);
			jp.repaint();
			c--;					
		}
		if(c == -1){
			remove(jp);
			repaint();
			count.setFont(new Font("Serif", Font.PLAIN, 12));
			JPanel main = new JPanel(new BorderLayout());
			main.add(count, BorderLayout.NORTH);
			JPanel j = createGUI();
			main.add(j,BorderLayout.CENTER);
			add(main);
			repaint();
			c--;
		}
		if(c<-1 && c>-13){
			count.setText("" + (12+c));
			c--;
			jp.repaint();
		}
		if(c == -13){
			done.set(true);
			setVisible(false);
			timer.stop();
			lock.lock();
			condition.signal();
			lock.unlock();
			if(!b.get()){
				bc.noAttack();
			}
			else{
				bc.wonMinigame();
			}
			bc = null;
			timer.stop();
			return;
		}
	}
	public abstract JPanel createGUI();
	public static String transformStringToHtml(String strToTransform) {
	    String ans = "<html>";
	    String br = "<br>";
	    String[] lettersArr = strToTransform.split("");
	    for (String letter : lettersArr) {
	        ans += letter + br;
	    }
	    ans += "</html>";
	    return ans;
	}
	
}

class targets extends minigame{
	String text = "";
	int check = 0;
	int numbs = 9;
	target but1;
	public targets(AtomicBoolean b1, int l, AtomicBoolean b2, BattleshipClient bc, Lock lock, Condition condition){
		super(b1, "Click the Targets!", l, b2, bc, lock, condition);
	}
	public JPanel createGUI(){
		JPanel jp = new JPanel(null);
		Random random = new Random();
		if(level==0){numbs =9;}
		if(level==1){numbs =14;}
		if(level==2){numbs =19;}
		for(int i=0;i<numbs;i++){
			int x = random.nextInt(540 - 5) + 5;
			int y = random.nextInt(290 - 5) + 5;
			but1 = new target("  ");
			but1.setBackground(Color.red);
			but1.setLocation(x,y);
			but1.setSize(50,50);
			jp.add(but1);
			but1.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae){
					check++;
					((JButton) ae.getSource()).setEnabled(false);
					((JButton) ae.getSource()).setVisible(false);
					((JButton) ae.getSource()).setBackground(Color.LIGHT_GRAY);
					if(check == numbs){
						b.set(true);
						((JButton) ae.getSource()).setVisible(false);
						done.set(true);
						return;
					}
				}
			});
		}
		return jp;
	}
}
class clicks extends minigame{
	String text = "";
	int clicktarget = 0;
	int clicks= 0;
	public clicks(AtomicBoolean b1, int l, AtomicBoolean b2, BattleshipClient bc, Lock lock, Condition condition){
		super(b1, "Click!", l, b2, bc, lock, condition);
		
	}
	public JPanel createGUI(){
		JPanel jp = new JPanel(null);
		int max=30;
		int min = 20;
		if(level==0){max =25; min = 20;}
		if(level==1){max =30; min = 25;}
		if(level==2){max =40; min = 30;}
		Random random = new Random();
		clicktarget = random.nextInt(max - min) + min;
		target but = new target("Click me exactly "+clicktarget+ " times!");
		but.setBackground(Color.red);
		but.setLocation(200,70);
		but.setSize(200,200);
		jp.add(but);
		but.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae){
				clicks++;
				if(clicks == clicktarget){
					b.set(true);
				}
				else{
					b.set(false);
				}
			}
		});
		return jp;
	}
}

class count extends minigame{
	int lr=0;
	int n1 =5;
	int n2 =0;
	int total = 3;
	int n=0;
	JLabel q;
	JPanel jp = new JPanel();
	Random random = new Random();
	String text = "";
	JTextArea textbox = new JTextArea();
	
	String [] imgPaths = {"Image/jeffrey_miller.png", "Image/mark_redekopp.png", "Image/mike_crowley.png"};
	int rand = (int)(Math.random()*3);
	Image im = Toolkit.getDefaultToolkit().getImage(imgPaths[rand]);

	public count(AtomicBoolean b1, int l, AtomicBoolean b2, BattleshipClient bc, Lock lock, Condition condition){
		super(b1, "Count!", l, b2, bc, lock, condition);
	}
	public void draw(Graphics g, int x, int y){
		g.drawImage(im, x,y, null);
	}
	public JPanel createGUI(){
		n1 = random.nextInt(4) + 1; n2 = random.nextInt(4) + 1;
		if(level==2){n2=random.nextInt(5) + 3; n1 = random.nextInt(4) + 2;}
		if(level==0){n2 =0;}
		total = n1+n2;
		jp.setLayout(null);
			for(int i=0; i<n1;i++){
				lr = (random.nextInt(10)+1)%2;
				final Timer time = new Timer(5, new ActionListener(){
					int rl = lr;
					int yinit = random.nextInt(300/n1-49) + n*(300/n1);
					int xinit = 600*lr;
					public void actionPerformed(ActionEvent ae){
						Graphics g = jp.getGraphics();
						draw(g, xinit, yinit);
						if(rl>0){
						xinit--;}
						else{
							xinit++;
						}
					}
				});
				int wait = random.nextInt(40) + 1;
				time.setInitialDelay(wait*25);
				time.start();
				n++;
			}
			n=0;
			for(int i=0; i<n2;i++){
				lr = (random.nextInt(10)+1)%2;
				final Timer time = new Timer(5, new ActionListener(){
					int rl = lr;
					int yinit = 600*rl;
					int xinit = random.nextInt(550/n2-49) + n*(550/n2);
					public void actionPerformed(ActionEvent ae){
						Graphics g = jp.getGraphics();
						draw(g, xinit, yinit);
						if(rl>0){
						yinit--;}
						else{
							yinit++;
						}
					}
				});
				int wait = random.nextInt(40) + 1;
				time.setInitialDelay(wait*25);
				time.start();
				n++;
			}
		final Timer timer2 = new Timer(125, new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				repaint();
			}});
		timer2.start();
		final Timer timer1 = new Timer(5000, new ActionListener(){
			public void actionPerformed(ActionEvent ae){
					q = new JLabel("How many people were there?");
					q.setSize(400, 25);
					q.setLocation(225,85);
					textbox.setSize(100, 25);
					textbox.setLocation(210,110);
					JButton enter = new JButton("Enter");
					enter.setSize(100, 25);
					enter.setLocation(310, 110);
					jp.add(q);
					jp.add(textbox);
					jp.add(enter);
					jp.repaint();
					enter.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae){
							String s =textbox.getText();
							int n=0;
							try {
								n =Integer.parseInt(s);
							} catch (Exception e) {}
							if(n==total){
								b.set(true);
								done.set(true);
							}
							else{
								q.setText("Wrong!");
								done.set(true);
							}
						}
					});
			}
		});
		timer1.setInitialDelay(5000);
		timer1.start();
		return jp;
	}
}

class target extends JButton {
		  public target(String label) {
		    super(label);
		    Dimension size = getPreferredSize();
		    size.width = size.height = Math.max(size.width, 
		      size.height);
		    setPreferredSize(size);
		    setContentAreaFilled(false);
		  }
		  protected void paintComponent(Graphics g) {
		    if (getModel().isArmed()) {
		      g.setColor(Color.lightGray);
		    } else {
		      g.setColor(getBackground());
		    }
		    g.fillOval(0, 0, getSize().width-1, 
		      getSize().height-1);
		    super.paintComponent(g);
		  }
		  protected void paintBorder(Graphics g) {
		    g.setColor(getForeground());
		    g.drawOval(0, 0, getSize().width-1, 
		      getSize().height-1);
		  }
		  Shape shape;
		  public boolean contains(int x, int y) {
		    if (shape == null || 
		      !shape.getBounds().equals(getBounds())) {
		      shape = new Ellipse2D.Float(0, 0, 
		        getWidth(), getHeight());
		    }
		    return shape.contains(x, y);
		  }
	}

class match extends minigame{
	String text = "";
	char[] characters = {'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','x','y','z','1','2','3','4','5','6','7','8','9','0','!','@','#','$','%','^','&','*','(',')'};
	String in = "";
	public match(AtomicBoolean b1, int l,AtomicBoolean d, BattleshipClient bc, Lock lock, Condition condition){
		super(b1, "Match String!",l,d,bc,lock,condition);
		
	}
	public JPanel createGUI(){
		JPanel jp = new JPanel();
		jp.setLayout(null);
		if(level == 0){
			for(int i=0; i<7; i++){
				int x = (int)(Math.random()*(characters.length));
				text += characters[x];
			}
		}
		if(level == 1){
			for(int i=0; i<9; i++){
				int x = (int)(Math.random()*(characters.length));
				text += characters[x];
			}
		}
		if(level == 2){
			for(int i=0; i<12; i++){
				int x = (int)(Math.random()*(characters.length));
				text += characters[x];
			}
		}
		JLabel t = new JLabel(text);
		t.setLocation(200,20);
		t.setFont(new Font("Serif", Font.PLAIN, 30));
		t.setSize(400,75);
		JLabel type = new JLabel("Type here:");
		type.setSize(new Dimension(75,20));
		type.setLocation(150,175);
		final JTextArea input = new JTextArea(1,15);
		input.setSize(new Dimension(100,20));
		input.setLocation(225,175);
		JButton enter = new JButton("Enter");
		enter.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				in = input.getText();
				if(in.equals(text)){
					b.set(true);
					done.set(true);
				}
				else{
					input.setText("");
				}
			}
		});
		enter.setLocation(225,200);
		enter.setSize(new Dimension(100,30));
		jp.add(t);
		jp.add(type);
		jp.add(input);
		jp.add(enter);
		return jp;
	}
}

class maze extends minigame{

	public maze(AtomicBoolean b1, int l, AtomicBoolean d, BattleshipClient bc, Lock lock, Condition condition) {
		super(b1, "Maze!", l, d, bc, lock, condition);
		loc.addMouseListener(new MouseListener() {
			public void mousePressed(MouseEvent me) {
	        }
	        public void mouseClicked(MouseEvent me) {
	        }
	        public void mouseEntered(MouseEvent me) {
	            timer.start();
	            c = -1;
	        }
	        public void mouseExited(MouseEvent me) {
	        }
			public void mouseReleased(MouseEvent arg0) {
			}
		});
		timer.setInitialDelay(0);
	}

	public JPanel createGUI() {
		JPanel jp = new JPanel();
		jp.setLayout(null);
		JLabel up = new JLabel("");
		up.setBackground(Color.black);
		up.setOpaque(true);
		up.setSize(new Dimension(600,10));
		up.setLocation(0,0);
		up.addMouseListener(new MouseListener() {
			public void mousePressed(MouseEvent me) {
	        }
	        public void mouseClicked(MouseEvent me) {
	        }
	        public void mouseEntered(MouseEvent me) {
	            lose.set(true);
	        }
	        public void mouseExited(MouseEvent me) {
	        }
			public void mouseReleased(MouseEvent arg0) {
			}
		});
		JLabel down = new JLabel("");
		down.setBackground(Color.black);
		down.setOpaque(true);
		down.setSize(new Dimension(600,15));
		down.setLocation(0,345);
		down.addMouseListener(new MouseListener() {
			public void mousePressed(MouseEvent me) {
	        }
	        public void mouseClicked(MouseEvent me) {
	        }
	        public void mouseEntered(MouseEvent me) {
	            lose.set(true);
	        }
	        public void mouseExited(MouseEvent me) {
	        }
			public void mouseReleased(MouseEvent arg0) {
			}
		});
		JLabel left = new JLabel("");
		left.setBackground(Color.black);
		left.setOpaque(true);
		left.setSize(new Dimension(10,400));
		left.setLocation(0,0);
		left.addMouseListener(new MouseListener() {
			public void mousePressed(MouseEvent me) {
	        }
	        public void mouseClicked(MouseEvent me) {
	        }
	        public void mouseEntered(MouseEvent me) {
	            lose.set(true);
	        }
	        public void mouseExited(MouseEvent me) {
	        }
			public void mouseReleased(MouseEvent arg0) {
			}
		});
		JLabel right = new JLabel("");
		right.setBackground(Color.black);
		right.setOpaque(true);
		right.setSize(new Dimension(10,400));
		right.setLocation(585,0);
		right.addMouseListener(new MouseListener() {
			public void mousePressed(MouseEvent me) {
	        }
	        public void mouseClicked(MouseEvent me) {
	        }
	        public void mouseEntered(MouseEvent me) {
	            lose.set(true);
	        }
	        public void mouseExited(MouseEvent me) {
	        }
			public void mouseReleased(MouseEvent arg0) {
			}
		});
		jp.add(left);
		jp.add(right);
		jp.add(up);
		jp.add(down);
		if(level == 0){
			for(int i=1; i<4; i++){
				int y = (int)(Math.random()*300);
				JLabel rect = new JLabel("");
				rect.setBackground(Color.black);
				rect.setOpaque(true);
				rect.setSize(new Dimension(50,y));
				rect.setLocation(150*i,0);
				rect.addMouseListener(new MouseListener() {
					public void mousePressed(MouseEvent me) {
			        }
			        public void mouseClicked(MouseEvent me) {
			        }
			        public void mouseEntered(MouseEvent me) {
			            lose.set(true);
			        }
			        public void mouseExited(MouseEvent me) {
			        }
					public void mouseReleased(MouseEvent arg0) {
					}
				});
				JLabel rect2 = new JLabel("");
				rect2.setBackground(Color.black);
				rect2.setOpaque(true);
				rect2.setSize(new Dimension(50,400-y));
				rect2.setLocation(150*i,y+25);
				rect2.addMouseListener(new MouseListener() {
					public void mousePressed(MouseEvent me) {
			        }
			        public void mouseClicked(MouseEvent me) {
			        }
			        public void mouseEntered(MouseEvent me) {
			            lose.set(true);
			        }
			        public void mouseExited(MouseEvent me) {
			        }
					public void mouseReleased(MouseEvent arg0) {
					}
				});
				jp.add(rect);
				jp.add(rect2);
			}
		}
		if(level == 1){
			for(int i=1; i<5; i++){
				int y = (int)(Math.random()*300);
				JLabel rect = new JLabel("");
				rect.setBackground(Color.black);
				rect.setOpaque(true);
				rect.setSize(new Dimension(40,y));
				rect.setLocation(115*i,0);
				rect.addMouseListener(new MouseListener() {
					public void mousePressed(MouseEvent me) {
			        }
			        public void mouseClicked(MouseEvent me) {
			        }
			        public void mouseEntered(MouseEvent me) {
			            lose.set(true);
			        }
			        public void mouseExited(MouseEvent me) {
			        }
					public void mouseReleased(MouseEvent arg0) {
					}
				});
				JLabel rect2 = new JLabel("");
				rect2.setBackground(Color.black);
				rect2.setOpaque(true);
				rect2.setSize(new Dimension(40,400-y));
				rect2.setLocation(115*i,y+25);
				rect2.addMouseListener(new MouseListener() {
					public void mousePressed(MouseEvent me) {
			        }
			        public void mouseClicked(MouseEvent me) {
			        }
			        public void mouseEntered(MouseEvent me) {
			            lose.set(true);
			        }
			        public void mouseExited(MouseEvent me) {
			        }
					public void mouseReleased(MouseEvent arg0) {
					}
				});
				jp.add(rect);
				jp.add(rect2);
			}
		}
		if(level == 2){
			for(int i=1; i<6; i++){
				int y = (int)(Math.random()*300);
				JLabel rect = new JLabel("");
				rect.setBackground(Color.black);
				rect.setOpaque(true);
				rect.setSize(new Dimension(40,y));
				rect.setLocation(90*i,0);
				rect.addMouseListener(new MouseListener() {
					public void mousePressed(MouseEvent me) {
			        }
			        public void mouseClicked(MouseEvent me) {
			        }
			        public void mouseEntered(MouseEvent me) {
			            lose.set(true);
			        }
			        public void mouseExited(MouseEvent me) {
			        }
					public void mouseReleased(MouseEvent arg0) {
					}
				});
				JLabel rect2 = new JLabel("");
				rect2.setBackground(Color.black);
				rect2.setOpaque(true);
				rect2.setSize(new Dimension(40,400-y));
				rect2.setLocation(90*i,y+25);
				rect2.addMouseListener(new MouseListener() {
					public void mousePressed(MouseEvent me) {
			        }
			        public void mouseClicked(MouseEvent me) {
			        }
			        public void mouseEntered(MouseEvent me) {
			            lose.set(true);
			        }
			        public void mouseExited(MouseEvent me) {
			        }
					public void mouseReleased(MouseEvent arg0) {
					}
				});
				jp.add(rect);
				jp.add(rect2);
			}
		}
		JButton win = new JButton("WIN!");
		win.setSize(new Dimension(70,30));
		win.setLocation(515,170);
		win.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				b.set(true);
				done.set(true);
			}
		});
		jp.add(win);
		return jp;
	}
	public void run(){
		
	}
	public void timerAction(){
				if(lose.get()){
					done.set(true);
					setVisible(false);
					return;
				}
				if(done.get()){
					setVisible(false);
					if(!b.get()){
						bc.noAttack();
					}
					else{
						bc.wonMinigame();
					}
					bc = null;
					timer.stop();
					return;
				}
				if(c>-1){
					count.setText("" + c);
					jp.repaint();
					c--;					
				}
				if(c == -1){
					remove(jp);
					repaint();
					count.setFont(new Font("Serif", Font.PLAIN, 12));
					JPanel main = new JPanel(new BorderLayout());
					main.add(count, BorderLayout.NORTH);
					JPanel j = createGUI();
					main.add(j,BorderLayout.CENTER);
					add(main);
					repaint();
					c--;
				}
				if(c<-1 && c>-13){
					count.setText("" + (12+c));
					c--;
					jp.repaint();
				}
				if(c == -13){
					done.set(true);
					setVisible(false);
					return;
				}
	}
}

class pattern extends minigame{
	LinkedList<JButton> buttons = new LinkedList<JButton>();
	LinkedList<Integer> pat = new LinkedList<Integer>();
	LinkedList<Integer> input = new LinkedList<Integer>();
	int count = 0;
	int c = 0;
	int s;
	int right = 0;
	public pattern(AtomicBoolean b1, int l,AtomicBoolean d, BattleshipClient bc, Lock lock, Condition condition){
		super(b1, "Pattern Memory!",l,d, bc, lock,condition);
	}


	public JPanel createGUI() {
		final JPanel jp = new JPanel();
		jp.setLayout(null);
		if(level == 0){
			s = 4;
			c = 4;
			for(int i=0; i<4; i++){
				int x = (int)(Math.random()*4);
				pat.add(x);
			}
		}
		if(level == 1){
			s = 6;
			c = 6;
			for(int i=0; i<6; i++){
				int x = (int)(Math.random()*4);
				pat.add(x);
			}
		}
		if(level == 2){
			s = 7;
			c = 7;
			for(int i=0; i<7; i++){
				int x = (int)(Math.random()*4);
				pat.add(x);
			}
		}
		final JLabel mem = new JLabel("Memorize!");
		mem.setSize(75,20);
		mem.setLocation(50,50);
		JButton b0 = new target("Red");
		b0.setForeground(Color.red);
		b0.setSize(new Dimension(100,100));
		b0.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				input.add(0);
			}
		});
		b0.setLocation(150,30);
		b0.setBackground(Color.white);
		b0.setEnabled(false);
		buttons.add(b0);
		JButton b1 = new target("Blue");
		b1.setForeground(Color.blue);
		b1.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				input.add(1);
			}
		});
		b1.setSize(new Dimension(100,100));
		b1.setLocation(300,30);
		b1.setBackground(Color.white);
		b1.setEnabled(false);
		buttons.add(b1);
		JButton b2 = new target("Green");
		b2.setForeground(Color.green);
		b2.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				input.add(2);
			}
		});
		b2.setSize(new Dimension(100,100));
		b2.setLocation(150,150);
		b2.setBackground(Color.white);
		b2.setEnabled(false);
		buttons.add(b2);
		JButton b3 = new target("Yellow");
		b3.setForeground(Color.yellow);
		b3.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				input.add(3);
			}
		});
		b3.setSize(new Dimension(100,100));
		b3.setLocation(300,150);
		b3.setBackground(Color.white);
		b3.setEnabled(false);
		buttons.add(b3);
		jp.add(b0);
		jp.add(b1);
		jp.add(b2);
		jp.add(b3);
		jp.add(mem);
		final Timer timer = new Timer(250, new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				if(count%2 == 1){
					for(int i=0; i<4; i++){
						buttons.get(i).setBackground(Color.white);
					}
				}
				if(count%2 == 0){
					if(c>0){
						int x = pat.get(s-c);
						JButton b = buttons.get(x);
						if(level == 0 || level == 1){
							if(x == 0){
								b.setBackground(Color.red);
							}
							if(x == 1){
								b.setBackground(Color.blue);
							}
							if(x == 2){
								b.setBackground(Color.green);
							}
							if(x == 3){
								b.setBackground(Color.yellow);
							}
						}
						if(level == 2){
							if(x == 0){
								b.setBackground(Color.yellow);
							}
							if(x == 1){
								b.setBackground(Color.green);
							}
							if(x == 2){
								b.setBackground(Color.blue);
							}
							if(x == 3){
								b.setBackground(Color.red);
							}
						}
					}
					jp.repaint();
					c--;
				}
				if(c<=0){
					mem.setText("Repeat!");
					right = 0;
					for(int i=0; i<4; i++){
						buttons.get(i).setEnabled(true);
					}
					if(pat.size() <= input.size()){
						for(int j=0; j<pat.size(); j++){
							if(pat.get(j) == input.get(j)){
								right++;
							}
						}
						if(right == s){
							b.set(true);
							done.set(true);
						}
					}
				}
				count++;
			}
		});
		timer.setInitialDelay(0);
		timer.start();
		return jp;
	}
}

