package com.pfa.Pieces;

import com.pfa.Main.Board;
import javafx.scene.image.Image;

public class Knight extends Pieces {
    public Knight(Board board, int col, int row, boolean isWhite) {
        super(board);
        this.col = col;
        this.row = row;
        this.xPos = col * board.tileSize;
        this.yPos = row * board.tileSize;
        this.isWhite = isWhite;
        this.name = "Knight";

        // Use consistent image loading approach
        String path = isWhite ? "wKnight.png" : "bKnight.png";
        this.sprite = loadAndSizeImage(path, board.tileSize);
    }

    public boolean isValidMovement(int col, int row) {
        return Math.abs(col - this.col) * Math.abs(row - this.row) == 2;
    }

    @Override
    public boolean MoveCollides(int col, int row) {
        return false;
    }
}
