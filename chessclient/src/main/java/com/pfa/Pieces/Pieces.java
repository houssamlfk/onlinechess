package com.pfa.Pieces;

import com.pfa.Main.Board;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public abstract class Pieces {
    public int col, row;
    public int xPos, yPos;
    public boolean isWhite;
    public boolean isFirstMove = true;
    public String name;
    public Image sprite;

    public Board board;

    public Pieces(Board board) {
        this.board = board;
    }

    public abstract boolean isValidMovement(int col, int row);

    public boolean MoveCollides(int col, int row) {
        return false;
    }

    public void draw(GraphicsContext gc) {
        gc.drawImage(sprite, xPos, yPos);
    }

    protected static Image loadImage(String path) {
        try {
            return new Image(Pieces.class.getResourceAsStream(path));
        } catch (Exception e) {
            System.err.println("Failed to load image: " + path);
            e.printStackTrace();
            return null;
        }
    }

    protected static Image loadAndSizeImage(String filename, int size) {
        try {
            String imagePath = "/Res/" + filename;
            System.out.println("Trying to load: " + imagePath);

            java.net.URL url = Pieces.class.getResource(imagePath);

            if (url == null) {
                imagePath = "Res/" + filename;
                url = Pieces.class.getResource(imagePath);
            }

            if (url == null) {
                System.err.println("Could not find resource: " + filename);
                return null;
            }

            Image img = new Image(url.toString());
            return new Image(img.getUrl(), size, size, true, true);
        } catch (Exception e) {
            System.err.println("Failed to load image: " + filename);
            e.printStackTrace();
            return null;
        }
    }
}
