package com.pfa.Main;

import com.pfa.Pieces.Pieces;
import java.util.ArrayList;

public class CheckScanner {
    private Board board;

    public CheckScanner(Board board) {
        this.board = board;
    }

    public boolean isKingChecked(Move move) {
        int startCol = move.piece.col;
        int startRow = move.piece.row;
        boolean isWhite = move.piece.isWhite;
        Pieces capturedPiece = move.capture;

        move.piece.col = move.newcol;
        move.piece.row = move.newrow;

        if (capturedPiece != null) {
            board.pieceList.remove(capturedPiece);
        }

        Pieces king = board.findKing(isWhite);

        boolean isChecked = false;
        if (king != null) {
            isChecked = isSquareAttacked(king.col, king.row, isWhite);
        } else {
            isChecked = true;
        }

        move.piece.col = startCol;
        move.piece.row = startRow;

        if (capturedPiece != null) {
            board.pieceList.add(capturedPiece);
        }

        return isChecked;
    }

    private boolean isSquareAttacked(int col, int row, boolean isWhite) {
        for (Pieces piece : board.pieceList) {
            if (piece.isWhite != isWhite) { // Enemy piece
                if (piece.isValidMovement(col, row) && !piece.MoveCollides(col, row)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isGameOver(Pieces king) {
        if (king == null)
            return true;

        for (Pieces piece : new ArrayList<>(board.pieceList)) {
            if (piece.isWhite == king.isWhite) {
                for (int r = 0; r < board.rows; r++) {
                    for (int c = 0; c < board.cols; c++) {
                        Move move = new Move(board, piece, c, r);
                        if (board.isValidMove(move)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }
}
