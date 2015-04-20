package chauvu_CSCI201_FinalProject;

import javax.swing.*;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;


public class ChatWindow extends JPanel {
	private Socket s;
	private String username;
	private JPanel top;
	private JScrollPane displayedJSP;
	private JTextArea displayedText;
	private JScrollPane userJSP;
	private JTextField userText;
	private PrintWriter pw;
	private BufferedReader br;
	
	public ChatWindow(Socket s, String Username) {
		super();
		this.s = s;
		try{
			pw = new PrintWriter(s.getOutputStream());
			br = new BufferedReader(new InputStreamReader(s.getInputStream()));
		}
		catch(IOException ioe){
			System.out.println("ioe: "+ioe.getMessage());
		}
		this.setSize(200, 1000);
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.username = Username;
		this.top = new JPanel();
		this.top.setSize(200, 60);
		this.displayedText = new JTextArea();
		this.displayedText.setSize(200, 680);
		this.displayedText.setEnabled(false);
		this.displayedText.setLineWrap(true);
		this.displayedJSP = new JScrollPane(this.displayedText, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		this.displayedJSP.setPreferredSize(new Dimension(200, 680));
		this.userText = new JTextField();
		this.userText.setSize(200, 60);
		
		this.userText.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent ke) {
				if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
					String message = userText.getText();
					receiveMessage("ME", message); // displays Client's own message
					pw.println(username+" "+message); pw.flush();
					userText.setText(null); // empties user's text 
				}
			}
			public void keyTyped(KeyEvent e) {}
			public void keyReleased(KeyEvent e) {}
		});
		
		this.userJSP = new JScrollPane(this.userText, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		this.userJSP.setPreferredSize(new Dimension(200, 60));
		this.add(new JLabel("            SITUATION ROOM"));
		this.add(top);
		this.add(displayedJSP);
		this.add(userJSP);
	}
	public void receiveMessage(String sender, String message) {
		this.displayedText.setText(this.displayedText.getText() + "\n\n" + sender + ":\n" + message);
	}
}
