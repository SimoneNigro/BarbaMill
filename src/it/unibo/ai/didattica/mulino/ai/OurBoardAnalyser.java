/*
 * Created on Mar 23, 2006
 *
 * (c) 2005 FAO of the UN
 */
package it.unibo.ai.didattica.mulino.ai;

import it.unibo.ai.didattica.mulino.actions.Action;
import it.unibo.ai.didattica.mulino.actions.ActionGenerator;
import it.unibo.ai.didattica.mulino.actions.ByteAction;
import it.unibo.ai.didattica.mulino.domain.BitBoardUtil;
import it.unibo.ai.didattica.mulino.domain.Board;
import it.unibo.ai.didattica.mulino.domain.BoardFeatures;

/**
 * Some observations/assumptions to be used in creating evaluation function:
 * 
 * Max for me, min for her:
 * - Degrees of freedom
 * - Material value
 * - Number of mills
 * 
 * Material values:
 * - Pieces that can form a mill are worth more
 * - Pieces that can form a running mill are worth even more
 *
 * Additional strategies adapted from Paul Emory Sullivan's 'Merrelles' applet:
 *  http://www3.sympatico.ca/pesullivan/merrelles/English.html
 *
 * 
 * Methods ending in "Value" return the evaluation of one side, and take the
 * player to consider as one of the parameters.
 * 
 * Methods ending in "Advantage" calculate the difference between the current
 * player's and the opponents relative score, with higher scores being to the
 * current player's advantage.
 * 
 * Methods ending with "Score" generally implies that some feature of the board 
 * is calculated and weighted 
 * 
 * A row is any three spaces where a mill can be formed. /
 * 
 * We sacrifice some performance for clarity and maintainability of code... 
 * Besides, having clearer code should make optimization easier!
 * 
 * 
 * @author G. Miceli
 */

public class OurBoardAnalyser
{
    
    //coefficienti della fase 1
    static final int CFT1=14;//era 14
    static final int CFT2=37;
    static final int CFT3=4;
    static final int CFT4=14;
    static final int CFT5=20;
    static final int CFT6=2;
    
    //coefficienti della fase 2
    static final int CSD1=16;//era 16
    static final int CSD2=43;
    static final int CSD3=11;
    static final int CSD4=8;
    static final int CSD5=7;
    static final int CSD6=42;
    static final int CSD7=1086;
    
    //coefficienti della fase 3
    static final int CFL1=10;
    static final int CFL2=1;
    static final int CFL3=16;
    static final int CFL4=1190;
    
    public static int calculateScoreTommy(Board board){
    	BoardFeatures.detectMills(board);
    	
    	byte player = board.currentPlayer;
        int score = 0;
        byte otherPlayer = (byte) (1 - player);
        byte morris = BoardFeatures.millcnt[player];
        int freebb = BitBoardUtil.freeSpaces(board.bbs);
        
    	if (board.piecesLeft[player] > 0) { // Fase 1
            return  24 * (board.piecesLeft[player] - board.piecesLeft[otherPlayer] - (9-board.unplacedPieces[player] - (9- board.unplacedPieces[otherPlayer]))) +
                     3 * (board.piecesLeft[otherPlayer]-OurBoardAnalyser.calculateMobility(board, otherPlayer, freebb) - (board.piecesLeft[player]-OurBoardAnalyser.calculateMobility(board, player, freebb))) +
                 //   11 * (OurBoardAnalyser.calculate3PConfigurations(board, player) - OurBoardAnalyser.calculate3PConfigurations(board, otherPlayer)) +
                     9 * (BoardFeatures.millcnt[player] - BoardFeatures.millcnt[otherPlayer]) +
                    10 * (OurBoardAnalyser.calculate2PConfigurationsTommy(board, player) - OurBoardAnalyser.calculate2PConfigurationsTommy(board, otherPlayer)) +
                     7 * (OurBoardAnalyser.calculate3PConfigurationsTommy(board, player) - OurBoardAnalyser.calculate3PConfigurationsTommy(board, otherPlayer)) ;
                         //(this.numberOfHypotheticallyMoves(this.currentPlayer) - this.numberOfHypotheticallyMoves(this.opponentPlayer));
        }  else if (board.piecesLeft[0] > 3 && board.piecesLeft[1] > 3) { // Fase 2
            return  43 * (board.piecesLeft[player] - board.piecesLeft[otherPlayer]) +
                    10 * (board.piecesLeft[otherPlayer]-OurBoardAnalyser.calculateMobility(board, otherPlayer, freebb) - (board.piecesLeft[player]-OurBoardAnalyser.calculateMobility(board, player, freebb))) +
                    // 8 * (this.numberOfUnblockableMorrises(this.currentPlayer) - this.numberOfUnblockableMorrises(this.opponentPlayer)) +
                    11 * (BoardFeatures.millcnt[player] - BoardFeatures.millcnt[otherPlayer]) +
                     8 * (OurBoardAnalyser.calculateDoubleMorrisTommy(board, player) - OurBoardAnalyser.calculateDoubleMorrisTommy(board, otherPlayer)) ;
                        // (this.numberOfReachablePositions(this.currentPlayer) - this.numberOfReachablePositions(this.opponentPlayer));
        } else { // Fase 3
            return  43 * (4 + board.piecesLeft[player] - board.piecesLeft[otherPlayer] * 2) +
                    10 * (OurBoardAnalyser.calculate2PConfigurationsTommy(board, player) - OurBoardAnalyser.calculate2PConfigurationsTommy(board, otherPlayer)) +
                         (OurBoardAnalyser.calculate3PConfigurationsTommy(board, player) - OurBoardAnalyser.calculate3PConfigurationsTommy(board, otherPlayer)) ;
                        // (this.numberOfReachablePositions(this.currentPlayer) - this.numberOfReachablePositions(this.opponentPlayer));
        }
    }
    
    public static int calculateScore(Board board, ByteAction action)
    {
    	BoardFeatures.detectMills(board);
    	
    	byte player = board.currentPlayer;
        int score = 0;
        byte otherPlayer = (byte) (1 - player);
        byte morris = BoardFeatures.millcnt[player];
        int freebb = BitBoardUtil.freeSpaces(board.bbs);
        int R1;
        int R2;
        int R3;
        int R4;
        int R5;
        int R6;
        int R7;
        
        /*
         * Si utilizza un modello a tre fasi con relative equazioni:
         * fase 1: 	opening 
         * fase 2: 	midgame
         * fase 3: 	endgame
         */
        switch(board.currentPhase){
        case 1:
        	//R1: Closed morris
        	R1 = OurBoardAnalyser.GetClosedMorris(board, action, morris, player);
        	//R2: Morrises number
        	R2 = morris;
        	//R3: Number of blocked opp. pieces
        	R3 = board.piecesLeft[otherPlayer]-OurBoardAnalyser.calculateMobility(board, otherPlayer, freebb);
        	//R4: Pieces number
        	R4 = board.piecesLeft[player];
        	//R5: Number of 2 pieces configurations
        	R5 = OurBoardAnalyser.calculate2PConfigurationsTommy(board, player);
        	//R6: Number of 3 pieces configurations 
        	R6 = OurBoardAnalyser.calculate3PConfigurationsTommy(board, player);
  	
        	score = //CFT2*R2 + CFT3*R3 + CFT4*R4 + CFT5*R5 + CFT6*R6;
        			CFT1*R1 + CFT2*R2 + CFT3*R3 + CFT4*R4 + CFT5*R5 + CFT6*R6;
            break;
        case 2:
        	//R1: Closed morris
        	R1 = OurBoardAnalyser.GetClosedMorris(board, action, morris, player);
        	//R2: Morrises number
        	R2 = morris;
        	//R3: Number of blocked opp. pieces
        	R3 = board.piecesLeft[otherPlayer]-OurBoardAnalyser.calculateMobility(board, otherPlayer, freebb);
        	//R4: Pieces number
        	R4 = board.piecesLeft[player];
        	//R5: Opened morris
        	R5 = OurBoardAnalyser.GetOpenedMorris(board, action, morris ,player);
        	//R6: Double morris
        	R6 = OurBoardAnalyser.calculateDoubleMorrisTommy(board, player);
        	//R7: Winning configuration 
        	R7 = OurBoardAnalyser.CheckWinCondition(board, player, freebb);
        
        	score = CSD1*R1 + CSD2*R2 + CSD3*R3 + CSD4*R4 + CSD5*R5 + CSD6*R6 + CSD7*R7;
        	//score = CSD2*R2 + CSD3*R3 + CSD4*R4 + CSD6*R6 + CSD7*R7;
        	break;
            
        case 3:
        	//R1: 2 pieces configurations
        	R1 = OurBoardAnalyser.calculate2PConfigurationsTommy(board, player);
        	//R2: 3 pieces configurations
        	R2 = OurBoardAnalyser.calculate3PConfigurationsTommy(board, player);
        	//R3: Closed morris
        	R3 = OurBoardAnalyser.GetClosedMorris(board, action, morris, player);
        	//R4: Winning configuration 
        	R4 = OurBoardAnalyser.CheckWinCondition(board, player, freebb);
        	
        	score = CFL1*R1 + CFL2*R2 +
        			CFL3*R3 + 
        			CFL4*R4;
        	break;
        default:
            // TODO Calculate defense/attack posture and regulate aggression accordingly.  For rely on search
        }

        return score;
    }

	/** calcolo il numero di Morris chiusi in questo turno */
    private static int GetClosedMorris(Board board, ByteAction action, byte morris, byte player) {
    	board = BitBoardUtil.revertAction(board, action);
    	BoardFeatures.detectMills(board);
    	board = BitBoardUtil.applyAction(board, action);
		byte openedMorris = (byte) (BoardFeatures.millcnt[player] - morris);
		if(openedMorris < 0)
			openedMorris = 0;
		return openedMorris;
	}
    
	/** calcolo il numero di Morris aperti in questo turno */
    private static byte GetOpenedMorris(Board board, ByteAction action, byte morris, byte player) {
       	board = BitBoardUtil.revertAction(board, action);
    	BoardFeatures.detectMills(board);
    	board = BitBoardUtil.applyAction(board, action);
		byte openedMorris = (byte) (morris - BoardFeatures.millcnt[player]);
		if(openedMorris < 0)
			openedMorris = 0;
		return openedMorris;
	}

    /** controlla se lo stato è di vittoria */
	private static int CheckWinCondition(Board board, byte player, int freebb) {
		int winCondition = 0;
		if(board.piecesLeft[1-player] < 3 || 
				OurBoardAnalyser.calculateMobility(board, 1-player, freebb) == 0)
			winCondition = 1;
		return winCondition;
	}


    // ATTENZIONE: sto assumendo che le uniche configurazioni valide siano quelle "fattibili", ovvero
    // non ostacolate da pezzi avversari
    
    /** calcolo le configurazioni a 2 pezzi */
    private static int calculate2PConfigurationsTommy(Board bb, byte player) {
        int board = bb.bbs[player];
        int opponentBoard = bb.bbs[1 - player];
        int tot2piecesConfiguration = 0;
        for (int mill : BoardFeatures.MILL_POSITION_BBS) {
            if ((opponentBoard & mill) == 0 && Integer.bitCount((board & mill)) == 2) {
                tot2piecesConfiguration++;
            }
        }

        return tot2piecesConfiguration;
    }
    public static int calculate2PConfigurations(Board board, int player) {
        int configurations = 0;
        int mybb = board.bbs[player];
        int herbb = board.bbs[1 - player];
        int myrow, herrow;

        for (int millno = 0; millno <= 15; millno++)
        {
            int millmask = BoardFeatures.MILL_POSITION_BBS[millno];
            myrow = mybb & millmask;
            herrow = herbb & millmask;

            // controllo che non ci siano pezzi avversari sulla riga
            if (myrow > 0 && herrow == 0)
            	if(Integer.bitCount(myrow) == 2)
            		configurations+=1;
        }
        return configurations;
    }
    
    // ATTENZIONE: le configurazioni a 3P ora calcolate sono formate da configurazioni a 2P, che vada rimosso
    // del punteggio da queste ultime?
    
    /** calcolo le configurazioni a 3 pezzi */
    private static int calculate3PConfigurationsTommy(Board bb, byte player) {
        int board = bb.bbs[player];
        int opponentBoard = bb.bbs[1 - player];
        int tot3piecesConfiguration = 0;
        for (byte pos = 0; pos < 24; pos++) {
            if (((board >>> pos) & 1) == 1) {
                boolean possibileConfiguration = true;
                for (int mill : BoardFeatures.MILL_POSITION_BBS) {
	                if(BitBoardUtil.isSet(mill, pos)){
	                    int pieces = board & mill;
	                    int opponentPieces = opponentBoard & mill;
	                    if (opponentPieces != 0 || pieces == mill || pieces == (1 << pos)) {
	                        possibileConfiguration = false;
	                        break;
	                    }
                	}
                }
                if (possibileConfiguration) {
                    tot3piecesConfiguration++;
                }
            }
        }

        return tot3piecesConfiguration;
    }
    public static int calculate3PConfigurations(Board board, int player) {
        int configurations = 0;
        int mybb = board.bbs[player];
        int herbb = board.bbs[1 - player];
        
        // FOLLIA più avanti
        
        // per fare una configurazione a 3 ho bisogno che 3 pezzi siano allineati in modo da formare 2 possibili
        // morris, e che gli spazi che userebbero per completare i morris siano liberi
        // anello esterno
        if(BitBoardUtil.isSet(mybb, 0) && BitBoardUtil.isSet(mybb, 1) && BitBoardUtil.isSet(mybb, 7)
        		&& !BitBoardUtil.isSet(herbb, 2) && !BitBoardUtil.isSet(herbb, 6))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 1) && BitBoardUtil.isSet(mybb, 2) && BitBoardUtil.isSet(mybb, 3)
        		&& !BitBoardUtil.isSet(herbb, 0) && !BitBoardUtil.isSet(herbb, 4))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 3) && BitBoardUtil.isSet(mybb, 4) && BitBoardUtil.isSet(mybb, 5)
        		&& !BitBoardUtil.isSet(herbb, 2) && !BitBoardUtil.isSet(herbb, 6))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 5) && BitBoardUtil.isSet(mybb, 6) && BitBoardUtil.isSet(mybb, 7)
        		&& !BitBoardUtil.isSet(herbb, 0) && !BitBoardUtil.isSet(herbb, 4))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 0) && BitBoardUtil.isSet(mybb, 1) && BitBoardUtil.isSet(mybb, 9)
        		&& !BitBoardUtil.isSet(herbb, 2) && !BitBoardUtil.isSet(herbb, 17))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 1) && BitBoardUtil.isSet(mybb, 2) && BitBoardUtil.isSet(mybb, 9)
        		&& !BitBoardUtil.isSet(herbb, 0) && !BitBoardUtil.isSet(herbb, 17))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 2) && BitBoardUtil.isSet(mybb, 3) && BitBoardUtil.isSet(mybb, 11)
        		&& !BitBoardUtil.isSet(herbb, 4) && !BitBoardUtil.isSet(herbb, 19))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 3) && BitBoardUtil.isSet(mybb, 4) && BitBoardUtil.isSet(mybb, 11)
        		&& !BitBoardUtil.isSet(herbb, 2) && !BitBoardUtil.isSet(herbb, 19))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 4) && BitBoardUtil.isSet(mybb, 5) && BitBoardUtil.isSet(mybb, 13)
        		&& !BitBoardUtil.isSet(herbb, 6) && !BitBoardUtil.isSet(herbb, 21))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 5) && BitBoardUtil.isSet(mybb, 6) && BitBoardUtil.isSet(mybb, 13)
        		&& !BitBoardUtil.isSet(herbb, 4) && !BitBoardUtil.isSet(herbb, 21))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 6) && BitBoardUtil.isSet(mybb, 7) && BitBoardUtil.isSet(mybb, 15)
        		&& !BitBoardUtil.isSet(herbb, 0) && !BitBoardUtil.isSet(herbb, 23))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 0) && BitBoardUtil.isSet(mybb, 7) && BitBoardUtil.isSet(mybb, 15)
        		&& !BitBoardUtil.isSet(herbb, 6) && !BitBoardUtil.isSet(herbb, 23))
        	configurations+=1;
        //anello centrale
        if(BitBoardUtil.isSet(mybb, 8) && BitBoardUtil.isSet(mybb, 9) && BitBoardUtil.isSet(mybb, 15)
        		&& !BitBoardUtil.isSet(herbb, 10) && !BitBoardUtil.isSet(herbb, 14))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 9) && BitBoardUtil.isSet(mybb, 10) && BitBoardUtil.isSet(mybb, 11)
        		&& !BitBoardUtil.isSet(herbb, 8) && !BitBoardUtil.isSet(herbb, 12))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 11) && BitBoardUtil.isSet(mybb, 12) && BitBoardUtil.isSet(mybb, 13)
        		&& !BitBoardUtil.isSet(herbb, 10) && !BitBoardUtil.isSet(herbb, 14))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 13) && BitBoardUtil.isSet(mybb, 14) && BitBoardUtil.isSet(mybb, 15)
        		&& !BitBoardUtil.isSet(herbb, 12) && !BitBoardUtil.isSet(herbb, 8))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 8) && BitBoardUtil.isSet(mybb, 9) && BitBoardUtil.isSet(mybb, 1)
        		&& !BitBoardUtil.isSet(herbb, 10) && !BitBoardUtil.isSet(herbb, 17))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 8) && BitBoardUtil.isSet(mybb, 9) && BitBoardUtil.isSet(mybb, 17)
        		&& !BitBoardUtil.isSet(herbb, 1) && !BitBoardUtil.isSet(herbb, 10))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 9) && BitBoardUtil.isSet(mybb, 10) && BitBoardUtil.isSet(mybb, 1)
        		&& !BitBoardUtil.isSet(herbb, 8) && !BitBoardUtil.isSet(herbb, 17))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 9) && BitBoardUtil.isSet(mybb, 10) && BitBoardUtil.isSet(mybb, 17)
        		&& !BitBoardUtil.isSet(herbb, 1) && !BitBoardUtil.isSet(herbb, 8))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 10) && BitBoardUtil.isSet(mybb, 11) && BitBoardUtil.isSet(mybb, 19)
        		&& !BitBoardUtil.isSet(herbb, 3) && !BitBoardUtil.isSet(herbb, 12))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 10) && BitBoardUtil.isSet(mybb, 11) && BitBoardUtil.isSet(mybb, 3)
        		&& !BitBoardUtil.isSet(herbb, 12) && !BitBoardUtil.isSet(herbb, 19))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 11) && BitBoardUtil.isSet(mybb, 12) && BitBoardUtil.isSet(mybb, 19)
        		&& !BitBoardUtil.isSet(herbb, 3) && !BitBoardUtil.isSet(herbb, 10))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 11) && BitBoardUtil.isSet(mybb, 12) && BitBoardUtil.isSet(mybb, 3)
        		&& !BitBoardUtil.isSet(herbb, 10) && !BitBoardUtil.isSet(herbb, 19))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 12) && BitBoardUtil.isSet(mybb, 13) && BitBoardUtil.isSet(mybb, 21)
        		&& !BitBoardUtil.isSet(herbb, 5) && !BitBoardUtil.isSet(herbb, 14))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 12) && BitBoardUtil.isSet(mybb, 13) && BitBoardUtil.isSet(mybb, 5)
        		&& !BitBoardUtil.isSet(herbb, 14) && !BitBoardUtil.isSet(herbb, 21))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 13) && BitBoardUtil.isSet(mybb, 13) && BitBoardUtil.isSet(mybb, 21)
        		&& !BitBoardUtil.isSet(herbb, 5) && !BitBoardUtil.isSet(herbb, 12))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 13) && BitBoardUtil.isSet(mybb, 14) && BitBoardUtil.isSet(mybb, 5)
        		&& !BitBoardUtil.isSet(herbb, 12) && !BitBoardUtil.isSet(herbb, 21))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 14) && BitBoardUtil.isSet(mybb, 15) && BitBoardUtil.isSet(mybb, 23)
        		&& !BitBoardUtil.isSet(herbb, 7) && !BitBoardUtil.isSet(herbb, 8))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 14) && BitBoardUtil.isSet(mybb, 15) && BitBoardUtil.isSet(mybb, 7)
        		&& !BitBoardUtil.isSet(herbb, 8) && !BitBoardUtil.isSet(herbb, 23))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 8) && BitBoardUtil.isSet(mybb, 15) && BitBoardUtil.isSet(mybb, 23)
        		&& !BitBoardUtil.isSet(herbb, 7) && !BitBoardUtil.isSet(herbb, 14))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 8) && BitBoardUtil.isSet(mybb, 15) && BitBoardUtil.isSet(mybb, 7)
        		&& !BitBoardUtil.isSet(herbb, 14) && !BitBoardUtil.isSet(herbb, 23))
        	configurations+=1;
        //anello esterno
        if(BitBoardUtil.isSet(mybb, 16) && BitBoardUtil.isSet(mybb, 17) && BitBoardUtil.isSet(mybb, 23)
        		&& !BitBoardUtil.isSet(herbb, 18) && !BitBoardUtil.isSet(herbb, 22))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 17) && BitBoardUtil.isSet(mybb, 18) && BitBoardUtil.isSet(mybb, 19)
        		&& !BitBoardUtil.isSet(herbb, 16) && !BitBoardUtil.isSet(herbb, 20))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 19) && BitBoardUtil.isSet(mybb, 20) && BitBoardUtil.isSet(mybb, 21)
        		&& !BitBoardUtil.isSet(herbb, 18) && !BitBoardUtil.isSet(herbb, 22))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 21) && BitBoardUtil.isSet(mybb, 22) && BitBoardUtil.isSet(mybb, 23)
        		&& !BitBoardUtil.isSet(herbb, 20) && !BitBoardUtil.isSet(herbb, 16))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 16) && BitBoardUtil.isSet(mybb, 17) && BitBoardUtil.isSet(mybb, 9)
        		&& !BitBoardUtil.isSet(herbb, 1) && !BitBoardUtil.isSet(herbb, 16))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 17) && BitBoardUtil.isSet(mybb, 18) && BitBoardUtil.isSet(mybb, 9)
        		&& !BitBoardUtil.isSet(herbb, 1) && !BitBoardUtil.isSet(herbb, 16))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 18) && BitBoardUtil.isSet(mybb, 19) && BitBoardUtil.isSet(mybb, 11)
        		&& !BitBoardUtil.isSet(herbb, 3) && !BitBoardUtil.isSet(herbb, 20))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 19) && BitBoardUtil.isSet(mybb, 20) && BitBoardUtil.isSet(mybb, 11)
        		&& !BitBoardUtil.isSet(herbb, 3) && !BitBoardUtil.isSet(herbb, 18))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 20) && BitBoardUtil.isSet(mybb, 21) && BitBoardUtil.isSet(mybb, 13)
        		&& !BitBoardUtil.isSet(herbb, 5) && !BitBoardUtil.isSet(herbb, 22))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 21) && BitBoardUtil.isSet(mybb, 22) && BitBoardUtil.isSet(mybb, 13)
        		&& !BitBoardUtil.isSet(herbb, 5) && !BitBoardUtil.isSet(herbb, 20))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 22) && BitBoardUtil.isSet(mybb, 23) && BitBoardUtil.isSet(mybb, 15)
        		&& !BitBoardUtil.isSet(herbb, 7) && !BitBoardUtil.isSet(herbb, 16))
        	configurations+=1;
        if(BitBoardUtil.isSet(mybb, 16) && BitBoardUtil.isSet(mybb, 23) && BitBoardUtil.isSet(mybb, 15)
        		&& !BitBoardUtil.isSet(herbb, 7) && !BitBoardUtil.isSet(herbb, 22))
        	configurations+=1;
        
        return configurations;
    }
    
    /** calcolo i Double Morris */
    private static int calculateDoubleMorrisTommy(Board bb, byte player) {
        int board = bb.bbs[player];
        int totDoubleMorris = 0;
        for (byte pos = 0; pos < 24; pos++) {
            int doubleMill = 0;
            for (int mill : BoardFeatures.MILL_POSITION_BBS) {
            	if(BitBoardUtil.isSet(mill, pos)){
            		doubleMill |= mill;
            	}
            }
            if ((board & doubleMill) == doubleMill) {
                totDoubleMorris++;
            }
        }

        return totDoubleMorris;
    }
    public static int calculateDoubleMorris(Board board, int player) {
        int doubleMorris = 0;
        int mybb = board.bbs[player];
        int completedMorris[] = new int[16];
        int myrow;
        
        for (int millno = 0; millno <= 15; millno++)
        {
            int millmask = BoardFeatures.MILL_POSITION_BBS[millno];
            myrow = mybb & millmask;
        	if(Integer.bitCount(myrow) == 3)
        		completedMorris[millno]=1;
        }
        
        for(int a = 0; a < 3; a++)
        	for(int m = 0; m < 4; m++) {
        		switch(m) {
        		case 3:
        	        if(completedMorris[m+4*a] + completedMorris[4*a] == 2)
        	        	doubleMorris+=1;
        	        if(completedMorris[m+4*a] + completedMorris[m+12] == 2)
        	        	doubleMorris+=1; 
        	        break;
        		default:
        	        if(completedMorris[m+4*a] + completedMorris[m+4*a+1] == 2)
        	        	doubleMorris+=1;
        	        if(completedMorris[m+4*a] + completedMorris[m+12] == 2)
        	        	doubleMorris+=1; 
        	        break;
        		}
        	}

        return doubleMorris;
    }
    
    /** calcolo i pezzi in grado di muoversi */
    public static int calculateMobility(Board board, int player, int freebb) {
        int bb = board.bbs[player];
        int m = 0;

        // for each space in BB
        for (int pos = 0; bb != 0; bb >>>= 1, pos++)
            // which contains a piece
            if ((bb & 1) != 0)
            {
                // count pieces can move to any space that is adjacent and free
                m += Integer.bitCount(ActionGenerator.ADJACENT_POSITION_BBS[pos] & freebb);
            }

        return m;
    }
    
}



