package com.pfa.Pieces;

import com.pfa.Main.Board;
import com.pfa.Main.Move;
import javafx.scene.image.Image;

public class King extends Pieces {
    public King(Board board, int col, int row, boolean isWhite) {
        super(board);
        this.col = col;
        this.row = row;
        this.xPos = col * board.tileSize;
        this.yPos = row * board.tileSize;
        this.isWhite = isWhite;
        this.name = "King";

        // Use consistent image loading approach
        String path = isWhite ? "wKing.png" : "bKing.png";
        this.sprite = loadAndSizeImage(path, board.tileSize);
    }

    public boolean isValidMovement(int col, int row) {
        return Math.abs((col - this.col) * (row - this.row)) == 1 ||
                Math.abs(col - this.col) + Math.abs(row - this.row) == 1 ||
                canCastle(col, row);
    }

    private boolean canCastle(int col, int row) {
        if (this.row == row) {
            if (col == 6) {
                Pieces rook = board.getPieces(7, row);
                if (rook != null && rook.isFirstMove && isFirstMove) {
                    return board.getPieces(5, row) == null &&
                            board.getPieces(6, row) == null &&
                            !board.checkScanner.isKingChecked(new Move(board, this, 5, row));
                }
            } else if (col == 2) {
                Pieces rook = board.getPieces(0, row);
                if (rook != null && rook.isFirstMove && isFirstMove) {
                    return board.getPieces(1, row) == null &&
                            board.getPieces(2, row) == null &&
                            board.getPieces(3, row) == null &&
                            !board.checkScanner.isKingChecked(new Move(board, this, 3, row));
                }
            }
        }
        return false;
    }
}
