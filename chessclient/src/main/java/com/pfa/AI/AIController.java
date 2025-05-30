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

import java.util.ArrayList;
import java.util.Random;

public class AIController {
    private Board board;
    public boolean aiPlaysWhite;
    private boolean isActive = false;
    private int difficulty; // 1=Easy, 2=Medium, 3=Hard, 4=Expert
    private Random random = new Random();
    private long startTime;
    private final long TIME_LIMIT = 2000;

    // Depth based on difficulty
    private final int[] DEPTHS = { 1, 2, 3, 4 };

    // Piece value constants
    private static final int PAWN_VALUE = 100;
    private static final int KNIGHT_VALUE = 320;
    private static final int BISHOP_VALUE = 330;
    private static final int ROOK_VALUE = 500;
    private static final int QUEEN_VALUE = 900;
    private static final int KING_VALUE = 20000;

    public AIController(Board board, boolean aiPlaysWhite, int difficulty) {
        this.board = board;
        this.aiPlaysWhite = aiPlaysWhite;
        this.difficulty = Math.min(Math.max(difficulty, 1), 4); // Ensure difficulty is between 1-4
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public void makeAIMove() {
        if (!isActive || board.isGameOver)
            return;

        // Only make a move if it's AI's turn
        if ((aiPlaysWhite && !board.isWhitetoMove) || (!aiPlaysWhite && board.isWhitetoMove)) {
            return;
        }

        Move bestMove = findBestMove();
        if (bestMove != null) {
            try {
                board.MakeMove(bestMove);
            } catch (Exception ex) {
                System.out.println("exception when making a move!");
            }
        }
    }

    private Move findBestMove() {
        startTime = System.currentTimeMillis();
        int searchDepth = DEPTHS[difficulty - 1];
        ArrayList<Move> legalMoves = generateAllLegalMoves(aiPlaysWhite);

        if (legalMoves.isEmpty()) {
            return null;
        }

        // Easy difficulty: Sometimes make random moves
        if (difficulty == 1 && random.nextInt(3) == 0) {
            return legalMoves.get(random.nextInt(legalMoves.size()));
        }

        // Sort moves to improve alpha-beta pruning
        sortMovesByHeuristic(legalMoves);

        Move bestMove = null;
        int bestValue = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;

        // Create virtual board for simulation
        VirtualBoard virtualBoard = new VirtualBoard(board);

        for (Move move : legalMoves) {
            // Make move on virtual board
            virtualBoard.makeMove(move);

            // Evaluate this move with minimax
            int moveValue = minimax(virtualBoard, searchDepth - 1, alpha, beta, false);

            // Undo the move
            virtualBoard.undoMove();

            // Update best move if needed
            if (moveValue > bestValue) {
                bestValue = moveValue;
                bestMove = move;
            }

            // Update alpha
            alpha = Math.max(alpha, bestValue);

            // Check time limit
            if (System.currentTimeMillis() - startTime > TIME_LIMIT * 0.8) {
                break;
            }
        }

        return bestMove;
    }

    private void sortMovesByHeuristic(ArrayList<Move> moves) {
        moves.sort((a, b) -> {
            if (a.capture != null && b.capture == null)
                return -1;
            if (a.capture == null && b.capture != null)
                return 1;
            if (a.capture != null && b.capture != null) {
                return getPieceValue(b.capture) - getPieceValue(a.capture);
            }

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

    private int minimax(VirtualBoard virtualBoard, int depth, int alpha, int beta, boolean isMaximizing) {
        if (System.currentTimeMillis() - startTime > TIME_LIMIT) {
            return evaluatePosition(virtualBoard);
        }

        if (depth == 0) {
            return evaluatePosition(virtualBoard);
        }

        if (virtualBoard.isGameOver()) {
            if (virtualBoard.isCheckmate()) {
                return isMaximizing ? -10000 : 10000;
            } else {
                return 0; // Stalemate
            }
        }

        boolean currentPlayerIsWhite = virtualBoard.isWhiteToMove();

        ArrayList<Move> legalMoves = virtualBoard.generateAllLegalMoves(currentPlayerIsWhite);

        if (legalMoves.isEmpty()) {
            return virtualBoard.isInCheck(currentPlayerIsWhite) ? (isMaximizing ? -10000 : 10000) : 0;
        }

        if (depth > 1) {
            sortMovesByHeuristic(legalMoves);
        }

        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;

            for (Move move : legalMoves) {
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

    public ArrayList<Move> generateAllLegalMoves(boolean isWhite) {
        ArrayList<Move> candidateMoves = new ArrayList<>();

        ArrayList<Pieces> pieceListCopy = new ArrayList<>(board.pieceList);

        for (Pieces piece : pieceListCopy) {
            if (piece.isWhite == isWhite) {
                addPieceMoves(piece, candidateMoves);
            }
        }

        return candidateMoves;
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

        if (row + direction >= 0 && row + direction < 8) {
            Pieces ahead = getPieceAt(col, row + direction);
            if (ahead == null) {
                addMoveIfValid(new Move(board, pawn, col, row + direction), moves);

                if ((pawn.isWhite && row == 6) || (!pawn.isWhite && row == 1)) {
                    if (getPieceAt(col, row + 2 * direction) == null) {
                        addMoveIfValid(new Move(board, pawn, col, row + 2 * direction), moves);
                    }
                }
            }

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

        if (king.isFirstMove) {
            if (canCastle(king, true)) {
                addMoveIfValid(new Move(board, king, king.col + 2, king.row), moves);
            }

            if (canCastle(king, false)) {
                addMoveIfValid(new Move(board, king, king.col - 2, king.row), moves);
            }
        }
    }

    private boolean canCastle(Pieces king, boolean kingSide) {
        int rookCol = kingSide ? 7 : 0;
        Pieces rook = getPieceAt(rookCol, king.row);

        if (rook == null || !rook.name.equals("Rook") || !rook.isFirstMove) {
            return false;
        }

        // Check if squares between king and rook are empty
        int start = king.col + (kingSide ? 1 : -1);
        int end = kingSide ? rookCol - 1 : rookCol + 1;
        int step = kingSide ? 1 : -1;

        for (int col = start; kingSide ? (col <= end) : (col >= end); col += step) {
            if (getPieceAt(col, king.row) != null) {
                return false;
            }
        }

        if (isSquareAttacked(king.col, king.row, !king.isWhite)) {
            return false;
        }

        int checkCol = king.col + step;
        if (isSquareAttacked(checkCol, king.row, !king.isWhite)) {
            return false;
        }

        return true;
    }

    private boolean isSquareAttacked(int col, int row, boolean byWhite) {
        for (Pieces piece : board.pieceList) {
            if (piece.isWhite == byWhite) {
                if (canPieceAttackSquare(piece, col, row)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addMoveIfValid(Move move, ArrayList<Move> moves) {
        if (board.isValidMove(move)) {
            moves.add(move);
        }
    }

    private Pieces getPieceAt(int col, int row) {
        for (Pieces piece : board.pieceList) {
            if (piece.col == col && piece.row == row) {
                return piece;
            }
        }
        return null;
    }

    private int evaluatePosition(VirtualBoard virtualBoard) {
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

        int totalScore = materialScore + positionScore + (whiteMobility - blackMobility);
        return aiPlaysWhite ? totalScore : -totalScore;
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

        if (!piece.isWhite) {
            row = 7 - row;
        }

        switch (piece.name) {
            case "Pawn":
                return 10 * (row - 1) + centralizationBonus(col, row, 3);
            case "Knight":
                return centralizationBonus(col, row, 5);
            case "Bishop":
                return centralizationBonus(col, row, 3);
            case "Rook":
                return (row == 6) ? 30 : 0;
            case "Queen":
                return centralizationBonus(col, row, 2);
            case "King":
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
        return factor * (fileDistance + rankDistance);
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

    // Simplified VirtualBoard class for move simulation
    private class VirtualBoard {
        private ArrayList<Pieces> pieces = new ArrayList<>();
        private boolean isWhiteToMove;
        private ArrayList<MoveMemento> moveHistory = new ArrayList<>();

        public VirtualBoard(Board realBoard) {
            for (Pieces originalPiece : realBoard.pieceList) {
                pieces.add(copyPiece(originalPiece));
            }
            this.isWhiteToMove = realBoard.isWhitetoMove;
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

        public ArrayList<Pieces> getPieces() {
            return pieces;
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
            MoveMemento memento = new MoveMemento(move);

            Pieces movingPiece = null;
            Pieces capturedPiece = null;

            for (Pieces piece : pieces) {
                if (piece.col == move.piece.col && piece.row == move.piece.row &&
                        piece.name.equals(move.piece.name) && piece.isWhite == move.piece.isWhite) {
                    movingPiece = piece;
                    memento.piece = piece;
                }
                if (move.capture != null && piece.col == move.capture.col && piece.row == move.capture.row) {
                    capturedPiece = piece;
                    memento.capturedPiece = capturedPiece;
                }
            }

            if (movingPiece == null)
                return;

            memento.oldCol = movingPiece.col;
            memento.oldRow = movingPiece.row;
            memento.wasFirstMove = movingPiece.isFirstMove;
            memento.newCol = move.newcol;
            memento.newRow = move.newrow;

            if (movingPiece.name.equals("Pawn") &&
                    ((movingPiece.isWhite && move.newrow == 0) || (!movingPiece.isWhite && move.newrow == 7))) {
                memento.wasPromotion = true;
                memento.promotedPawn = movingPiece;

                pieces.remove(movingPiece);

                Queen queen = new Queen(board, move.newcol, move.newrow, movingPiece.isWhite);
                pieces.add(queen);
            } else {
                // Normal move
                movingPiece.col = move.newcol;
                movingPiece.row = move.newrow;
                movingPiece.isFirstMove = false;
            }

            // Remove captured piece
            if (capturedPiece != null) {
                pieces.remove(capturedPiece);
            }

            // Handle castling
            if (movingPiece.name.equals("King") && Math.abs(memento.oldCol - move.newcol) == 2) {
                handleCastling(movingPiece, memento);
            }

            moveHistory.add(memento);
            isWhiteToMove = !isWhiteToMove;
        }

        private void handleCastling(Pieces king, MoveMemento memento) {
            memento.wasCastling = true;

            // Find the rook and move it
            int rookOldCol = (memento.newCol > memento.oldCol) ? 7 : 0;
            int rookNewCol = (memento.newCol > memento.oldCol) ? 5 : 3;

            for (Pieces piece : pieces) {
                if (piece.name.equals("Rook") && piece.col == rookOldCol && piece.row == king.row &&
                        piece.isWhite == king.isWhite) {
                    memento.castlingRook = piece;
                    memento.rookOldCol = rookOldCol;
                    piece.col = rookNewCol;
                    break;
                }
            }
        }

        public void undoMove() {
            if (moveHistory.isEmpty())
                return;

            MoveMemento memento = moveHistory.remove(moveHistory.size() - 1);

            if (memento.wasPromotion) {
                // Find and remove the promoted piece
                pieces.removeIf(p -> p.col == memento.newCol && p.row == memento.newRow);

                // Restore the pawn
                memento.promotedPawn.col = memento.oldCol;
                memento.promotedPawn.row = memento.oldRow;
                pieces.add(memento.promotedPawn);
            } else {
                // Find the moved piece and restore its position
                for (Pieces piece : pieces) {
                    if (piece == memento.piece ||
                            (piece.col == memento.newCol && piece.row == memento.newRow &&
                                    piece.name.equals(memento.piece.name) && piece.isWhite == memento.piece.isWhite)) {
                        piece.col = memento.oldCol;
                        piece.row = memento.oldRow;
                        piece.isFirstMove = memento.wasFirstMove;
                        break;
                    }
                }
            }

            // Restore captured piece if any
            if (memento.capturedPiece != null) {
                pieces.add(memento.capturedPiece);
            }

            // Handle castling undo
            if (memento.wasCastling && memento.castlingRook != null) {
                memento.castlingRook.col = memento.rookOldCol;
            }

            isWhiteToMove = !isWhiteToMove;
        }

        public boolean isGameOver() {
            // Find king
            Pieces king = findKing(isWhiteToMove);
            if (king == null)
                return true;

            // If king is in check and there are no legal moves, it's checkmate
            if (isInCheck(isWhiteToMove)) {
                return generateAllLegalMoves(isWhiteToMove).isEmpty();
            }

            // If king is not in check but there are no legal moves, it's stalemate
            return generateAllLegalMoves(isWhiteToMove).isEmpty();
        }

        public boolean isCheckmate() {
            Pieces king = findKing(isWhiteToMove);
            if (king == null)
                return false;

            return isInCheck(isWhiteToMove) && generateAllLegalMoves(isWhiteToMove).isEmpty();
        }

        public boolean isStalemate() {
            Pieces king = findKing(isWhiteToMove);
            if (king == null)
                return false;

            return !isInCheck(isWhiteToMove) && generateAllLegalMoves(isWhiteToMove).isEmpty();
        }

        public boolean isInCheck(boolean isWhite) {
            Pieces king = findKing(isWhite);
            if (king == null)
                return false;

            // Check if any opponent piece can attack the king
            for (Pieces piece : pieces) {
                if (piece.isWhite != isWhite) {
                    if (canPieceAttackKing(piece, king)) {
                        return true;
                    }
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
            int dc = Math.abs(attacker.col - king.col);
            int dr = Math.abs(attacker.row - king.row);

            switch (attacker.name) {
                case "Pawn":
                    int direction = attacker.isWhite ? -1 : 1;
                    return dc == 1 && (attacker.row + direction) == king.row;
                case "Knight":
                    return (dc == 1 && dr == 2) || (dc == 2 && dr == 1);
                case "Bishop":
                    return dc == dr && isPathClear(attacker.col, attacker.row, king.col, king.row);
                case "Rook":
                    return (dc == 0 || dr == 0) && isPathClear(attacker.col, attacker.row, king.col, king.row);
                case "Queen":
                    return (dc == dr || dc == 0 || dr == 0)
                            && isPathClear(attacker.col, attacker.row, king.col, king.row);
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

        public ArrayList<Move> generateAllLegalMoves(boolean isWhite) {
            ArrayList<Move> candidateMoves = new ArrayList<>();

            // Generate all possible moves
            for (Pieces piece : pieces) {
                if (piece.isWhite == isWhite) {
                    addPieceLegalMoves(piece, candidateMoves);
                }
            }

            // Filter out moves that would leave the king in check
            candidateMoves.removeIf(this::moveCausesCheck);

            return candidateMoves;
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
                    moves.add(createMove(pawn, col, row + direction));

                    // Double move from starting position
                    if ((pawn.isWhite && row == 6) || (!pawn.isWhite && row == 1)) {
                        if (getPieceAt(col, row + 2 * direction) == null) {
                            moves.add(createMove(pawn, col, row + 2 * direction));
                        }
                    }
                }

                // Captures
                for (int dcol : new int[] { -1, 1 }) {
                    if (col + dcol >= 0 && col + dcol < 8) {
                        Pieces target = getPieceAt(col + dcol, row + direction);
                        if (target != null && target.isWhite != pawn.isWhite) {
                            moves.add(createMove(pawn, col + dcol, row + direction));
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
                        moves.add(createMove(knight, newCol, newRow));
                    }
                }
            }
        }

        private void addSlidingMoves(Pieces piece, ArrayList<Move> moves, boolean diagonal, boolean straight) {
            int[][] directions = new int[8][2];
            int dirCount = 0;

            // Define movement directions
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

            // Explore each direction
            for (int i = 0; i < dirCount; i++) {
                int dcol = directions[i][0];
                int drow = directions[i][1];

                for (int step = 1; step < 8; step++) {
                    int newCol = piece.col + dcol * step;
                    int newRow = piece.row + drow * step;

                    // Check if we're off the board
                    if (newCol < 0 || newCol >= 8 || newRow < 0 || newRow >= 8) {
                        break;
                    }

                    Pieces target = getPieceAt(newCol, newRow);
                    if (target == null) {
                        moves.add(createMove(piece, newCol, newRow));
                    } else {
                        // Can capture opponent's piece
                        if (target.isWhite != piece.isWhite) {
                            moves.add(createMove(piece, newCol, newRow));
                        }
                        break; // Can't move past any piece
                    }
                }
            }
        }

        private void addKingMoves(Pieces king, ArrayList<Move> moves) {
            // Regular king moves (1 square in any direction)
            for (int dcol = -1; dcol <= 1; dcol++) {
                for (int drow = -1; drow <= 1; drow++) {
                    if (dcol == 0 && drow == 0)
                        continue;

                    int newCol = king.col + dcol;
                    int newRow = king.row + drow;

                    if (newCol >= 0 && newCol < 8 && newRow >= 0 && newRow < 8) {
                        Pieces target = getPieceAt(newCol, newRow);
                        if (target == null || target.isWhite != king.isWhite) {
                            moves.add(createMove(king, newCol, newRow));
                        }
                    }
                }
            }

            // Castling
            if (king.isFirstMove) {
                // Kingside castling
                if (canCastle(king, true)) {
                    moves.add(createMove(king, king.col + 2, king.row));
                }

                // Queenside castling
                if (canCastle(king, false)) {
                    moves.add(createMove(king, king.col - 2, king.row));
                }
            }
        }

        private boolean canCastle(Pieces king, boolean kingSide) {
            int rookCol = kingSide ? 7 : 0;
            Pieces rook = getPieceAt(rookCol, king.row);

            if (rook == null || !rook.name.equals("Rook") || !rook.isFirstMove) {
                return false;
            }

            // Check if squares between king and rook are empty
            int start = king.col + (kingSide ? 1 : -1);
            int end = kingSide ? rookCol - 1 : rookCol + 1;
            int step = kingSide ? 1 : -1;

            for (int col = start; kingSide ? (col <= end) : (col >= end); col += step) {
                if (getPieceAt(col, king.row) != null) {
                    return false;
                }
            }

            // The king must not be in check and must not pass through check
            if (isInCheck(king.isWhite)) {
                return false;
            }

            int checkCol = king.col + step;
            if (isSquareAttacked(checkCol, king.row, !king.isWhite)) {
                return false;
            }

            return true;
        }

        private boolean isSquareAttacked(int col, int row, boolean byWhite) {
            for (Pieces piece : pieces) {
                if (piece.isWhite == byWhite) {
                    // Check if piece can attack the square
                    if (canPieceAttack(piece, col, row)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean canPieceAttack(Pieces piece, int col, int row) {
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

        private Move createMove(Pieces piece, int newCol, int newRow) {
            Move move = new Move();
            move.piece = piece;
            move.oldcol = piece.col;
            move.oldrow = piece.row;
            move.newcol = newCol;
            move.newrow = newRow;
            move.capture = getPieceAt(newCol, newRow);
            return move;
        }

        private boolean moveCausesCheck(Move move) {
            // Store original positions
            Pieces movingPiece = move.piece;
            int originalCol = movingPiece.col;
            int originalRow = movingPiece.row;
            Pieces capturedPiece = move.capture;
            boolean wasCaptured = false;

            // Apply the move temporarily
            if (capturedPiece != null) {
                pieces.remove(capturedPiece);
                wasCaptured = true;
            }

            movingPiece.col = move.newcol;
            movingPiece.row = move.newrow;

            // Check if own king is in check
            boolean causesCheck = isInCheck(movingPiece.isWhite);

            // Restore the original state
            movingPiece.col = originalCol;
            movingPiece.row = originalRow;

            if (wasCaptured) {
                pieces.add(capturedPiece);
            }

            return causesCheck;
        }

        // Helper class to store move state for undo
        private class MoveMemento {
            public Pieces piece;
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
                this.piece = move.piece;
                this.oldCol = move.oldcol;
                this.oldRow = move.oldrow;
                this.newCol = move.newcol;
                this.newRow = move.newrow;
            }
        }
    }
}
