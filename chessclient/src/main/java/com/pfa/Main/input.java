package com.pfa.Main;

import com.pfa.Pieces.Pieces;
import com.pfa.Pieces.SoundPlayer;
import javafx.scene.input.MouseEvent;

public class input {
    private Board board;
    private double dragOffsetX;
    private double dragOffsetY;

    public input(Board board) {
        this.board = board;

        board.setOnMousePressed(this::handleMousePressed);
        board.setOnMouseDragged(this::handleMouseDragged);
        board.setOnMouseReleased(this::handleMouseReleased);
    }

    private void handleMousePressed(MouseEvent e) {
        if (board.isGameOver || (board.getAIController() != null &&
                ((board.getAIController().aiPlaysWhite && board.isWhitetoMove) ||
                        (!board.getAIController().aiPlaysWhite && !board.isWhitetoMove)))) {
            return;
        }

        int col = (int) (e.getX() / board.tileSize);
        int row = (int) (e.getY() / board.tileSize);

        if (isInsideBoard(col, row)) {
            Pieces pieceXY = board.getPieces(col, row);

            if (pieceXY != null && pieceXY.isWhite == board.isWhitetoMove) {
                board.SelectedPiece = pieceXY;

                dragOffsetX = e.getX() - (pieceXY.col * board.tileSize);
                dragOffsetY = e.getY() - (pieceXY.row * board.tileSize);

                board.draw();
            }
        }
    }

    private void handleMouseDragged(MouseEvent e) {
        if (board.SelectedPiece != null) {
            Pieces piece = board.SelectedPiece;
            piece.xPos = (int) (e.getX() - dragOffsetX);
            piece.yPos = (int) (e.getY() - dragOffsetY);

            board.draw();
        }
    }

    private void handleMouseReleased(MouseEvent e) {
        if (board.SelectedPiece != null) {
            if (board.isGameOver) {
                returnPieceToOriginalPosition();
                board.draw();
                board.SelectedPiece = null;
                return;
            }

            int col = (int) (e.getX() / board.tileSize);
            int row = (int) (e.getY() / board.tileSize);

            if (isInsideBoard(col, row)) {
                Move move = new Move(board, board.SelectedPiece, col, row);

                if (board.isValidMove(move)) {
                    board.MakeMove(move);
                } else {
                    returnPieceToOriginalPosition();
                    SoundPlayer.playSound("error.wav");
                    board.draw();
                }
            } else {
                returnPieceToOriginalPosition();
                board.draw();
            }

            board.SelectedPiece = null;
        }
    }

    private void returnPieceToOriginalPosition() {
        if (board.SelectedPiece != null) {
            Pieces piece = board.SelectedPiece;
            piece.xPos = piece.col * board.tileSize;
            piece.yPos = piece.row * board.tileSize;
        }
    }

    private boolean isInsideBoard(int col, int row) {
        return col >= 0 && col < board.cols && row >= 0 && row < board.rows;
    }
}
