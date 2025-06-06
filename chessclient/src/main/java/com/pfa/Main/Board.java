package com.pfa.Main;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfa.AI.AIController;
import com.pfa.Pieces.*;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

public class Board extends StackPane {
    public int tileSize = 85;
    public int cols = 8;
    public int rows = 8;
    public ArrayList<Pieces> pieceList = new ArrayList<>();

    public ArrayList<String> movesList = new ArrayList<>();
    public Pieces SelectedPiece;

    private Canvas boardCanvas;
    private GraphicsContext gc;
    private AIController aiController;
    private boolean aiMoveInProgress = false;
    private Consumer<String> onMoveExecuted;

    public String matchId;
    public String onlineUsername;
    public boolean isOnlineWhite;
    public boolean isOnline;
    public int enPassantTile = -1;
    public boolean isGameOver = false;
    public boolean isWhitetoMove = true;
    public boolean isWaiting;

    public CheckScanner checkScanner = new CheckScanner(this);

    public Board(boolean isOnline, String matchId, String playerName) {
        // Set up the board size with fixed dimensions
        int boardWidth = cols * tileSize;
        int boardHeight = rows * tileSize;

        // Set fixed size for the board
        setPrefSize(boardWidth, boardHeight);
        setMaxSize(boardWidth, boardHeight);
        setMinSize(boardWidth, boardHeight);

        // Center the board within the StackPane
        setAlignment(javafx.geometry.Pos.CENTER);

        // Create canvas for drawing the board and pieces
        boardCanvas = new Canvas(boardWidth, boardHeight);
        gc = boardCanvas.getGraphicsContext2D();
        getChildren().add(boardCanvas);

        // Set up input handlers
        input inputHandler = new input(this);

        // Add pieces
        addPieces();

        // Initial draw
        draw();
        this.isOnline = isOnline;
        this.matchId = matchId;
        this.onlineUsername = playerName;
    }

    public void setOnMoveExecuted(Consumer<String> action) {
        this.onMoveExecuted = action;
    }

    public Pieces getPieces(int col, int row) {
        for (Pieces piece : pieceList) {
            if (piece.col == col && piece.row == row) {
                return piece;
            }
        }
        return null;
    }

    public void setAIController(AIController aiController) {
        this.aiController = aiController;
    }

    public AIController getAIController() {
        return this.aiController;
    }

    public Move parseMove(String textMove) {
        Move move = new Move();
        Map<Character, Integer> columnMapper = new HashMap<>();
        columnMapper.put('a', 0);
        columnMapper.put('b', 1);
        columnMapper.put('c', 2);
        columnMapper.put('d', 3);
        columnMapper.put('e', 4);
        columnMapper.put('f', 5);
        columnMapper.put('g', 6);
        columnMapper.put('h', 7);

        char c = textMove.charAt(0);
        if (c == 'O') {
            move.piece = getPieces(4, 7);
            move.oldrow = 7;
            move.oldcol = 4;
            if (textMove.length() > 3) {
                move.longCastle = true;
                move.newcol = 2;
                move.newrow = 7;
                return move;
            } else {
                move.shortCastle = true;
                move.newcol = 6;
                move.newrow = 7;
                return move;
            }
        } else if (c == 'o') {
            move.piece = getPieces(4, 0);
            move.oldrow = 0;
            move.oldcol = 4;
            if (textMove.length() > 3) {
                move.longCastle = true;
                move.newcol = 2;
                move.newrow = 0;
                return move;
            } else {
                move.shortCastle = true;
                move.newcol = 6;
                move.newrow = 0;
                return move;
            }
        } else {
            int offset = 0;
            int oldcol = columnMapper.get(textMove.charAt(0));
            int oldrow = Character.getNumericValue(textMove.charAt(1));
            System.out.println("(" + oldcol + ", " + oldrow + ")");
            move.oldcol = oldcol;
            move.oldrow = oldrow;
            move.piece = this.getPieces(oldcol, oldrow);
            if (textMove.charAt(2) == 'x') {
                offset = 1;
            }
            int newcol = columnMapper.get(textMove.charAt(2 + offset));
            int newrow = Character.getNumericValue(textMove.charAt(3 + offset));
            move.newcol = newcol;
            move.newrow = newrow;
            System.out.println("(" + newcol + ", " + newrow + ")");
            move.capture = this.getPieces(newcol, newrow);
            return move;
        }
    }

    public void MakeMove(Move move) throws Exception {
        String who = (isWhitetoMove) ? "white" : "black";

        if (move.piece.name.equals("Pawn")) {
            movePawn(move);
        } else if (move.piece.name.equals("King")) {
            moveKing(move);
        }

        move.piece.col = move.newcol;
        move.piece.row = move.newrow;
        move.piece.xPos = move.newcol * tileSize;
        move.piece.yPos = move.newrow * tileSize;
        System.out.println("the piece should move to : (" + move.piece.xPos + ", " + move.piece.yPos + ")");
        move.piece.isFirstMove = false;
        capture(move.capture);

        isWhitetoMove = !isWhitetoMove;

        String gameResult = null;
        updateGameState();

        // Force a redraw after each move
        draw();

        // Notify that a move was executed
        if (onMoveExecuted != null) {
            onMoveExecuted.accept(null);
        }
        // online mode
        if (isOnline && !isWaiting) {
            boolean isCapture = (move.capture == null) ? false : true;
            sendMove(who, move, isCapture, matchId);
            waitForMove();
            return;
        }
        if (isOnline && isWaiting) {
            isWaiting = false;
            return;
        }
        // Only trigger AI if it's active and the game isn't over
        if (aiController != null && aiController.isActive() && !isGameOver && !aiMoveInProgress) {
            // Don't immediately make an AI move - use a timer to add a small delay
            aiMoveInProgress = true;

            PauseTransition pause = new PauseTransition(Duration.millis(500));
            pause.setOnFinished(event -> {
                // Inside timer callback - now check if it's AI's turn
                if (!isGameOver &&
                        ((aiController.aiPlaysWhite && isWhitetoMove) ||
                                (!aiController.aiPlaysWhite && !isWhitetoMove))) {
                    // It's definitely AI's turn
                    aiController.makeAIMove();
                    // Redraw after AI move
                    draw();
                    // Notify about move
                    if (onMoveExecuted != null) {
                        onMoveExecuted.accept(null);
                    }
                }
                aiMoveInProgress = false;
            });
            pause.play();
        }
    }

    public void sendMove(String mover, Move move, boolean capture, String matchId) throws Exception {
        // parser
        HashMap<Integer, String> columnMapper = new HashMap<Integer, String>();
        HashMap<String, String> pieceMapper = new HashMap<String, String>();

        columnMapper.put(0, "a");
        columnMapper.put(1, "b");
        columnMapper.put(2, "c");
        columnMapper.put(3, "d");
        columnMapper.put(4, "e");
        columnMapper.put(5, "f");
        columnMapper.put(6, "g");
        columnMapper.put(7, "h");
        String translatedMove;
        String captureMarker = (capture) ? "x" : "";
        if (move.shortCastle || move.longCastle) {
            if (move.shortCastle) {
                if (move.piece.isWhite) {
                    translatedMove = "O-O";
                } else {
                    translatedMove = "o-o";
                }
            } else {
                if (move.piece.isWhite) {
                    translatedMove = "O-O-O";
                } else {
                    translatedMove = "o-o-o";
                }
            }
        } else {
            translatedMove = columnMapper.get(move.oldcol) + move.oldrow + captureMarker + columnMapper.get(move.newcol)
                    + move.newrow;
        }
        // online part
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                HttpClient httpclient = HttpClients.createDefault();
                String text = "nothing";
                HttpPost moveRequest = new HttpPost(
                        "http://localhost:8080/match/" + matchId + "/" + translatedMove + "/" + onlineUsername);
                StringEntity moveEntity = new StringEntity(text, ContentType.TEXT_PLAIN);
                moveRequest.setEntity(moveEntity);
                HttpResponse moveResponse = httpclient.execute(moveRequest);
                HttpEntity moveResponseEntity = moveResponse.getEntity();
                if (moveResponseEntity != null) {
                    try (InputStream instream = moveResponseEntity.getContent()) {
                        Scanner sc = new Scanner(instream);
                        String response = sc.nextLine();
                        System.out.println("server response : " + response);
                        sc.close();
                    } catch (Exception e) {
                        System.out.println("No response from Server when sending a move!");
                        e.printStackTrace();
                    }
                }
                return null;
            }
        };

        task.setOnFailed(ex -> {
            System.out.println("exception in task for sending move!");
            task.getException().printStackTrace();
        });

        new Thread(task).start();
    }

    private void moveKing(Move move) {
        if (Math.abs(move.piece.col - move.newcol) == 2) {
            Pieces rook;
            if (move.piece.col < move.newcol) {
                move.shortCastle = true;
                rook = getPieces(7, move.piece.row);
                rook.col = 5;
            } else {
                move.longCastle = true;
                rook = getPieces(0, move.piece.row);
                rook.col = 3;
            }
            rook.xPos = rook.col * tileSize;
        }
    }

    private void movePawn(Move move) {
        int colorIndex = move.piece.isWhite ? 1 : -1;
        if (getTilenumber(move.newcol, move.newrow) == enPassantTile) {
            move.capture = getPieces(move.newcol, move.newrow + colorIndex);
        }
        if (Math.abs(move.piece.row - move.newrow) == 2) {
            enPassantTile = getTilenumber(move.newcol, move.newrow + colorIndex);
        } else {
            enPassantTile = -1;
        }
        // promotion
        colorIndex = move.piece.isWhite ? 0 : 7;
        if (move.newrow == colorIndex) {
            promotePawn(move);
        }
    }

    private void promotePawn(Move move) {
        pieceList.add(new Queen(this, move.newcol, move.newrow, move.piece.isWhite));
        capture(move.piece);
        SoundPlayer.playSound("pawn level.wav");
    }

    public void capture(Pieces piece) {
        pieceList.remove(piece);
    }

    public void waitForMove() throws Exception {
        this.isWaiting = true;
        if (!isGameOver) {
            Task<Move> task = new Task<Move>() {
                @Override
                protected Move call() throws Exception {
                    while (true) {
                        String textMove = "x";
                        textMove = checkMoved();
                        if (!textMove.equals("x")) {
                            Move nextMove = new Move();
                            nextMove = parseMove(textMove);
                            return nextMove;
                        } else {
                            Thread.sleep(2000);
                        }
                    }
                }
            };

            task.setOnFailed(ex -> {
                System.out.println("exception in task for checking next move !");
                task.getException().printStackTrace();
            });
            task.setOnSucceeded(ex -> {
                try {
                    this.MakeMove(task.getValue());
                } catch (Exception exception) {
                    System.out.println("error when making next move from task");
                    exception.printStackTrace();
                }
            });
            new Thread(task).start();

        }
        return;
    }

    public String checkMoved() throws Exception {
        HttpClient httpclient = HttpClients.createDefault();
        HttpGet moveRequest = new HttpGet("http://localhost:8080/match/" + matchId + "/" + onlineUsername);
        System.out.println("trying to reach server with: " + matchId + "," + onlineUsername);
        HttpResponse moveResponse = httpclient.execute(moveRequest);
        HttpEntity moveResponseEntity = moveResponse.getEntity();
        if (moveResponseEntity != null) {
            try (InputStream instream = moveResponseEntity.getContent()) {
                Scanner sc = new Scanner(instream);
                String response = sc.nextLine();
                sc.close();
                System.out.println("received from server move :" + response);
                return response;
            } catch (Exception e) {
                System.out.println("No reponse from Server when checking for move!");
                e.printStackTrace();
            }
        }
        return "x";
    }

    public boolean isValidMove(Move move) {
        if (isGameOver) {
            return false;
        }
        if (move.piece.isWhite != isWhitetoMove) {
            return false;
        }
        if (sameTeam(move.piece, move.capture)) {
            return false;
        }
        if (!move.piece.isValidMovement(move.newcol, move.newrow)) {
            return false;
        }
        if (move.piece.MoveCollides(move.newcol, move.newrow)) {
            return false;
        }
        if (checkScanner.isKingChecked(move)) {
            return false;
        }
        return true;
    }

    public Pieces findKing(boolean isWhite) {
        for (Pieces piece : pieceList) {
            if (isWhite == piece.isWhite && piece.name.equals("King")) {
                return piece;
            }
        }
        return null;
    }

    public boolean sameTeam(Pieces p1, Pieces p2) {
        if (p1 == null || p2 == null) {
            return false;
        }
        return (p1.isWhite == p2.isWhite);
    }

    public int getTilenumber(int col, int row) {
        return row * rows + col;
    }

    public void addPieces() {
        pieceList.add(new King(this, 4, 0, false));
        pieceList.add(new Queen(this, 3, 0, false));
        pieceList.add(new Knight(this, 6, 0, false));
        pieceList.add(new Knight(this, 1, 0, false));
        pieceList.add(new Bishop(this, 2, 0, false));
        pieceList.add(new Bishop(this, 5, 0, false));
        pieceList.add(new Rook(this, 0, 0, false));
        pieceList.add(new Rook(this, 7, 0, false));

        pieceList.add(new King(this, 4, 7, true));
        pieceList.add(new Queen(this, 3, 7, true));
        pieceList.add(new Knight(this, 6, 7, true));
        pieceList.add(new Knight(this, 1, 7, true));
        pieceList.add(new Bishop(this, 2, 7, true));
        pieceList.add(new Bishop(this, 5, 7, true));
        pieceList.add(new Rook(this, 0, 7, true));
        pieceList.add(new Rook(this, 7, 7, true));

        pieceList.add(new Pawn(this, 0, 1, false));
        pieceList.add(new Pawn(this, 1, 1, false));
        pieceList.add(new Pawn(this, 2, 1, false));
        pieceList.add(new Pawn(this, 3, 1, false));
        pieceList.add(new Pawn(this, 4, 1, false));
        pieceList.add(new Pawn(this, 5, 1, false));
        pieceList.add(new Pawn(this, 6, 1, false));
        pieceList.add(new Pawn(this, 7, 1, false));

        pieceList.add(new Pawn(this, 0, 6, true));
        pieceList.add(new Pawn(this, 1, 6, true));
        pieceList.add(new Pawn(this, 2, 6, true));
        pieceList.add(new Pawn(this, 3, 6, true));
        pieceList.add(new Pawn(this, 4, 6, true));
        pieceList.add(new Pawn(this, 5, 6, true));
        pieceList.add(new Pawn(this, 6, 6, true));
        pieceList.add(new Pawn(this, 7, 6, true));
    }

    private void updateGameState() throws Exception {
        Pieces king = findKing(isWhitetoMove);
        String gameResult = null;

        if (king == null) {
            // King not found - likely captured, which shouldn't happen
            isGameOver = true;
            gameResult = isWhitetoMove ? "Black wins" : "White wins";

            System.out.println(gameResult);

            // Notify that a game result is available if callback is set
            if (onMoveExecuted != null) {
                onMoveExecuted.accept(gameResult);
            }
            return;
        }

        if (checkScanner.isGameOver(king)) {
            isGameOver = true;
            if (checkScanner.isKingChecked(new Move(this, king, king.col, king.row))) {
                gameResult = isWhitetoMove ? "Black wins" : "White wins";
                if (isOnline) {
                    String color = isWhitetoMove ? "Black" : "White";
                    HttpClient httpclient = HttpClients.createDefault();
                    HttpGet endRequest = new HttpGet("http://localhost:8080/match-result/" + matchId + "/" + color);
                    HttpResponse endResponse = httpclient.execute(endRequest);
                    HttpEntity endResponseEntity = endResponse.getEntity();
                    if (endResponseEntity != null) {
                        try (InputStream instream = endResponseEntity.getContent()) {
                            Scanner sc = new Scanner(instream);
                            String response = sc.nextLine();
                            System.out.println(response);
                            sc.close();
                        } catch (Exception e) {
                            System.out.println("No reponse from Server!");
                            e.printStackTrace();
                        }
                    }
                }

                System.out.println(gameResult);
            } else {
                gameResult = "Stalemate";
                System.out.println(gameResult);
            }

            // Notify that a game result is available if callback is set
            if (onMoveExecuted != null) {
                onMoveExecuted.accept(gameResult);
            }
        } else if (insufficientPieces(true) && insufficientPieces(false)) {
            isGameOver = true;
            gameResult = "Insufficient Material!";
            System.out.println(gameResult);

            // Notify that a game result is available if callback is set
            if (onMoveExecuted != null) {
                onMoveExecuted.accept(gameResult);
            }
        }
    }

    private boolean insufficientPieces(boolean isWhite) {
        ArrayList<String> names = pieceList.stream()
                .filter(p -> p.isWhite == isWhite)
                .map(p -> p.name)
                .collect(Collectors.toCollection(ArrayList::new));
        if (names.contains("Queen") || names.contains("Pawn") || names.contains("Rook")) {
            return false;
        }
        return names.size() < 3;
    }

    public void draw() {
        // Clear the canvas
        gc.clearRect(0, 0, boardCanvas.getWidth(), boardCanvas.getHeight());

        // Draw the checkered board
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                gc.setFill((c + r) % 2 == 0 ? Color.rgb(4, 121, 250) : Color.rgb(255, 255, 255));
                gc.fillRect(c * tileSize, r * tileSize, tileSize, tileSize);
            }
        }

        // Highlight valid moves for selected piece
        if (SelectedPiece != null) {
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (isValidMove(new Move(this, SelectedPiece, c, r))) {
                        gc.setFill(Color.rgb(255, 0, 0, 0.5));
                        gc.fillRect(c * tileSize, r * tileSize, tileSize, tileSize);
                    }
                }
            }
        }

        // Draw all pieces
        for (Pieces piece : pieceList) {
            if (piece != SelectedPiece) { // Don't draw the selected piece yet, to ensure it's drawn on top
                piece.draw(gc);
            }
        }

        // Draw the selected piece on top
        if (SelectedPiece != null) {
            SelectedPiece.draw(gc);
        }
    }

    public void reset() {
        pieceList.clear();
        movesList.clear();
        addPieces();
        SelectedPiece = null;
        enPassantTile = -1;
        isGameOver = false;
        isWhitetoMove = true;
        aiMoveInProgress = false;
        // Don't reset aiController to null here as it breaks the reference
        draw();
    }
}
