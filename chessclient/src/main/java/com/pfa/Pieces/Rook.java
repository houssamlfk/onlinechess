package com.pfa.Pieces;

import com.pfa.Main.Board;
import javafx.scene.image.Image;

public class Rook extends Pieces {
    public Rook(Board board, int col, int row, boolean isWhite) {
        super(board);
        this.col = col;
        this.row = row;
        this.xPos = col * board.tileSize;
        this.yPos = row * board.tileSize;
        this.isWhite = isWhite;
        this.name = "Rook";

        // Use consistent image loading approach
        String path = isWhite ? "zwTower.png" : "bTower.png";
        this.sprite = loadAndSizeImage(path, board.tileSize);
    }

    public boolean isValidMovement(int col, int row) {
        return this.col == col || this.row == row;
    }

    public boolean MoveCollides(int col, int row) {
        if (this.col > col)
            for (int c = this.col - 1; c > col; c--) {
                if (board.getPieces(c, this.row) != null)
                    return true;
            }
        if (this.col < col)
            for (int c = this.col + 1; c < col; c++) {
                if (board.getPieces(c, this.row) != null)
                    return true;
            }
        if (this.row < row)
            for (int r = this.row + 1; r < row; r++) {
                if (board.getPieces(this.col, r) != null)
                    return true;
            }
        if (this.row > row)
            for (int r = this.row - 1; r > row; r--) {
                if (board.getPieces(this.col, r) != null)
                    return true;
            }
        return false;
    }
}
