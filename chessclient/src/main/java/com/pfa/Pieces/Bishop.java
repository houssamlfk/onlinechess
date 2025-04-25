package com.pfa.Pieces;

import com.pfa.Main.Board;
import javafx.scene.image.Image;

public class Bishop extends Pieces {
    public Bishop(Board board, int col, int row, boolean isWhite) {
        super(board);
        this.col = col;
        this.row = row;
        this.xPos = col * board.tileSize;
        this.yPos = row * board.tileSize;
        this.isWhite = isWhite;
        this.name = "Bishop";

        // Use consistent image loading approach
        String path = isWhite ? "wbishop.png" : "bbishop.png";
        this.sprite = loadAndSizeImage(path, board.tileSize);
    }

    public boolean isValidMovement(int col, int row) {
        return Math.abs(this.col - col) == Math.abs(this.row - row);
    }

    public boolean MoveCollides(int col, int row) {
        if (this.col > col && this.row > row) {
            for (int i = 1; i < Math.abs(this.col - col); i++)
                if (board.getPieces(this.col - i, this.row - i) != null) {
                    return true;
                }
        }
        if (this.col < col && this.row > row) {
            for (int i = 1; i < Math.abs(this.col - col); i++)
                if (board.getPieces(this.col + i, this.row - i) != null) {
                    return true;
                }
        }
        if (this.col > col && this.row < row) {
            for (int i = 1; i < Math.abs(this.col - col); i++)
                if (board.getPieces(this.col - i, this.row + i) != null) {
                    return true;
                }
        }
        if (this.col < col && this.row < row) {
            for (int i = 1; i < Math.abs(this.col - col); i++)
                if (board.getPieces(this.col + i, this.row + i) != null) {
                    return true;
                }
        }
        return false;
    }
}
