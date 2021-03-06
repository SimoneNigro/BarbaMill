package it.unibo.ai.didattica.mulino.client;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import it.unibo.ai.didattica.mulino.actions.Action;
import it.unibo.ai.didattica.mulino.actions.Phase1Action;
import it.unibo.ai.didattica.mulino.actions.Phase2Action;
import it.unibo.ai.didattica.mulino.actions.PhaseFinalAction;
import it.unibo.ai.didattica.mulino.domain.State;
import it.unibo.ai.didattica.mulino.engine.TCPMulino;


public class MulinoClient {
	
	private State.Checker player;
	private Socket playerSocket;
	private ObjectInputStream in;
	private ObjectOutputStream out;
	
	
	public MulinoClient(State.Checker player) throws UnknownHostException, IOException {
		this.player = player;
		int port = 0;
		switch (player) {
			case WHITE:
				port = TCPMulino.whiteSocket;
				break;
			case BLACK:
				port = TCPMulino.blackSocket;
				break;
			default:
				System.exit(5);
		}
		playerSocket = new Socket("localhost", port);
		out = new ObjectOutputStream(playerSocket.getOutputStream());
		in = new ObjectInputStream(new BufferedInputStream(playerSocket.getInputStream()));
	}
	
	
	public void write(Action action) throws IOException, ClassNotFoundException {
		out.writeObject(action);
	}
	
	public void write(String actionString, State.Phase phase) throws IOException, ClassNotFoundException {
		     Action action = null;
		     switch (phase) {
		     case FIRST: 
		       Phase1Action phase1Action = new Phase1Action();
		       phase1Action.setPutPosition(actionString.substring(0, 2));
		       if (actionString.length() == 4) {
		         phase1Action.setRemoveOpponentChecker(actionString.substring(2, 4));
		       } else {
		         phase1Action.setRemoveOpponentChecker(null);
		       }
		       action = phase1Action;
		       break;
		     case SECOND: 
		       Phase2Action phase2Action = new Phase2Action();
		       phase2Action.setFrom(actionString.substring(0, 2));
		       phase2Action.setTo(actionString.substring(2, 4));
		       if (actionString.length() == 6) {
		         phase2Action.setRemoveOpponentChecker(actionString.substring(4, 6));
		       } else {
		         phase2Action.setRemoveOpponentChecker(null);
		       }
		       action = phase2Action;
		       break;
		     case FINAL: 
		       PhaseFinalAction phaseFinalAction = new PhaseFinalAction();
		       phaseFinalAction.setFrom(actionString.substring(0, 2));
		       phaseFinalAction.setTo(actionString.substring(2, 4));
		       if (actionString.length() == 6) {
		         phaseFinalAction.setRemoveOpponentChecker(actionString.substring(4, 6));
		       } else {
		         phaseFinalAction.setRemoveOpponentChecker(null);
		       }
		       action = phaseFinalAction;
		     }
		     
		     write(action);
		   }
	
	public State read() throws ClassNotFoundException, IOException {
		return (State) in.readObject();
	}
	
	public State.Checker getPlayer() { return player; }
	public void setPlayer(State.Checker player) { this.player = player; }
	
	
	
	
	
	
	
	public static void main(String[] args) throws UnknownHostException, IOException, ClassNotFoundException {
		State.Checker playerColor;
		
		if (args.length==0) {
			System.out.println("You must specify which player you are (Wthie or Black)!");
			System.exit(-1);
		}
		System.out.println("Selected client: " + args[0]);
				
		if ("White".equals(args[0]))
			playerColor = State.Checker.WHITE;
		else
			playerColor = State.Checker.BLACK;
		String actionString = "";
		Phase1Action action;
		State currentState = null;
		
	//	IAClient player = new IAClient(playerColor, depth, maxTime);
//		player.start();
		
		if (playerColor == State.Checker.WHITE) {
			MulinoClient client = new MulinoClient(State.Checker.WHITE);
			System.out.println("You are player " + client.getPlayer().toString() + "!");
			System.out.println("Current state:");
			currentState = client.read();
			System.out.println(currentState.toString());
			BufferedReader in = new BufferedReader( new InputStreamReader(System.in));
			while (true) {
				System.out.println("Player " + client.getPlayer().toString() + ", do your move: ");
				actionString = in.readLine();
				action = new Phase1Action();
				action.setPutPosition(actionString.substring(0, 2));
				if (actionString.length() == 4)
					action.setRemoveOpponentChecker(actionString.substring(2,4));
				else
					action.setRemoveOpponentChecker(null);
				client.write(action);
				currentState = client.read();
				System.out.println("Effect of your move: ");
				System.out.println(currentState.toString());
				System.out.println("Waiting for your opponent move... ");
				currentState = client.read();
				System.out.println("Your Opponent did his move, and the result is: ");
				System.out.println(currentState.toString());
			}
		}
		else {
			MulinoClient client = new MulinoClient(State.Checker.BLACK);
			BufferedReader in = new BufferedReader( new InputStreamReader(System.in));
			currentState = client.read();
			System.out.println("You are player " + client.getPlayer().toString() + "!");
			System.out.println("Current state:");
			System.out.println(currentState.toString());
			while (true) {
				System.out.println("Waiting for your opponent move...");
				currentState = client.read();
				System.out.println("Your Opponent did his move, and the result is: ");
				System.out.println(currentState.toString());
				System.out.println("Player " + client.getPlayer().toString() + ", do your move: ");
				actionString = in.readLine();
				action = new Phase1Action();
				action.setPutPosition(actionString.substring(0, 2));
				if (actionString.length() == 4)
					action.setRemoveOpponentChecker(actionString.substring(2,4));
				else
					action.setRemoveOpponentChecker(null);
				client.write(action);
				currentState = client.read();
				System.out.println("Effect of your move: ");
				System.out.println(currentState.toString());
			}
		}
		
	}


	
}
