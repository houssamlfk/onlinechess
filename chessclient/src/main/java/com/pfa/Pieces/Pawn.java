package com.pfa.Pieces;

import com.pfa.Main.Board;
import javafx.scene.image.Image;

public class Pawn extends Pieces {
    public Pawn(Board board, int col, int row, boolean isWhite) {
        super(board);
        this.col = col;
        this.row = row;
        this.xPos = col * board.tileSize;
        this.yPos = row * board.tileSize;
        this.isWhite = isWhite;
        this.name = "Pawn";

        // Use consistent image loading approach
        String path = isWhite ? "wPawn.png" : "bPawn.png";
        this.sprite = loadAndSizeImage(path, board.tileSize);
    }

    public boolean isValidMovement(int col, int row) {
        int colorIndex = isWhite ? 1 : -1;

        // push pawn 1
        if (this.col == col && row == this.row - colorIndex && board.getPieces(col, row) == null) {
            return true;
        }
        // push pawn 2
        if (isFirstMove && this.col == col && row == this.row - colorIndex * 2 &&
                board.getPieces(col, row) == null && // Check destination square is empty
                board.getPieces(col, row + colorIndex) == null) { // Check square in between is empty
            return true;
        }
        // capture left
        if (col == this.col - 1 && row == this.row - colorIndex && board.getPieces(col, row) != null) {
            return true;
        }
        // capture right
        if (col == this.col + 1 && row == this.row - colorIndex && board.getPieces(col, row) != null) {
            return true;
        }
        // en passant left
        if (board.getTilenumber(col, row) == board.enPassantTile &&
                col == this.col - 1 &&
                row == this.row - colorIndex &&
                board.getPieces(col, row + colorIndex) != null) {
            return true;
        }
        // en passant right
        if (board.getTilenumber(col, row) == board.enPassantTile &&
                col == this.col + 1 &&
                row == this.row - colorIndex &&
                board.getPieces(col, row + colorIndex) != null) {
            return true;
        }

        return false;
    }

    @Override
    public boolean MoveCollides(int col, int row) {
        // Pawns don't need special collision detection beyond what's in isValidMovement
        return false;
    }
}
