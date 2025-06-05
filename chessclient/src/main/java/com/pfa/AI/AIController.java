package com.pfa.AI;

import com.pfa.Main.Board;
import com.pfa.Main.Move;
import com.pfa.Pieces.Bishop;
import com.pfa.Pieces.King;
import com.pfa.Pieces.Knight;
import com.pfa.Pieces.Pawn;
import com.pfa.Pieces.Pieces;
import com.pfa.Pieces.Queen;
import com.pfa.Pieces.Rook;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Callable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

public class AIController {
    private Board board;
    public boolean aiPlaysWhite;
    private boolean isActive = false;
    private int difficulty; // 1=Easy, 2=Medium, 3=Hard, 4=Expert
    private Random random = new Random();
    private long startTime;
    private final long TIME_LIMIT = 5000; // Increased time limit for stability

    // Depth based on difficulty
    private final int[] DEPTHS = { 1, 2, 3, 4 };

    // Thread management
    private ExecutorService executor;
    private Future<Move> moveFuture;
    private volatile boolean searching = false;

    // Piece value constants
    private static final int PAWN_VALUE = 100;
    private static final int KNIGHT_VALUE = 320;
    private static final int BISHOP_VALUE = 330;
    private static final int ROOK_VALUE = 500;
    private static final int QUEEN_VALUE = 900;
    private static final int KING_VALUE = 20000;

    // For logging
    private static final boolean DEBUG = false;

    public AIController(Board board, boolean aiPlaysWhite, int difficulty) {
        this.board = board;
        this.aiPlaysWhite = aiPlaysWhite;
        this.difficulty = Math.min(Math.max(difficulty, 1), 4); // Ensure difficulty is between 1-4
        this.executor = Executors.newSingleThreadExecutor();
        this.random = new Random();
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public void makeAIMove() {
        if (!isActive || board.isGameOver || searching) {
            log("Cannot make AI move - inactive, game over, or already searching");
            return;
        }

        // FIX: Clear and consistent turn logic check
        boolean isAITurn = (aiPlaysWhite && board.isWhitetoMove) || (!aiPlaysWhite && !board.isWhitetoMove);
        if (!isAITurn) {
            log("Not AI's turn");
            return; // Not AI's turn
        }

        searching = true;
        startTime = System.currentTimeMillis();

        log("Starting AI move search, difficulty=" + difficulty);

        // Submit the search task to the executor
        moveFuture = executor.submit(() -> {
            try {
                return findBestMove();
            } catch (Exception e) {
                log("Error in AI search: " + e.getMessage());
                e.printStackTrace();
                return null;
            } finally {
                searching = false;
            }
        });

        try {
            Move bestMove = moveFuture.get(TIME_LIMIT + 1000, TimeUnit.MILLISECONDS);
            log("AI search complete, move found: " + (bestMove != null));

            if (bestMove != null) {
                // FIX: Find the actual piece on the current board
                Pieces actualPiece = findActualPiece(bestMove.piece);
                if (actualPiece != null) {
                    Move safeMove = new Move(board, actualPiece, bestMove.newcol, bestMove.newrow);
                    log("Executing move: " + actualPiece.name + " from (" + actualPiece.col + "," +
                            actualPiece.row + ") to (" + bestMove.newcol + "," + bestMove.newrow + ")");
                    try {

                        board.MakeMove(safeMove);
                    } catch (Exception ex) {
                        System.out.println("an exception when making a move as AI!");
                        ex.printStackTrace();
                    }
                } else {
                    log("ERROR: Could not find piece on board");
                }
            } else {
                log("No valid move found by AI");
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log("AI move calculation error: " + e.getMessage());
            moveFuture.cancel(true);
        } finally {
            searching = false;
        }
    }

    private void log(String message) {
        if (DEBUG) {
            System.out.println("[AI] " + message);
        }
    }

    // FIX: More robust piece finding
    private Pieces findActualPiece(Pieces referencePiece) {
        for (Pieces p : board.pieceList) {
            if (p.col == referencePiece.col && p.row == referencePiece.row &&
                    p.isWhite == referencePiece.isWhite && p.name.equals(referencePiece.name)) {
                return p;
            }
        }
        return null;
    }

    public void cancelSearch() {
        if (moveFuture != null && !moveFuture.isDone()) {
            moveFuture.cancel(true);
        }
        searching = false;
    }

    private Move findBestMove() {
        startTime = System.currentTimeMillis();
        int searchDepth = DEPTHS[difficulty - 1];
        log("Starting search with depth " + searchDepth);

        // FIX: Ensure correct color is used for generating moves
        boolean currentTurn = aiPlaysWhite ? true : false;
        ArrayList<Move> legalMoves = generateAllLegalMoves(currentTurn);
        log("Generated " + legalMoves.size() + " legal moves");

        if (legalMoves.isEmpty()) {
            log("No legal moves found");
            return null;
        }

        // Easy difficulty: Sometimes make random moves
        if (difficulty == 1 && random.nextInt(3) == 0) {
            Move randomMove = legalMoves.get(random.nextInt(legalMoves.size()));
            log("Choosing random move (easy difficulty)");
            return randomMove;
        }

        // Sort moves to improve alpha-beta pruning
        sortMovesByHeuristic(legalMoves);

        // Evaluate moves sequentially - more reliable than the parallel version
        return evaluateMovesSequentially(legalMoves, searchDepth);
    }

    // Sequential move evaluation
    private Move evaluateMovesSequentially(ArrayList<Move> legalMoves, int searchDepth) {
        Move bestMove = null;
        int bestValue = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;

        // Create virtual board for simulation
        VirtualBoard virtualBoard = new VirtualBoard(board);

        for (Move move : legalMoves) {
            if (Thread.currentThread().isInterrupted()) {
                log("Search interrupted");
                break;
            }

            // Make move on virtual board
            virtualBoard.makeMove(move);

            // FIX: Always calculate if this is a maximizing move correctly
            // For AI, we want to maximize if we are the current player
            boolean isMaximizing = virtualBoard.isWhiteToMove() != aiPlaysWhite;

            // Evaluate this move with minimax
            int moveValue = minimax(virtualBoard, searchDepth - 1, alpha, beta, isMaximizing);

            // Log every 10 moves for debugging
            if (DEBUG && legalMoves.indexOf(move) % 10 == 0) {
                log("Evaluated move " + move.piece.name + " from (" + move.piece.col + "," +
                        move.piece.row + ") to (" + move.newcol + "," + move.newrow + ") = " + moveValue);
            }

            // Undo the move
            virtualBoard.undoMove();

            // Update best move if needed
            if (moveValue > bestValue) {
                bestValue = moveValue;
                bestMove = move;
                log("New best move: " + move.piece.name + " to (" + move.newcol + "," + move.newrow + ") = "
                        + bestValue);
            }

            // Update alpha
            alpha = Math.max(alpha, bestValue);

            // Check time limit
            if (System.currentTimeMillis() - startTime > TIME_LIMIT * 0.8) {
                log("Time limit reaching, stopping search");
                break;
            }
        }

        log("Best move found with value: " + bestValue);
        return bestMove;
    }

    // Thread-safe minimax implementation
    private int minimax(VirtualBoard virtualBoard, int depth, int alpha, int beta, boolean isMaximizing) {
        // Check if the current thread has been interrupted
        if (Thread.currentThread().isInterrupted()) {
            return 0;
        }

        // Check time limit
        if (System.currentTimeMillis() - startTime > TIME_LIMIT * 0.8) {
            return evaluatePosition(virtualBoard, aiPlaysWhite);
        }

        // Reach depth limit or game over
        if (depth == 0 || virtualBoard.isGameOver()) {
            return evaluatePosition(virtualBoard, aiPlaysWhite);
        }

        boolean currentPlayerIsWhite = virtualBoard.isWhiteToMove();
        ArrayList<Move> legalMoves = virtualBoard.generateAllLegalMoves(currentPlayerIsWhite);

        if (legalMoves.isEmpty()) {
            // If in check, it's checkmate; otherwise stalemate
            return virtualBoard.isInCheck(currentPlayerIsWhite) ? (isMaximizing ? -10000 : 10000) : 0;
        }

        // Sort moves if not at leaf nodes
        if (depth > 1) {
            sortMovesByHeuristic(legalMoves);
        }

        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;

            for (Move move : legalMoves) {
                if (Thread.currentThread().isInterrupted()) {
                    return 0;
                }

                virtualBoard.makeMove(move);
                int eval = minimax(virtualBoard, depth - 1, alpha, beta, false);
                virtualBoard.undoMove();

                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) {
                    break; // Beta cutoff
                }
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;

            for (Move move : legalMoves) {
                if (Thread.currentThread().isInterrupted()) {
                    return 0;
                }

                virtualBoard.makeMove(move);
                int eval = minimax(virtualBoard, depth - 1, alpha, beta, true);
                virtualBoard.undoMove();

                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) {
                    break; // Alpha cutoff
                }
            }
            return minEval;
        }
    }

    private void sortMovesByHeuristic(ArrayList<Move> moves) {
        moves.sort((a, b) -> {
            // Prioritize captures by the value of the captured piece
            if (a.capture != null && b.capture == null)
                return -1;
            if (a.capture == null && b.capture != null)
                return 1;
            if (a.capture != null && b.capture != null) {
                return getPieceValue(b.capture) - getPieceValue(a.capture);
            }

            // Then prioritize center control
            int aCenterValue = getCenterControlValue(a.newcol, a.newrow);
            int bCenterValue = getCenterControlValue(b.newcol, b.newrow);
            return bCenterValue - aCenterValue;
        });
    }

    private int getCenterControlValue(int col, int row) {
        int colDist = Math.min(col, 7 - col);
        int rowDist = Math.min(row, 7 - row);
        return 8 - (colDist + rowDist);
    }

    public ArrayList<Move> generateAllLegalMoves(boolean isWhite) {
        ArrayList<Move> candidateMoves = new ArrayList<>();

        // FIX: Create a safe copy to avoid concurrent modification
        ArrayList<Pieces> pieceListCopy = new ArrayList<>(board.pieceList);

        for (Pieces piece : pieceListCopy) {
            if (piece.isWhite == isWhite) {
                addPieceMoves(piece, candidateMoves);
            }
        }

        // Filter out moves that would leave the king in check
        ArrayList<Move> legalMoves = new ArrayList<>();
        VirtualBoard virtualBoard = new VirtualBoard(board);

        for (Move move : candidateMoves) {
            virtualBoard.makeMove(move);
            if (!virtualBoard.isInCheck(isWhite)) {
                legalMoves.add(move);
            }
            virtualBoard.undoMove();
        }

        return legalMoves;
    }

    private void addPieceMoves(Pieces piece, ArrayList<Move> moves) {
        switch (piece.name) {
            case "Pawn":
                addPawnMoves(piece, moves);
                break;
            case "Knight":
                addKnightMoves(piece, moves);
                break;
            case "Bishop":
                addSlidingMoves(piece, moves, true, false);
                break;
            case "Rook":
                addSlidingMoves(piece, moves, false, true);
                break;
            case "Queen":
                addSlidingMoves(piece, moves, true, true);
                break;
            case "King":
                addKingMoves(piece, moves);
                break;
        }
    }

    private void addPawnMoves(Pieces pawn, ArrayList<Move> moves) {
        int direction = pawn.isWhite ? -1 : 1;
        int row = pawn.row;
        int col = pawn.col;

        // Forward move
        if (row + direction >= 0 && row + direction < 8) {
            Pieces ahead = getPieceAt(col, row + direction);
            if (ahead == null) {
                addMoveIfValid(new Move(board, pawn, col, row + direction), moves);

                // Double move from starting position - fix the row check
                boolean isAtStartRow = (pawn.isWhite && row == 6) || (!pawn.isWhite && row == 1);
                if (isAtStartRow) {
                    Pieces twoAhead = getPieceAt(col, row + 2 * direction);
                    if (twoAhead == null) {
                        addMoveIfValid(new Move(board, pawn, col, row + 2 * direction), moves);
                    }
                }
            }

            // Capture moves
            for (int dcol : new int[] { -1, 1 }) {
                if (col + dcol >= 0 && col + dcol < 8) {
                    Pieces target = getPieceAt(col + dcol, row + direction);
                    if (target != null && target.isWhite != pawn.isWhite) {
                        addMoveIfValid(new Move(board, pawn, col + dcol, row + direction), moves);
                    }
                }
            }
        }
    }

    private void addKnightMoves(Pieces knight, ArrayList<Move> moves) {
        int[][] offsets = { { 1, 2 }, { 2, 1 }, { 2, -1 }, { 1, -2 }, { -1, -2 }, { -2, -1 }, { -2, 1 }, { -1, 2 } };

        for (int[] offset : offsets) {
            int newCol = knight.col + offset[0];
            int newRow = knight.row + offset[1];

            if (newCol >= 0 && newCol < 8 && newRow >= 0 && newRow < 8) {
                Pieces target = getPieceAt(newCol, newRow);
                if (target == null || target.isWhite != knight.isWhite) {
                    addMoveIfValid(new Move(board, knight, newCol, newRow), moves);
                }
            }
        }
    }

    private void addSlidingMoves(Pieces piece, ArrayList<Move> moves, boolean diagonal, boolean straight) {
        int[][] directions = new int[8][2];
        int dirCount = 0;

        if (diagonal) {
            directions[dirCount++] = new int[] { 1, 1 };
            directions[dirCount++] = new int[] { 1, -1 };
            directions[dirCount++] = new int[] { -1, 1 };
            directions[dirCount++] = new int[] { -1, -1 };
        }

        if (straight) {
            directions[dirCount++] = new int[] { 0, 1 };
            directions[dirCount++] = new int[] { 1, 0 };
            directions[dirCount++] = new int[] { 0, -1 };
            directions[dirCount++] = new int[] { -1, 0 };
        }

        for (int i = 0; i < dirCount; i++) {
            int dcol = directions[i][0];
            int drow = directions[i][1];

            for (int step = 1; step < 8; step++) {
                int newCol = piece.col + dcol * step;
                int newRow = piece.row + drow * step;

                if (newCol < 0 || newCol >= 8 || newRow < 0 || newRow >= 8) {
                    break;
                }

                Pieces target = getPieceAt(newCol, newRow);
                if (target == null) {
                    addMoveIfValid(new Move(board, piece, newCol, newRow), moves);
                } else {
                    if (target.isWhite != piece.isWhite) {
                        addMoveIfValid(new Move(board, piece, newCol, newRow), moves);
                    }
                    break; // Can't move past any piece
                }
            }
        }
    }

    private void addKingMoves(Pieces king, ArrayList<Move> moves) {
        // Regular king moves
        for (int dcol = -1; dcol <= 1; dcol++) {
            for (int drow = -1; drow <= 1; drow++) {
                if (dcol == 0 && drow == 0)
                    continue;

                int newCol = king.col + dcol;
                int newRow = king.row + drow;

                if (newCol >= 0 && newCol < 8 && newRow >= 0 && newRow < 8) {
                    Pieces target = getPieceAt(newCol, newRow);
                    if (target == null || target.isWhite != king.isWhite) {
                        addMoveIfValid(new Move(board, king, newCol, newRow), moves);
                    }
                }
            }
        }

        // Castling
        if (king.isFirstMove) {
            // Kingside castling
            if (canCastle(king, true)) {
                addMoveIfValid(new Move(board, king, king.col + 2, king.row), moves);
            }

            // Queenside castling
            if (canCastle(king, false)) {
                addMoveIfValid(new Move(board, king, king.col - 2, king.row), moves);
            }
        }
    }

    private boolean canCastle(Pieces king, boolean kingSide) {
        // Check if king is in check
        if (isSquareAttacked(king.col, king.row, !king.isWhite)) {
            return false;
        }

        int rookCol = kingSide ? 7 : 0;
        Pieces rook = getPieceAt(rookCol, king.row);

        // Check if rook exists and hasn't moved
        if (rook == null || !rook.name.equals("Rook") || !rook.isFirstMove) {
            return false;
        }

        // Check if path is clear between king and rook
        int start = Math.min(king.col, rookCol) + 1;
        int end = Math.max(king.col, rookCol);

        for (int col = start; col < end; col++) {
            if (getPieceAt(col, king.row) != null) {
                return false;
            }
        }

        // Check if squares the king passes through are under attack
        int direction = kingSide ? 1 : -1;
        for (int i = 1; i <= 2; i++) {
            int checkCol = king.col + (direction * i);
            if (isSquareAttacked(checkCol, king.row, !king.isWhite)) {
                return false;
            }
        }

        return true;
    }

    private boolean isSquareAttacked(int col, int row, boolean byWhite) {
        // FIX: Create a safe copy to avoid concurrent modification
        ArrayList<Pieces> pieceListCopy = new ArrayList<>(board.pieceList);

        for (Pieces piece : pieceListCopy) {
            if (piece.isWhite == byWhite) {
                if (canPieceAttackSquare(piece, col, row)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addMoveIfValid(Move move, ArrayList<Move> moves) {
        // Add move to candidate list without calling board.isValidMove
        // The legality check will be done later in generateAllLegalMoves
        moves.add(move);
    }

    private Pieces getPieceAt(int col, int row) {
        for (Pieces piece : board.pieceList) {
            if (piece.col == col && piece.row == row) {
                return piece;
            }
        }
        return null;
    }

    // FIX: Updated to account for both player perspectives properly
    private int evaluatePosition(VirtualBoard virtualBoard, boolean forWhite) {
        int whiteMaterial = 0;
        int blackMaterial = 0;
        int whitePosition = 0;
        int blackPosition = 0;

        for (Pieces piece : virtualBoard.getPieces()) {
            int pieceValue = getPieceValue(piece);
            int positionBonus = getPositionBonus(piece);

            if (piece.isWhite) {
                whiteMaterial += pieceValue;
                whitePosition += positionBonus;
            } else {
                blackMaterial += pieceValue;
                blackPosition += positionBonus;
            }
        }

        int materialScore = whiteMaterial - blackMaterial;
        int positionScore = whitePosition - blackPosition;

        int whiteMobility = 0;
        int blackMobility = 0;

        if (difficulty > 2) { // Only for higher difficulties
            ArrayList<Move> whiteMoves = virtualBoard.generateAllLegalMoves(true);
            ArrayList<Move> blackMoves = virtualBoard.generateAllLegalMoves(false);
            whiteMobility = whiteMoves.size() * 5; // 5 points per available move
            blackMobility = blackMoves.size() * 5;
        }

        int mobilityScore = whiteMobility - blackMobility;

        // Check for checkmate/stalemate
        boolean whiteInCheck = virtualBoard.isInCheck(true);
        boolean blackInCheck = virtualBoard.isInCheck(false);

        int checkScore = 0;
        if (whiteInCheck)
            checkScore -= 50;
        if (blackInCheck)
            checkScore += 50;

        int totalScore = materialScore + positionScore + mobilityScore + checkScore;

        // Return score from perspective of the player we're evaluating for
        return forWhite ? totalScore : -totalScore;
    }

    private int getPieceValue(Pieces piece) {
        switch (piece.name) {
            case "Pawn":
                return PAWN_VALUE;
            case "Knight":
                return KNIGHT_VALUE;
            case "Bishop":
                return BISHOP_VALUE;
            case "Rook":
                return ROOK_VALUE;
            case "Queen":
                return QUEEN_VALUE;
            case "King":
                return KING_VALUE;
            default:
                return 0;
        }
    }

    private int getPositionBonus(Pieces piece) {
        int col = piece.col;
        int row = piece.row;

        // Always adjust row based on piece color for proper evaluation
        int adjustedRow = piece.isWhite ? row : 7 - row;

        switch (piece.name) {
            case "Pawn":
                // For pawns, advancement is always good
                return 10 * (7 - adjustedRow) + centralizationBonus(col, adjustedRow, 3);
            case "Knight":
                return centralizationBonus(col, adjustedRow, 5);
            case "Bishop":
                return centralizationBonus(col, adjustedRow, 3);
            case "Rook":
                // Reward rooks on 7th rank (2nd rank for opponent)
                return (adjustedRow == 1) ? 30 : 0;
            case "Queen":
                return centralizationBonus(col, adjustedRow, 2);
            case "King":
                // Kings should generally stay back in the middlegame
                int middlegameBonus = (col < 2 || col > 5) ? 20 : 0;
                return middlegameBonus;
            default:
                return 0;
        }
    }

    // Helper method for centralization bonus
    private int centralizationBonus(int col, int row, int factor) {
        int fileDistance = Math.min(col, 7 - col);
        int rankDistance = Math.min(row, 7 - row);
        return factor * (4 - (fileDistance + rankDistance));
    }

    // Check if piece can attack a square (simplified but efficient)
    private boolean canPieceAttackSquare(Pieces piece, int col, int row) {
        int dc = Math.abs(piece.col - col);
        int dr = Math.abs(piece.row - row);

        switch (piece.name) {
            case "Pawn":
                int direction = piece.isWhite ? -1 : 1;
                return dc == 1 && (piece.row + direction) == row;
            case "Knight":
                return (dc == 1 && dr == 2) || (dc == 2 && dr == 1);
            case "Bishop":
                return dc == dr && isPathClear(piece.col, piece.row, col, row);
            case "Rook":
                return (dc == 0 || dr == 0) && isPathClear(piece.col, piece.row, col, row);
            case "Queen":
                return (dc == dr || dc == 0 || dr == 0) && isPathClear(piece.col, piece.row, col, row);
            case "King":
                return dc <= 1 && dr <= 1;
            default:
                return false;
        }
    }

    private boolean isPathClear(int startCol, int startRow, int endCol, int endRow) {
        int colStep = Integer.compare(endCol, startCol);
        int rowStep = Integer.compare(endRow, startRow);

        int currentCol = startCol + colStep;
        int currentRow = startRow + rowStep;

        while (currentCol != endCol || currentRow != endRow) {
            if (getPieceAt(currentCol, currentRow) != null) {
                return false;
            }
            currentCol += colStep;
            currentRow += rowStep;
        }

        return true;
    }

    // Cleanup method
    public void shutdown() {
        cancelSearch();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
            try {
                executor.awaitTermination(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Thread-safe VirtualBoard class for move simulation
    public class VirtualBoard {
        private ArrayList<Pieces> pieces = new ArrayList<>();
        private boolean isWhiteToMove;
        private ArrayList<MoveMemento> moveHistory = new ArrayList<>();

        private class MoveMemento {
            public int oldCol;
            public int oldRow;
            public int newCol;
            public int newRow;
            public boolean wasFirstMove;
            public Pieces capturedPiece;
            public boolean wasCastling = false;
            public Pieces castlingRook;
            public int rookOldCol;
            public boolean wasPromotion = false;
            public Pieces promotedPawn;

            public MoveMemento(Move move) {
                this.oldCol = move.piece.col;
                this.oldRow = move.piece.row;
                this.newCol = move.newcol;
                this.newRow = move.newrow;
                this.wasFirstMove = move.piece.isFirstMove;
            }
        }

        public VirtualBoard(Board realBoard) {
            synchronized (realBoard) {
                for (Pieces originalPiece : realBoard.pieceList) {
                    pieces.add(copyPiece(originalPiece));
                }
                this.isWhiteToMove = realBoard.isWhitetoMove;
            }
        }

        public VirtualBoard(VirtualBoard other) {
            for (Pieces originalPiece : other.pieces) {
                pieces.add(copyPiece(originalPiece));
            }
            this.isWhiteToMove = other.isWhiteToMove;
            // Don't copy move history as it's not needed for a new branch
        }

        private Pieces copyPiece(Pieces original) {
            Pieces copy;
            switch (original.name) {
                case "Pawn":
                    copy = new Pawn(board, original.col, original.row, original.isWhite);
                    break;
                case "Knight":
                    copy = new Knight(board, original.col, original.row, original.isWhite);
                    break;
                case "Bishop":
                    copy = new Bishop(board, original.col, original.row, original.isWhite);
                    break;
                case "Rook":
                    copy = new Rook(board, original.col, original.row, original.isWhite);
                    break;
                case "Queen":
                    copy = new Queen(board, original.col, original.row, original.isWhite);
                    break;
                case "King":
                    copy = new King(board, original.col, original.row, original.isWhite);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown piece type: " + original.name);
            }
            copy.isFirstMove = original.isFirstMove;
            return copy;
        }

        public boolean isWhiteToMove() {
            return isWhiteToMove;
        }

        public List<Pieces> getPieces() {
            return Collections.unmodifiableList(pieces);
        }

        public Pieces getPieceAt(int col, int row) {
            for (Pieces piece : pieces) {
                if (piece.col == col && piece.row == row) {
                    return piece;
                }
            }
            return null;
        }

        public void makeMove(Move move) {
            // Create a memento to store the current state for undo
            MoveMemento memento = new MoveMemento(move);
            moveHistory.add(memento);

            // Find the actual piece in this virtual board
            Pieces movingPiece = getPieceAt(move.piece.col, move.piece.row);

            // Capture piece if any
            Pieces capturedPiece = getPieceAt(move.newcol, move.newrow);
            if (capturedPiece != null) {
                memento.capturedPiece = capturedPiece;
                pieces.remove(capturedPiece);
            }

            // Move the piece
            movingPiece.col = move.newcol;
            movingPiece.row = move.newrow;

            // Mark as moved
            if (movingPiece.isFirstMove) {
                movingPiece.isFirstMove = false;
            }

            // Handle special moves
            if (movingPiece.name.equals("King") && Math.abs(move.newcol - move.piece.col) == 2) {
                handleCastling(movingPiece, move.newcol, memento);
            }

            // Handle pawn promotion
            if (movingPiece.name.equals("Pawn") && (move.newrow == 0 || move.newrow == 7)) {
                memento.wasPromotion = true;
                memento.promotedPawn = movingPiece;

                // Replace pawn with queen (default promotion)
                pieces.remove(movingPiece);
                Queen newQueen = new Queen(board, move.newcol, move.newrow, movingPiece.isWhite);
                pieces.add(newQueen);
            }

            // Switch turns
            isWhiteToMove = !isWhiteToMove;
        }

        private void handleCastling(Pieces king, int newCol, MoveMemento memento) {
            int rookCol = (newCol > king.col) ? 7 : 0;
            int rookNewCol = (newCol > king.col) ? newCol - 1 : newCol + 1;

            Pieces rook = getPieceAt(rookCol, king.row);
            if (rook != null && rook.name.equals("Rook")) {
                memento.wasCastling = true;
                memento.castlingRook = rook;
                memento.rookOldCol = rookCol;

                rook.col = rookNewCol;
                rook.isFirstMove = false;
            }
        }

        public void undoMove() {
            if (moveHistory.isEmpty()) {
                return;
            }

            MoveMemento memento = moveHistory.remove(moveHistory.size() - 1);

            // If it was a promotion, remove the promoted piece and restore the pawn
            if (memento.wasPromotion) {
                Pieces promotedPiece = getPieceAt(memento.newCol, memento.newRow);
                if (promotedPiece != null) {
                    pieces.remove(promotedPiece);
                }
                pieces.add(memento.promotedPawn);
                memento.promotedPawn.col = memento.oldCol;
                memento.promotedPawn.row = memento.oldRow;
                memento.promotedPawn.isFirstMove = memento.wasFirstMove;
            } else {
                // Find the piece that was moved
                Pieces movedPiece = getPieceAt(memento.newCol, memento.newRow);
                if (movedPiece != null) {
                    // Restore original position
                    movedPiece.col = memento.oldCol;
                    movedPiece.row = memento.oldRow;
                    movedPiece.isFirstMove = memento.wasFirstMove;
                }
            }

            // Restore captured piece if any
            if (memento.capturedPiece != null) {
                pieces.add(memento.capturedPiece);
            }

            // Undo castling if applicable
            if (memento.wasCastling && memento.castlingRook != null) {
                Pieces rook = getPieceAt(memento.rookOldCol == 0 ? 3 : 5, memento.oldRow);
                if (rook != null && rook.name.equals("Rook")) {
                    rook.col = memento.rookOldCol;
                    rook.isFirstMove = true;
                }
            }

            // Switch turns back
            isWhiteToMove = !isWhiteToMove;
        }

        public boolean isGameOver() {
            return isCheckmate() || isStalemate();
        }

        public boolean isCheckmate() {
            if (!isInCheck(isWhiteToMove)) {
                return false;
            }
            return generateAllLegalMoves(isWhiteToMove).isEmpty();
        }

        public boolean isStalemate() {
            if (isInCheck(isWhiteToMove)) {
                return false;
            }
            return generateAllLegalMoves(isWhiteToMove).isEmpty();
        }

        public boolean isInCheck(boolean isWhiteKing) {
            Pieces king = findKing(isWhiteKing);
            if (king == null) {
                return false;
            }

            for (Pieces piece : pieces) {
                if (piece.isWhite != isWhiteKing && canPieceAttackKing(piece, king)) {
                    return true;
                }
            }
            return false;
        }

        private Pieces findKing(boolean isWhite) {
            for (Pieces piece : pieces) {
                if (piece.name.equals("King") && piece.isWhite == isWhite) {
                    return piece;
                }
            }
            return null;
        }

        private boolean canPieceAttackKing(Pieces attacker, Pieces king) {
            return canPieceAttack(attacker, king.col, king.row);
        }

        private boolean canPieceAttack(Pieces piece, int targetCol, int targetRow) {
            int dc = Math.abs(piece.col - targetCol);
            int dr = Math.abs(piece.row - targetRow);

            switch (piece.name) {
                case "Pawn":
                    int direction = piece.isWhite ? -1 : 1;
                    return (piece.row + direction == targetRow) && (Math.abs(piece.col - targetCol) == 1);
                case "Knight":
                    return (dc == 1 && dr == 2) || (dc == 2 && dr == 1);
                case "Bishop":
                    if (dc == dr) {
                        return isPathClear(piece.col, piece.row, targetCol, targetRow);
                    }
                    return false;
                case "Rook":
                    if (dc == 0 || dr == 0) {
                        return isPathClear(piece.col, piece.row, targetCol, targetRow);
                    }
                    return false;
                case "Queen":
                    if (dc == dr || dc == 0 || dr == 0) {
                        return isPathClear(piece.col, piece.row, targetCol, targetRow);
                    }
                    return false;
                case "King":
                    return dc <= 1 && dr <= 1;
                default:
                    return false;
            }
        }

        private boolean isPathClear(int startCol, int startRow, int endCol, int endRow) {
            int colStep = Integer.compare(endCol, startCol);
            int rowStep = Integer.compare(endRow, startRow);

            int currentCol = startCol + colStep;
            int currentRow = startRow + rowStep;

            while (currentCol != endCol || currentRow != endRow) {
                if (getPieceAt(currentCol, currentRow) != null) {
                    return false;
                }
                currentCol += colStep;
                currentRow += rowStep;
            }

            return true;
        }

        public ArrayList<Move> generateAllLegalMoves(boolean forWhite) {
            ArrayList<Move> candidateMoves = new ArrayList<>();

            for (Pieces piece : new ArrayList<>(pieces)) {
                if (piece.isWhite == forWhite) {
                    addPieceLegalMoves(piece, candidateMoves);
                }
            }

            // Filter out moves that would leave the king in check
            ArrayList<Move> legalMoves = new ArrayList<>();
            for (Move move : candidateMoves) {
                // Make the move
                makeMove(move);

                // Check if king is in check after the move
                if (!isInCheck(forWhite)) {
                    legalMoves.add(move);
                }

                // Undo the move
                undoMove();
            }

            return legalMoves;
        }

        private void addPieceLegalMoves(Pieces piece, ArrayList<Move> moves) {
            switch (piece.name) {
                case "Pawn":
                    addPawnMoves(piece, moves);
                    break;
                case "Knight":
                    addKnightMoves(piece, moves);
                    break;
                case "Bishop":
                    addSlidingMoves(piece, moves, true, false);
                    break;
                case "Rook":
                    addSlidingMoves(piece, moves, false, true);
                    break;
                case "Queen":
                    addSlidingMoves(piece, moves, true, true);
                    break;
                case "King":
                    addKingMoves(piece, moves);
                    break;
            }
        }

        private void addPawnMoves(Pieces pawn, ArrayList<Move> moves) {
            int direction = pawn.isWhite ? -1 : 1;
            int row = pawn.row;
            int col = pawn.col;

            // Forward move
            if (row + direction >= 0 && row + direction < 8) {
                if (getPieceAt(col, row + direction) == null) {
                    moves.add(new Move(board, pawn, col, row + direction));

                    // Double move from starting position - fix the row check
                    boolean isAtStartRow = (pawn.isWhite && row == 6) || (!pawn.isWhite && row == 1);
                    if (isAtStartRow) {
                        if (getPieceAt(col, row + 2 * direction) == null) {
                            moves.add(new Move(board, pawn, col, row + 2 * direction));
                        }
                    }
                }

                // Capture moves
                for (int dcol : new int[] { -1, 1 }) {
                    if (col + dcol >= 0 && col + dcol < 8) {
                        Pieces target = getPieceAt(col + dcol, row + direction);
                        if (target != null && target.isWhite != pawn.isWhite) {
                            moves.add(new Move(board, pawn, col + dcol, row + direction));
                        }
                    }
                }
            }
        }

        private void addKnightMoves(Pieces knight, ArrayList<Move> moves) {
            int[][] offsets = { { 1, 2 }, { 2, 1 }, { 2, -1 }, { 1, -2 }, { -1, -2 }, { -2, -1 }, { -2, 1 },
                    { -1, 2 } };

            for (int[] offset : offsets) {
                int newCol = knight.col + offset[0];
                int newRow = knight.row + offset[1];

                if (newCol >= 0 && newCol < 8 && newRow >= 0 && newRow < 8) {
                    Pieces target = getPieceAt(newCol, newRow);
                    if (target == null || target.isWhite != knight.isWhite) {
                        moves.add(new Move(board, knight, newCol, newRow));
                    }
                }
            }
        }

        private void addSlidingMoves(Pieces piece, ArrayList<Move> moves, boolean diagonal, boolean straight) {
            int[][] directions = new int[8][2];
            int dirCount = 0;

            if (diagonal) {
                directions[dirCount++] = new int[] { 1, 1 };
                directions[dirCount++] = new int[] { 1, -1 };
                directions[dirCount++] = new int[] { -1, 1 };
                directions[dirCount++] = new int[] { -1, -1 };
            }

            if (straight) {
                directions[dirCount++] = new int[] { 0, 1 };
                directions[dirCount++] = new int[] { 1, 0 };
                directions[dirCount++] = new int[] { 0, -1 };
                directions[dirCount++] = new int[] { -1, 0 };
            }

            for (int i = 0; i < dirCount; i++) {
                int dcol = directions[i][0];
                int drow = directions[i][1];

                for (int step = 1; step < 8; step++) {
                    int newCol = piece.col + dcol * step;
                    int newRow = piece.row + drow * step;

                    if (newCol < 0 || newCol >= 8 || newRow < 0 || newRow >= 8) {
                        break;
                    }

                    Pieces target = getPieceAt(newCol, newRow);
                    if (target == null) {
                        moves.add(new Move(board, piece, newCol, newRow));
                    } else {
                        if (target.isWhite != piece.isWhite) {
                            moves.add(new Move(board, piece, newCol, newRow));
                        }
                        break; // Can't move past any piece
                    }
                }
            }
        }

        private void addKingMoves(Pieces king, ArrayList<Move> moves) {
            // Normal king moves (one square in any direction)
            for (int dcol = -1; dcol <= 1; dcol++) {
                for (int drow = -1; drow <= 1; drow++) {
                    if (dcol == 0 && drow == 0)
                        continue;

                    int newCol = king.col + dcol;
                    int newRow = king.row + drow;

                    if (newCol >= 0 && newCol < 8 && newRow >= 0 && newRow < 8) {
                        Pieces target = getPieceAt(newCol, newRow);
                        if (target == null || target.isWhite != king.isWhite) {
                            moves.add(new Move(board, king, newCol, newRow));
                        }
                    }
                }
            }

            // Castling moves
            if (king.isFirstMove) {
                // Kingside castling
                if (canCastle(king, true)) {
                    moves.add(new Move(board, king, king.col + 2, king.row));
                }

                // Queenside castling
                if (canCastle(king, false)) {
                    moves.add(new Move(board, king, king.col - 2, king.row));
                }
            }
        }

        private boolean canCastle(Pieces king, boolean kingSide) {
            // Check if king is in check
            if (isInCheck(king.isWhite)) {
                return false;
            }

            int rookCol = kingSide ? 7 : 0;
            Pieces rook = getPieceAt(rookCol, king.row);

            // Check if rook exists and hasn't moved
            if (rook == null || !rook.name.equals("Rook") || !rook.isFirstMove) {
                return false;
            }

            // Check if path is clear between king and rook
            int start = Math.min(king.col, rookCol) + 1;
            int end = Math.max(king.col, rookCol);

            for (int col = start; col < end; col++) {
                if (getPieceAt(col, king.row) != null) {
                    return false;
                }
            }

            // Check if king would move through check
            int direction = kingSide ? 1 : -1;
            for (int i = 1; i <= 2; i++) {
                int checkCol = king.col + (direction * i);
                // Create a temporary virtual board to check if this square is under attack
                int originalCol = king.col;
                king.col = checkCol;
                boolean squareAttacked = isInCheck(king.isWhite);
                king.col = originalCol; // Restore king position

                if (squareAttacked) {
                    return false;
                }
            }

            return true;
        }

        private boolean isSquareAttacked(int col, int row, boolean byWhite) {
            for (Pieces piece : pieces) {
                if (piece.isWhite == byWhite && canPieceAttack(piece, col, row)) {
                    return true;
                }
            }
            return false;
        }
    }
}
