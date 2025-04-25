package com.pfa.Main;

import com.pfa.Pieces.Pieces;
import com.pfa.Pieces.SoundPlayer;

public class Move {
    public int oldcol;
    public int oldrow;

    public int newcol;
    public int newrow;
    public Pieces piece;
    public Pieces capture;

    public Move(Board board, Pieces piece, int newcol, int newrow) {
        this.oldcol = piece.col;
        this.oldrow = piece.row;
        this.newcol = newcol;
        this.newrow = newrow;
        this.piece = piece;
        this.capture = board.getPieces(newcol, newrow);

    }

    public Move() {
    }
}
