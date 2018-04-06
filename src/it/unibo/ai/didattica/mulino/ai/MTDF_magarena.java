package it.unibo.ai.didattica.mulino.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import it.unibo.ai.didattica.mulino.actions.ActionGenerator;
import it.unibo.ai.didattica.mulino.actions.ByteAction;
import it.unibo.ai.didattica.mulino.domain.BitBoardUtil;
import it.unibo.ai.didattica.mulino.domain.Board;

import java.util.HashMap;
import java.util.List;

/*
 * MTD(f) algorithm by Aske Plaat
 * https://askeplaat.wordpress.com/534-2/mtdf-algorithm/
 */
public class MTDF_magarena {

    private final Map<Long,TTEntry> table = new HashMap<>();
    private ActionGenerator generator = new ActionGenerator();
    private long END;

    private boolean hasTime() {
        return System.currentTimeMillis() < END;
    }

    private TTEntry iterative_deepening(final Board root, final List<ByteAction> choices, int maxDepth) {
        TTEntry result = null;
        int firstguess = 0;
        for (int d = 1; d<maxDepth; d++) {
            firstguess = MTDF(root, choices, firstguess, d);
            if (hasTime()) {
                result = table.get(root.calculateHashCode());
            }
        }
        return result;
    }

    private int MTDF(final Board root, final List<ByteAction> choices, int f, int d) {
        int g = f;
        int lowerbound = Integer.MIN_VALUE;
        int upperbound = Integer.MAX_VALUE;
        table.clear();
        while (lowerbound < upperbound) {
            int beta = (g == lowerbound) ? g + 1 : g;
            g = AlphaBetaWithMemory(root, choices, beta - 1, beta, d);
            if (g < beta) {
                upperbound = g;
            } else {
                lowerbound = g;
            }
        }
        return g;
    }

    private int AlphaBetaWithMemory(Board board, final List<ByteAction> choices, int alpha, int beta, int d) {
        /* Transposition table lookup */
        final long id = board.calculateHashCode();
        TTEntry entry = table.get(id);
        if (entry != null) {
            if (entry.lowerbound >= beta) {
                return entry.lowerbound;
            }
            if (entry.upperbound <= alpha) {
                return entry.upperbound;
            }
            alpha = Math.max(alpha, entry.lowerbound);
            beta = Math.min(beta, entry.upperbound);
        } else {
            entry = new TTEntry();
            table.put(id, entry);
        }

        if (d == 0 || isOver(board) || hasTime() == false) {
            /* leaf node */
            int g = OurBoardAnalyser.calculateScoreTommy(board);
            entry.update(g, alpha, beta);
            return g;
        }

        final boolean isMax = board.currentPlayer == 0;
        final boolean isMin = !isMax;

        int g = isMax ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        int a = alpha; /* save original alpha value */
        int b = beta;  /* save original beta value */
        int idx = -1;

        for (final ByteAction choice : choices) {
            if ((isMax && g >= beta) ||
                (isMin && g <= alpha)) {
                break;
            }

            board = BitBoardUtil.applyAction(board, choice);
            final List<ByteAction> choices_child = d == 1 ?
                Collections.<ByteAction>emptyList():
                generator.generateMoves(new ArrayList<ByteAction>(), board, false);
            final int g_child = AlphaBetaWithMemory(board, choices_child, a, b, d - 1);
            board = BitBoardUtil.revertAction(board, choice);

            idx++;
            if ((isMax && g_child > g) ||
                (isMin && g_child < g)) {
                g = g_child;
                entry.chosen = idx;
            }

            if (isMax) {
                a = Math.max(a, g);
            } else {
                b = Math.min(b, g);
            }
        }

        final long id_check = board.calculateHashCode();
        if (id != id_check) {
            table.put(id_check, entry);
            table.remove(id);
        }

        entry.update(g, alpha, beta);
        return g;
    }
    public boolean isOver(Board board){
        return this.hasWon((byte)0, board) || this.hasWon((byte)1, board);
    }
    
    public boolean hasWon(byte player, Board board){
    return board.currentPhase != 1 &&
            (board.piecesLeft[1 - player] <= 2 || 
            		OurBoardAnalyser.calculateMobility(board, 1-player, BitBoardUtil.freeSpaces(board.bbs)) == 0 ); // L'avversario non puo' muoversi
    }
}

class TTEntry {
    int lowerbound = Integer.MIN_VALUE;
    int upperbound = Integer.MAX_VALUE;
    int chosen = -1;

    void update(int g, int alpha, int beta) {
        /* Traditional transposition table storing of bounds */
        /* Fail low result implies an upper bound */
        if (g <= alpha) {
            upperbound = g;
        }
        /* Found an accurate minimax value - will not occur if called with zero window */
        if (g > alpha && g < beta) {
            lowerbound = g;
            upperbound = g;
        }
        /* Fail high result implies a lower bound */
        if (g >= beta) {
            lowerbound = g;
        }
    }
}
