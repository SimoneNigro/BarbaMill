package it.unibo.ai.didattica.mulino.client;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

import java.util.ArrayList;

import gnu.trove.map.TMap;
import gnu.trove.map.hash.THashMap;
import it.unibo.ai.didattica.mulino.actions.Action;
import it.unibo.ai.didattica.mulino.actions.ActionGenerator;
import it.unibo.ai.didattica.mulino.actions.ByteAction;
import it.unibo.ai.didattica.mulino.actions.Phase1Action;
import it.unibo.ai.didattica.mulino.actions.Phase2Action;
import it.unibo.ai.didattica.mulino.actions.PhaseFinalAction;
import it.unibo.ai.didattica.mulino.ai.AlfaBetaSearch;
import it.unibo.ai.didattica.mulino.ai.AlphaBeta;
import it.unibo.ai.didattica.mulino.ai.AlphaBetaNoTT;
import it.unibo.ai.didattica.mulino.ai.MTDF;
import it.unibo.ai.didattica.mulino.ai.Negamax_no_tt;
import it.unibo.ai.didattica.mulino.ai.MTDF.MoveWrapper;
import it.unibo.ai.didattica.mulino.ai.Negamax;
import it.unibo.ai.didattica.mulino.domain.BitBoardUtil;
import it.unibo.ai.didattica.mulino.domain.Board;
import it.unibo.ai.didattica.mulino.domain.State;
import it.unibo.ai.didattica.mulino.domain.State.Checker;
import it.unibo.ai.didattica.mulino.engine.TCPMulino;

public class IAClient extends Thread{
	

	private it.unibo.ai.didattica.mulino.domain.State stato_prof;
	private Thread timeoutThread;
	private ActionGenerator generator;
	private Board pastBoard;
	 Negamax test;
	private List<ByteAction> hisPossibleActions;
	protected Board currentState;
	private boolean isMyTurn;
	private Checker player;
	private int maxTime;
	private int maxDepth;
	//Negamax_no_tt test;
	
	static final int MIN_DEPTH_BOUND = 2;
    static final int MAX_DEPTH_BOUND = 10;
    
    AlphaBeta motore;

    private SearchResult lastChoice;
    private TMap<ByteAction, SearchResult> preparedMoves;
    private boolean interruptSearch;
	
	private Socket playerSocket;
	private ObjectInputStream in;
	private ObjectOutputStream out;
	private boolean interruptParallelSearch;
	
	
	public IAClient(Checker player, int maxDepth, int maxTime) throws UnknownHostException, IOException {
		
		this.motore = new MTDF();
		this.player = player;
		this.maxDepth = maxDepth;
		this.maxTime = maxTime;
		this.generator = null;
		this.hisPossibleActions = null;		
		
		int port = 0;

		switch (player) {
			case WHITE:
				port = TCPMulino.whiteSocket;
				isMyTurn = true;
				break;
			case BLACK:
				port = TCPMulino.blackSocket;
				isMyTurn = false;
				break;
			default:
				System.exit(5);
		}
		this.playerSocket = new Socket("localhost", port);
		this.out = new ObjectOutputStream(playerSocket.getOutputStream());
		this.in = new ObjectInputStream(new BufferedInputStream(playerSocket.getInputStream()));
	}
	
	public void write(Action action) throws IOException, ClassNotFoundException {
		out.writeObject(action);
	}
	
	public void write(String actionString, byte phase) throws IOException, ClassNotFoundException {
		     Action action = null;
		     switch (phase) {
		     case 1: 
		       Phase1Action phase1Action = new Phase1Action();
		       phase1Action.setPutPosition(actionString.substring(0, 2));
		       if (actionString.length() == 4) {
		         phase1Action.setRemoveOpponentChecker(actionString.substring(2, 4));
		       } else {
		         phase1Action.setRemoveOpponentChecker(null);
		       }
		       action = phase1Action;
		       break;
		     case 2: 
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
		     case 3: 
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
	
	public Board read() throws ClassNotFoundException, IOException {
		this.stato_prof = (it.unibo.ai.didattica.mulino.domain.State) this.in.readObject();
		//System.out.println("read(): Provo ad assegnare ad OurState lo stato: "+stato_prof);
		return BitBoardUtil.setState(stato_prof, this.player);
	}
	
	public Checker getPlayer() { return player; }
	public void setPlayer(Checker player) { this.player = player; }

   public void run() {

	 String actionToSend;

     System.out.println("Hello " + this.player + " player here!");
     System.out.println("Port: "+this.playerSocket.getPort());

     try {
		init();
	} catch (ClassNotFoundException e1) {
		e1.printStackTrace();
	} catch (IOException e1) {
		e1.printStackTrace();
	}

     //reads the current state from the server and updates legalActions
     while(true)
     {   	
    	 
       actionToSend = "";
       
       if (!this.isMyTurn) {
    	   System.out.println("currentState "+this.currentState);
    	   System.out.println("Fase currentState "+this.currentState.currentPhase);

    	   nextMove();
       }
       //lo dico io che tocca a me
       this.currentState.currentPlayer = 0;
  	   this.currentState.currentPhase = updatePhase();

       System.out.println("Player " + this.player + ", do your move: ");
       System.out.println("It's your turn, player: "+this.currentState.currentPlayer);
       
       actionToSend = doMove();
       
       if (timeoutThread.isAlive()) {
    	   timeoutThread.interrupt();
       }
       
       try
       {
    	   System.out.println("Provo a mandare la mossa "+actionToSend);
         this.write(actionToSend, this.currentState.currentPhase);
       } catch (Exception e) {
         e.printStackTrace();
         return;
      }
       try
       {
    	   //questa mi mette in this.stato_prof lo stato effetto della mia mossa
    	   this.currentState = read();
    	   this.currentState.currentPhase = updatePhase();
       
       } catch (Exception e) {
         e.printStackTrace();
         return;
       }
       
       System.out.println("Effect of your move: \n" + this.stato_prof.toString());
   
       this.isMyTurn = false;
       
      // System.gc();
     }
   }   
   
   protected void init() throws ClassNotFoundException, IOException {
    // System.out.println("Current state:");
     
    	this.currentState = read();
    	this.currentState.currentPhase = updatePhase();
     //System.out.println(this.currentState.toString());
   }
   
   protected void nextMove() {
     System.out.println("Waiting for your opponent move...");
     try {

    	 //leggo lo stato iniziale "vuoto" se sono all'inizio
    	 //altrimenti leggo l'effetto della mia ultima mossa
    	 if(currentState == null){
	         this.currentState = this.read();
	         this.currentState.currentPhase = updatePhase();
         }
    	 
	       //controlla se è da inizializzare
	       this.currentState.currentPhase = updatePhase();
	       System.out.println("this.currentState prima di pastBoard"+this.currentState);
	       this.pastBoard = this.currentState.clone();
	       this.pastBoard.currentPhase = this.currentState.currentPhase;
	       System.out.println("this.pastBoard prima di prepareSearcg"+this.pastBoard);

	       
     Thread parallelSearch = new Thread(){
    	  public void run(){

    	     	 doPrepareSearch(currentState);
    	     	 stop();
    	  }  
          
      };
       
      parallelSearch.start();
      
       this.currentState = this.read();
       System.out.println("Ho appena letto lo stato "+this.currentState);
       System.out.println("Ora dovrei fermare la ricerca preventiva");
       this.currentState.currentPhase = updatePhase();
       this.currentState.currentPlayer = 0;
       
       //fermo la ricerca parallela e uccido il thread
       this.interruptParallelSearch = true; //magari non serve a niente
       parallelSearch.stop();
       
       this.interruptParallelSearch = false;
       this.isMyTurn = true;
     } catch (Exception e) {
       e.printStackTrace();
       return;
     }

  }
     
   protected String doMove(){
	   
	   this.lastChoice = null;
	   
	   timeoutThread = new Thread() {
		public void run() {
			try {
				Thread.sleep(maxTime*1000);
				motore.interrupt();
				test.interrupt();
				interruptSearch = true;
			} catch (InterruptedException e) {
				System.out.println("Timeout thread is alive!");
			}
		}
	};
	
	timeoutThread.start();
	   
	   doMoveSearch();
	   
	   return lastChoice.action.toString();
   }
	    
   private synchronized void doPrepareSearch(Board currentState)
   {
       // Now do a move search on each one, keeping the results in a map
       preparedMoves = new THashMap<ByteAction, SearchResult>(32);

       System.out.println("Preparo le mosse partendo dallo stato "+currentState);
       System.out.println("FASE "+currentState.currentPhase);
       //calcolo le mosse che l'avversario potrebbe fare, ed i rispettivi stati finali
       this.generator = new ActionGenerator();
       hisPossibleActions=null;
       Board tempState = currentState.clone();
       tempState.currentPlayer = 1;
       tempState.currentPhase = currentState.currentPhase;
       hisPossibleActions = this.generator.generateMoves(new ArrayList<ByteAction>(24), tempState, false);
       this.generator = null;
       
       motore = new MTDF();
       
       for (int db = 2; db <= maxDepth; db++)
       {
    	   //per ciascuna possibile mossa che l'avversario potrebbe fare
           for (ByteAction move : hisPossibleActions)
           {
        	   
               if ( interruptParallelSearch ){
                   System.out.println("currestState a prepareSearch interrotta"+this.currentState);
                   break;

               }
               //    break;
               
               //vai nello stato in cui porterebbe il gioco
               BitBoardUtil.applyAction(tempState, move);	
               
               //e genera le mosse che IO potrei fare a partire dallo stato
               //in cui si è portato l'avversario
 
               tempState.currentPlayer = 0;
               tempState.awaitingCapture = 0;

               MoveWrapper<ByteAction> res = motore.Search(tempState, 0, db, db+1);
               
               if ( res.action != null & !interruptParallelSearch )
               {
            	   //metto MOSSA_AVVERSARIO-----MOSSA_CHE_FARÒ_PER_RISPONDERE
                   preparedMoves.put(move, new SearchResult(res.action, db));
               }
               
               //rimetto a posto lo stato per provare un'altra mossa
               BitBoardUtil.revertAction(tempState, move);

           }
       }
       
       interruptParallelSearch = false;
       System.out.println("currestState dopo la prepareSearch "+this.currentState);
   }


	private synchronized void doMoveSearch()
	{

		lastChoice = new SearchResult();
		int startingDepth = 1;
				//MIN_DEPTH_BOUND;        
		
		if(preparedMoves != null)
			System.out.println("preparedMoves.size() = "+preparedMoves.size());

		//cerco l'ultima azione fatta dall'avversario
		ByteAction lastAction = null;
		//test per escludere il parallelo
		//hisPossibleActions = null;
		
		if(hisPossibleActions != null){
			for(ByteAction temp : this.hisPossibleActions){
				System.out.println("Avrà fatto l'azione? "+temp);

				if(isEqual(temp))
				{
					lastAction = temp;
					System.out.println("Aveva fatto la mossa "+ lastAction);
					break;
				}
			}
			
	       // If we've already done some work, continue from
	       // where we left off
	       if ( preparedMoves != null  )
	       {
	       	//noi qui dobbiamo partire dallo stato letto,
	       	//lo stato di chesani non ha l'ultima mossa, e il tizio usava l'ultima mossa
	    	//per recuperare quel punto
	           SearchResult c = preparedMoves.get(lastAction);
	           if ( c!= null )
	           {
	        	   System.out.println("Parto da: "+c.action+" Profondità: "+c.depth);
	               lastChoice = c;
	               startingDepth = lastChoice.depth+1;
	           }
	       }
		}

		Board tempState = this.currentState.clone();
	       tempState.currentPlayer = 0;
	       tempState.currentPhase = currentState.currentPhase;

        MoveWrapper<ByteAction> res = null;
        this.currentState.currentPlayer = 0;

//        test = new Negamax();
//        res = new MoveWrapper<>();
//        res.action = test.getBestMove(this.currentState, 1, this.maxDepth);
//        
        motore = new AlphaBeta();
        res = motore.Search(this.currentState, 0, 1, this.maxDepth);
        
        if(res != null ){
     	   System.out.println("Best move: "+res.action);
     	   lastChoice.action = res.action;
        }
                      
       timeoutThread.stop();
       interruptSearch = false;
       preparedMoves = null;
       hisPossibleActions = null;
       System.gc();
	}
   
	private boolean isEqual(ByteAction hisLastAction) {
		boolean res;

		this.pastBoard.currentPlayer = 1;
		this.pastBoard = BitBoardUtil.applyAction(this.pastBoard, hisLastAction);

		res = this.pastBoard.bbs[1] == this.currentState.bbs[1]
				&& this.pastBoard.bbs[0] == this.currentState.bbs[0];
		
		
		this.pastBoard = BitBoardUtil.revertAction(this.pastBoard, hisLastAction);

		return res;
}

	public static void main(String[] args) throws UnknownHostException, IOException, ClassNotFoundException, InterruptedException {
		Checker playerColor;
		
		if (args.length==0) {
			System.out.println("You must specify which player you are (White or Black)!");
			System.out.println("-d <depth>     Set the IA algorithm's max search depth (default is no limit)");
			System.out.println("-t <time>      Search w/ iterative deepening for <time> seconds");
			System.exit(-1);
		}
		System.out.println("Selected client: " + args[0]);
				
		int depth = 9999;
		int time = 57;
		
		for (int i = 0; i < args.length; i++) {
			switch (args[i].toLowerCase()) {
			       case "-d": 
			    	   depth = Integer.parseInt(args[++i]);
			    	   break;
			       case "-t": 
			    	   time = Integer.parseInt(args[++i]);
			    	   break;
			}
		}
		if ("White".equals(args[0]))
			playerColor = Checker.WHITE;
		else
			playerColor = Checker.BLACK;
		
		IAClient player = new IAClient(playerColor, depth, time);
		sleep(3000);
		player.start();
	
	}
	
	public byte updatePhase(){
		
		if(this.currentState.unplacedPieces[0] > 0)
			return 1;
		else if(this.currentState.piecesLeft[0] > 3 &&
					this.currentState.piecesLeft[1] > 3 )
			return 2;
		else
			return 3;

		
	}
}

class SearchResult 
{
    public int depth;
    public ByteAction action;
    
    public SearchResult()
    {
    }

    public SearchResult(ByteAction move, int depth)
    {
        this.action = move;
        this.depth = depth;
    }

}