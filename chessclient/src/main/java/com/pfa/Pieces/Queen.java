package com.pfa.Pieces;

import com.pfa.Main.Board;
import javafx.scene.image.Image;

public class Queen extends Pieces {
    public Queen(Board board, int col, int row, boolean isWhite) {
        super(board);
        this.col = col;
        this.row = row;
        this.xPos = col * board.tileSize;
        this.yPos = row * board.tileSize;
        this.isWhite = isWhite;
        this.name = "Queen";

        // Use consistent image loading approach
        String path = isWhite ? "wQueen.png" : "bQueen.png";
        this.sprite = loadAndSizeImage(path, board.tileSize);
    }

    public boolean isValidMovement(int col, int row) {
        return this.col == col || this.row == row || Math.abs(this.col - col) == Math.abs(this.row - row);
    }

    public boolean MoveCollides(int col, int row) {
        if (this.col == col || this.row == row) {
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
        } else {
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
        }
        return false;
    }
}
