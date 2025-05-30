package com.pfa.Main;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfa.AI.AIController;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.animation.PauseTransition;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.util.Duration;

public class App extends Application {
    private Stage primaryStage;
    private Stage connectionStage;
    private BorderPane gamePanel;
    private Board board;
    private HBox turnIndicatorPanel;
    private String onlineUsername;
    private Label turnLabel;
    private String playerName = "Player";
    private String opponentName = "Player 2";
    private int playerELO = 0;
    private int playerGamesPlayed = 0;
    private Label eloLabel;
    private VBox playerInfoPanel;
    private boolean isConnected = false;
    private String onlineloginId;
    // online client part
    // WebSocketClient client = new StandardWebSocketClient();

    // WebSocketStompClient stompClient = new WebSocketStompClient(client);
    // stompClient.setMessageConverter(new MappingJackson2MessageConverter());

    // StompSessionHandler sessionHandler = new StompSessionHandler();
    // stompClient.connect(URL, sessionHandler);

    // new Scanner(System.in).nextLine();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Chess Game");
        primaryStage.setMaximized(true);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(800);
        primaryStage.centerOnScreen();

        showMainMenu();

        primaryStage.show();
    }

    private void showMainMenu() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #282828;");

        VBox menuPanel = new VBox(20);
        menuPanel.setAlignment(Pos.CENTER);
        menuPanel.setPadding(new Insets(100, 0, 100, 0));
        menuPanel.setStyle("-fx-background-color: #282828;");

        Label titleLabel = new Label("Chess Game");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        titleLabel.setTextFill(Color.WHITE);

        FadeTransition titleFade = new FadeTransition(Duration.millis(1000), titleLabel);
        titleFade.setFromValue(0.0);
        titleFade.setToValue(1.0);
        titleFade.play();

        menuPanel.getChildren().add(titleLabel);

        Region spacer = new Region();
        spacer.setPrefHeight(60);
        menuPanel.getChildren().add(spacer);

        playerInfoPanel = new VBox(5);
        playerInfoPanel.setAlignment(Pos.CENTER);
        playerInfoPanel.setMaxWidth(400);

        Label eloDisplayLabel = new Label("ELO: " + playerELO);
        eloDisplayLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        eloDisplayLabel.setTextFill(Color.GOLD);

        Label gamesPlayedLabel = new Label("Games Played: " + playerGamesPlayed);
        gamesPlayedLabel.setFont(Font.font("Arial", 18));
        gamesPlayedLabel.setTextFill(Color.LIGHTGRAY);

        playerInfoPanel.getChildren().addAll(eloDisplayLabel, gamesPlayedLabel);
        menuPanel.getChildren().add(playerInfoPanel);

        HBox namePanel = new HBox(10);
        namePanel.setAlignment(Pos.CENTER);
        namePanel.setMaxWidth(400);

        Label nameLabel = new Label("Your Name: ");
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setFont(Font.font("Arial", 18));
        namePanel.getChildren().add(nameLabel);

        TextField nameField = new TextField("Player");
        nameField.setFont(Font.font("Arial", 18));
        nameField.setPrefWidth(200);
        namePanel.getChildren().add(nameField);

        TranslateTransition namePanelSlide = new TranslateTransition(Duration.millis(800), namePanel);
        namePanelSlide.setFromX(-800);
        namePanelSlide.setToX(0);
        namePanelSlide.play();

        menuPanel.getChildren().add(namePanel);

        Region spacer2 = new Region();
        spacer2.setPrefHeight(30);
        menuPanel.getChildren().add(spacer2);

        Button pvpButton = new Button("Player vs Player");
        pvpButton.setPrefSize(300, 60);
        pvpButton.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        pvpButton.setOnAction(e -> startPVPGame(nameField.getText()));

        addButtonAnimation(pvpButton);

        Button onlineButton = new Button("Online Mode");
        onlineButton.setPrefSize(300, 60);
        onlineButton.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        onlineButton.setOnAction(e -> checkConnection());

        Button aiButton = new Button("Player vs Computer");
        aiButton.setPrefSize(300, 60);
        aiButton.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        aiButton.setOnAction(e -> showComputerOptions(nameField.getText()));

        addButtonAnimation(aiButton);

        addButtonWithFadeIn(menuPanel, pvpButton, 0.5);
        addButtonWithFadeIn(menuPanel, onlineButton, 0.7);
        addButtonWithFadeIn(menuPanel, aiButton, 0.9);

        root.setCenter(menuPanel);

        Scene scene = new Scene(root, 1000, 800);
        primaryStage.setScene(scene);
    }

    private void addButtonAnimation(Button button) {
        button.setOnMouseEntered(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), button);
            scale.setToX(1.1);
            scale.setToY(1.1);
            scale.play();
        });

        button.setOnMouseExited(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), button);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.play();
        });
    }

    private void addButtonWithFadeIn(VBox parent, Button button, double delay) {
        button.setOpacity(0);
        parent.getChildren().add(button);

        FadeTransition fade = new FadeTransition(Duration.millis(500), button);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setDelay(Duration.seconds(delay));
        fade.play();
    }

    private void showComputerOptions(String playerName) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #282828;");

        VBox optionsPanel = new VBox(20);
        optionsPanel.setAlignment(Pos.CENTER);
        optionsPanel.setPadding(new Insets(100, 0, 100, 0));
        optionsPanel.setStyle("-fx-background-color: #282828;");
        optionsPanel.setOpacity(0);

        FadeTransition panelFade = new FadeTransition(Duration.millis(500), optionsPanel);
        panelFade.setFromValue(0.0);
        panelFade.setToValue(1.0);
        panelFade.play();

        Label titleLabel = new Label("Computer Game Options");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        titleLabel.setTextFill(Color.WHITE);
        optionsPanel.getChildren().add(titleLabel);

        Region spacer = new Region();
        spacer.setPrefHeight(60);
        optionsPanel.getChildren().add(spacer);

        Label nameDisplay = new Label("Playing as: " + (playerName.isEmpty() ? "Player" : playerName));
        nameDisplay.setFont(Font.font("Arial", 18));
        nameDisplay.setTextFill(Color.WHITE);
        optionsPanel.getChildren().add(nameDisplay);

        HBox colorPanel = new HBox(10);
        colorPanel.setAlignment(Pos.CENTER);
        colorPanel.setMaxWidth(400);

        Label colorLabel = new Label("Play as: ");
        colorLabel.setTextFill(Color.WHITE);
        colorLabel.setFont(Font.font("Arial", 18));
        colorPanel.getChildren().add(colorLabel);

        ComboBox<String> colorSelector = new ComboBox<>();
        colorSelector.getItems().addAll("White", "Black");
        colorSelector.setValue("White");
        colorSelector.setStyle("-fx-font-size: 18px;");
        colorPanel.getChildren().add(colorSelector);

        optionsPanel.getChildren().add(colorPanel);

        HBox diffPanel = new HBox(10);
        diffPanel.setAlignment(Pos.CENTER);
        diffPanel.setMaxWidth(400);

        Label diffLabel = new Label("Difficulty: ");
        diffLabel.setTextFill(Color.WHITE);
        diffLabel.setFont(Font.font("Arial", 18));
        diffPanel.getChildren().add(diffLabel);

        ComboBox<String> difficultySelector = new ComboBox<>();
        difficultySelector.getItems().addAll("Easy", "Medium", "Hard", "Expert");
        difficultySelector.setValue("Medium");
        difficultySelector.setStyle("-fx-font-size: 18px;");
        diffPanel.getChildren().add(difficultySelector);

        optionsPanel.getChildren().add(diffPanel);

        Region spacer2 = new Region();
        spacer2.setPrefHeight(60);
        optionsPanel.getChildren().add(spacer2);

        HBox buttonsPanel = new HBox(20);
        buttonsPanel.setAlignment(Pos.CENTER);

        Button startButton = new Button("Start Game");
        startButton.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        addButtonAnimation(startButton);
        startButton.setOnAction(e -> {
            boolean playAsWhite = colorSelector.getValue().equals("White");
            int difficulty = difficultySelector.getItems().indexOf(difficultySelector.getValue()) + 1;
            startComputerGame(playAsWhite, difficulty, playerName);
        });
        buttonsPanel.getChildren().add(startButton);

        Button backButton = new Button("Back to Menu");
        backButton.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        addButtonAnimation(backButton);
        backButton.setOnAction(e -> showMainMenu());
        buttonsPanel.getChildren().add(backButton);

        optionsPanel.getChildren().add(buttonsPanel);

        root.setCenter(optionsPanel);

        Scene scene = new Scene(root, 1000, 800);
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
    }

    private void startPVPGame(String playerName) {
        setupGameScreen(playerName, "Player 2", false, "", "", "");
        board.reset();
        board.setAIController(null);
        updateTurnIndicator(null);
    }

    private void startOnlineGame(String matchId, String username, String opponentName, String whiteName,
            String playerElo, String opponentElo) {
        this.playerName = username;
        this.onlineUsername = username;
        this.opponentName = opponentName;
        Platform.runLater(() -> {
            if (username.equals(whiteName)) {
                setupGameScreen(username, opponentName, true, matchId, playerElo, opponentElo);
                board.reset();
                board.isWaiting = false;
                board.setAIController(null);
                board.isOnlineWhite = true;
                updateTurnIndicatorOnline(null, username, opponentName);
            } else {
                setupGameScreen(opponentName, username, true, matchId, playerElo, opponentElo);
                board.reset();
                board.setAIController(null);
                board.isOnlineWhite = false;
                updateTurnIndicatorOnline(null, opponentName, username);
                try {
                    board.waitForMove();
                } catch (Exception ex) {
                    System.out.println("Exception found when trying to run wait for move for the first time !");
                }
            }
            // ArrayList<String> moves = board.movesList;
        });
    }

    private void checkConnection() {

        BorderPane root = new BorderPane();

        VBox credentialsPanel = new VBox(20);
        credentialsPanel.setAlignment(Pos.CENTER);
        credentialsPanel.setPadding(new Insets(100, 150, 100, 150));
        credentialsPanel.setStyle("-fx-background-color: #282828;");
        credentialsPanel.setOpacity(1);

        TextField loginField = new TextField("login");
        loginField.setFont(Font.font("Arial", 18));
        loginField.setPrefWidth(100);
        credentialsPanel.getChildren().add(loginField);

        TextField passwordField = new TextField("password");
        passwordField.setFont(Font.font("Arial", 18));
        passwordField.setPrefWidth(100);
        credentialsPanel.getChildren().add(passwordField);

        Label connectionLabel = new Label("");
        connectionLabel.setFont(Font.font("Arial", 18));
        connectionLabel.setTextFill(Color.rgb(20, 20, 255));
        connectionLabel.setPrefWidth(400);
        credentialsPanel.getChildren().add(connectionLabel);

        Button loginButton = new Button("Login");
        loginButton.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        loginButton.setPrefWidth(200);
        addButtonAnimation(loginButton);
        loginButton.setOnAction(e -> {
            try {
                authenticate(loginField.getText(), passwordField.getText(), connectionLabel);
            } catch (Exception ex) {
                System.out.println("please enter credentials");
            }
        });
        credentialsPanel.getChildren().add(loginButton);

        Button signupButton = new Button("signup");
        signupButton.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        addButtonAnimation(signupButton);
        signupButton.setPrefWidth(200);
        signupButton.setOnAction(e -> signupAction(""));
        credentialsPanel.getChildren().add(signupButton);

        Button mainButton = new Button("Main Menu");
        mainButton.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        addButtonAnimation(mainButton);
        mainButton.setPrefWidth(200);
        mainButton.setOnAction(e -> showMainMenu());
        credentialsPanel.getChildren().add(mainButton);

        root.setCenter(credentialsPanel);
        Scene scene = new Scene(root, 1000, 800);
        primaryStage.setScene(scene);
    }

    public void setConnected(boolean isConnected) {
        this.isConnected = isConnected;
    }

    public void loginAction(String loginId, String elo) {
        onlineloginId = loginId;

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(100, 150, 100, 150));

        VBox accountPanel = new VBox(20);
        accountPanel.setAlignment(Pos.CENTER);
        accountPanel.setPadding(new Insets(100, 150, 100, 150));
        accountPanel.setStyle("-fx-background-color: #440044;");
        accountPanel.setOpacity(1);

        Label userLabel = new Label("logged in as " + loginId + " !");
        userLabel.setFont(Font.font("Arial", 24));
        userLabel.setAlignment(Pos.CENTER);
        userLabel.setTextFill(Color.rgb(20, 20, 255));
        userLabel.setPrefWidth(400);
        accountPanel.getChildren().add(userLabel);

        Label eloLabel = new Label("Elo : " + elo);
        eloLabel.setFont(Font.font("Arial", 20));
        eloLabel.setAlignment(Pos.CENTER);
        eloLabel.setTextFill(Color.rgb(255, 255, 255));
        eloLabel.setPrefWidth(400);
        accountPanel.getChildren().add(eloLabel);

        Label queueLabel = new Label("");
        queueLabel.setFont(Font.font("Arial", 18));
        queueLabel.setAlignment(Pos.CENTER);
        queueLabel.setTextFill(Color.rgb(20, 20, 255));
        queueLabel.setPrefWidth(400);
        accountPanel.getChildren().add(queueLabel);

        Button queueButton = new Button("Queue up!");
        queueButton.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        addButtonAnimation(queueButton);
        queueButton.setOnAction(e -> {
            queueLabel.setText("in queue...");

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    queueUp(loginId);
                    return null;
                }
            };

            task.setOnFailed(ex -> {
                queueLabel.setText("error when queuing up!");
            });

            task.setOnSucceeded(ex -> {
                Task<Void> secondtask = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        waitForMatch(loginId);
                        return null;
                    }
                };
                secondtask.setOnFailed(exx -> {
                    queueLabel.setText("error when trying to find a match!");
                });

                new Thread(secondtask).start();
            });
            new Thread(task).start();
        });
        accountPanel.getChildren().add(queueButton);

        root.setCenter(accountPanel);
        Scene scene = new Scene(root, 1000, 800);
        scene.setFill(Color.rgb(0, 0, 0));
        primaryStage.setScene(scene);
    }

    public void queueUp(String loginId) {
        HttpClient httpclient = HttpClients.createDefault();
        final String myjson = "{\"loginId\":\"" + loginId + "\"}";
        HttpPost queue = new HttpPost("http://localhost:8080/queue");
        StringEntity queueEntity = new StringEntity(myjson, ContentType.APPLICATION_JSON);
        queue.setEntity(queueEntity);
        queue.setHeader("Accept", "application/json");
        queue.setHeader("Content-type", "application/json");
        try {
            HttpResponse queueResponse = httpclient.execute(queue);
        } catch (Exception ex) {
            System.out.println("no response when queuing up!");
        }
    }

    public void waitForMatch(String loginId) throws Exception {
        HttpClient httpclient = HttpClients.createDefault();
        while (true) {
            final String myjson = "{\"loginId\":\"" + loginId + "\"}";
            HttpPost match = new HttpPost("http://localhost:8080/check-match");
            StringEntity matchEntity = new StringEntity(myjson, ContentType.APPLICATION_JSON);
            match.setEntity(matchEntity);
            match.setHeader("Accept", "application/json");
            match.setHeader("Content-type", "application/json");
            HttpResponse matchResponse = httpclient.execute(match);
            HttpEntity matchResponseEntity = matchResponse.getEntity();
            if (matchResponseEntity != null) {
                try (InputStream instream = matchResponseEntity.getContent()) {
                    Scanner sc = new Scanner(instream);
                    String json = sc.nextLine();
                    ObjectMapper mapper = new ObjectMapper();
                    Map<?, ?> map = mapper.readValue(json, Map.class);
                    String id = (String) map.get("matchId");
                    String whiteName = (String) map.get("whiteName");
                    String opponentName = (String) map.get("opponentName");
                    String username = (String) map.get("username");
                    String playerElo = (String) map.get("playerElo");
                    String opponentElo = (String) map.get("opponentElo");
                    sc.close();
                    if (id.compareTo("-1") != 0) {
                        startOnlineGame(id, username, opponentName, whiteName, playerElo, opponentElo);
                        break;
                    }
                } catch (Exception e) {
                    System.out.println("No response when waiting for match!");
                }
            }
            Thread.sleep(2000);
        }
    }

    public void signupAction(String labelStatus) {
        BorderPane root = new BorderPane();

        VBox credentialsPanel = new VBox(20);
        credentialsPanel.setAlignment(Pos.CENTER);
        credentialsPanel.setPadding(new Insets(100, 150, 100, 150));
        credentialsPanel.setStyle("-fx-background-color: #282828;");
        credentialsPanel.setOpacity(1);

        TextField usernameField = new TextField("username");
        usernameField.setFont(Font.font("Arial", 18));
        usernameField.setPrefWidth(200);
        credentialsPanel.getChildren().add(usernameField);

        TextField loginField = new TextField("login");
        loginField.setFont(Font.font("Arial", 18));
        loginField.setPrefWidth(200);
        credentialsPanel.getChildren().add(loginField);

        TextField passwordField = new TextField("password");
        passwordField.setFont(Font.font("Arial", 18));
        passwordField.setPrefWidth(200);
        credentialsPanel.getChildren().add(passwordField);

        Button signupButton = new Button("signup");
        signupButton.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        addButtonAnimation(signupButton);
        signupButton.setOnAction(e -> {
            try {
                signupRequest(loginField.getText(), passwordField.getText(), usernameField.getText());
            } catch (Exception ex) {
                System.out.println("problem detected while signing up!");
            }

            checkConnection();
        });
        credentialsPanel.getChildren().add(signupButton);

        Label connectionLabel = new Label(labelStatus);
        connectionLabel.setFont(Font.font("Arial", 18));
        connectionLabel.setTextFill(Color.rgb(20, 20, 255));
        connectionLabel.setPrefWidth(400);
        credentialsPanel.getChildren().add(connectionLabel);

        Button mainButton = new Button("Main Menu");
        mainButton.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        addButtonAnimation(mainButton);
        mainButton.setOnAction(e -> showMainMenu());
        credentialsPanel.getChildren().add(mainButton);

        root.setCenter(credentialsPanel);
        Scene scene = new Scene(root, 1000, 800);
        primaryStage.setScene(scene);
    }

    public void signupRequest(String loginId, String password, String username) throws Exception {
        HttpClient httpclient = HttpClients.createDefault();
        final String myjson = "{\"username\":\"" + username + "\",\"loginId\":\"" + loginId + "\",\"password\":\""
                + password + "\"}";
        HttpPost signup = new HttpPost("http://localhost:8080/signup");
        StringEntity singupEntity = new StringEntity(myjson, ContentType.APPLICATION_JSON);
        signup.setEntity(singupEntity);
        signup.setHeader("Accept", "application/json");
        signup.setHeader("Content-type", "application/json");
        HttpResponse signupResponse = httpclient.execute(signup);
        HttpEntity signupResponseEntity = signupResponse.getEntity();
        if (signupResponseEntity != null) {
            try (InputStream instream = signupResponseEntity.getContent()) {
                Scanner sc = new Scanner(instream);
                if (sc.next().compareTo("user is already signed up !") == 0) {
                    signupAction("user is already signed up !");
                } else {
                    checkConnection();
                }
                sc.close();
            } catch (Exception e) {
                System.out.println("No reponse from Server when trying to sign up!");
            }
        }
        setConnected(true);

    }

    public void authenticate(String loginId, String password, Label status) throws Exception {
        HttpClient httpclient = HttpClients.createDefault();
        final String myjson = "{\"loginId\":\"" + loginId + "\",\"password\":\"" + password + "\"}";
        HttpPost login = new HttpPost("http://localhost:8080/login");
        StringEntity loginEntity = new StringEntity(myjson, ContentType.APPLICATION_JSON);
        login.setEntity(loginEntity);
        login.setHeader("Accept", "application/json");
        login.setHeader("Content-type", "application/json");
        HttpResponse loginResponse = httpclient.execute(login);
        HttpEntity loginResponseEntity = loginResponse.getEntity();
        if (loginResponseEntity != null) {
            try (InputStream instream = loginResponseEntity.getContent()) {
                Scanner sclogin = new Scanner(instream);
                String json = sclogin.nextLine();
                ObjectMapper mapper = new ObjectMapper();
                Map<?, ?> map = mapper.readValue(json, Map.class);
                status.setText((String) map.get("response"));
                if (status.getText().compareTo("successfully authenticated as " + loginId + "!") == 0) {
                    // setConnected(true);
                    System.out.println("logged in!");
                    loginAction(loginId, map.get("elo").toString());
                }
                sclogin.close();
            } catch (Exception e) {
                System.out.println("No reponse from Server when trying to log in!");
            }
        }
    }

    private void startComputerGame(boolean playAsWhite, int difficulty, String playerName) {
        String opponentName = "Computer";
        setupGameScreen(playAsWhite ? playerName : opponentName, playAsWhite ? opponentName : playerName, false, "", "",
                "");
        board.reset();

        AIController aiController = new AIController(board, !playAsWhite, difficulty);
        board.setAIController(aiController);
        aiController.setActive(true);

        updateTurnIndicator(null);

        if (!playAsWhite && board.isWhitetoMove) {
            PauseTransition pause = new PauseTransition(Duration.millis(500));
            pause.setOnFinished(event -> {
                aiController.makeAIMove();
                updateTurnIndicator(null); // Update after AI's move
            });
            pause.play();
        }
    }

    private void setupGameScreen(String whiteName, String blackName, boolean isOnline, String matchId,
            String playerElo,
            String opponentElo) {
        System.out.println(matchId);
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #000000;");

        gamePanel = new BorderPane();
        gamePanel.setStyle("-fx-background-color: #000000;");

        this.playerName = (whiteName == null || whiteName.trim().isEmpty()) ? "Player" : whiteName;
        this.opponentName = (blackName == null || blackName.trim().isEmpty()) ? "Player 2" : blackName;

        if (board == null && !isOnline) {
            board = new Board(isOnline, matchId, "");
        } else if (board == null && isOnline) {
            board = new Board(isOnline, matchId, this.onlineUsername);
        }

        StackPane boardWrapper = new StackPane();
        boardWrapper.setAlignment(Pos.CENTER);
        boardWrapper.getChildren().add(board);

        boardWrapper.setMinSize(board.cols * board.tileSize, board.rows * board.tileSize);
        boardWrapper.setPadding(new Insets(20));

        board.setOnMoveExecuted((gameResult) -> {
            if (gameResult != null) {
                showGameOverDialog(gameResult);
            }
            if (isOnline) {
                updateTurnIndicatorOnline(gameResult, whiteName, blackName);
            } else {
                updateTurnIndicator(gameResult);
            }

        });

        turnIndicatorPanel = new HBox();
        turnIndicatorPanel.setPrefHeight(50);
        turnIndicatorPanel.setAlignment(Pos.CENTER);
        turnIndicatorPanel.setStyle("-fx-background-color: #DCDCDC;");

        turnLabel = new Label(playerName + " (White's Turn)");
        turnLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        turnLabel.setTextFill(Color.rgb(50, 50, 50));
        turnIndicatorPanel.getChildren().add(turnLabel);

        VBox leftPanel = new VBox(20);
        leftPanel.setAlignment(Pos.TOP_CENTER);
        leftPanel.setPadding(new Insets(20));
        leftPanel.setStyle("-fx-background-color: #1E1E1E;");
        leftPanel.setPrefWidth(180);

        HBox eloBox = new HBox(5);
        eloBox.setAlignment(Pos.CENTER);
        Label eloPrefix = new Label("ELO: ");
        eloPrefix.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        eloPrefix.setTextFill(Color.WHITE);

        eloLabel = new Label((isOnline) ? playerElo : "0");
        eloLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        eloLabel.setTextFill(Color.GOLD);

        eloBox.getChildren().addAll(eloPrefix, eloLabel);
        leftPanel.getChildren().add(eloBox);

        if (isOnline) {
            HBox eloBoxO = new HBox(5);
            eloBox.setAlignment(Pos.CENTER);
            Label eloPrefixO = new Label("opponent ELO: ");
            eloPrefixO.setFont(Font.font("Arial", FontWeight.BOLD, 18));
            eloPrefixO.setMinWidth(100);
            eloPrefixO.setTextFill(Color.WHITE);

            Label eloLabelO = new Label(opponentElo);
            eloLabelO.setFont(Font.font("Arial", FontWeight.BOLD, 24));
            eloLabelO.setMinWidth(50);
            eloLabelO.setTextFill(Color.GOLD);

            eloBoxO.getChildren().addAll(eloPrefixO, eloLabelO);
            leftPanel.getChildren().add(eloBoxO);
        }

        Button backButton = new Button("Back to Menu");
        backButton.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        backButton.setPrefSize(160, 50);
        addButtonAnimation(backButton);
        backButton.setOnAction(e -> showMainMenu());

        leftPanel.getChildren().add(backButton);

        VBox rightPanel = new VBox(20);
        rightPanel.setAlignment(Pos.TOP_CENTER);
        rightPanel.setPadding(new Insets(20));
        rightPanel.setStyle("-fx-background-color: #1E1E1E;");
        rightPanel.setPrefWidth(180);
        if (!isOnline) {
            Button resetButton = new Button("RESET");
            resetButton.setFont(Font.font("Arial", FontWeight.BOLD, 20));
            resetButton.setTextFill(Color.WHITE);
            resetButton.setStyle("-fx-background-color: #B40000;");
            resetButton.setPrefSize(160, 80);
            addButtonAnimation(resetButton);
            resetButton.setOnAction(e -> {
                board.reset();
                updateTurnIndicator(null);

                if (board.getAIController() != null &&
                        !board.getAIController().aiPlaysWhite &&
                        board.isWhitetoMove) {
                    PauseTransition pause = new PauseTransition(Duration.millis(500));
                    pause.setOnFinished(event -> {
                        board.getAIController().makeAIMove();
                        updateTurnIndicator(null); // Update after AI's move
                    });
                    pause.play();
                }
            });

            rightPanel.getChildren().add(resetButton);

        }
        gamePanel.setTop(turnIndicatorPanel);
        gamePanel.setCenter(boardWrapper);
        gamePanel.setLeft(leftPanel);
        gamePanel.setRight(rightPanel);

        root.setCenter(gamePanel);

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
    }

    private void updateTurnIndicator(String gameResult) {
        Platform.runLater(() -> {
            if (gameResult != null) {
                turnIndicatorPanel.setStyle("-fx-background-color: #8B0000;"); // Dark red for game over
                turnLabel.setText(gameResult);
                turnLabel.setTextFill(Color.WHITE);
            } else if (board.isWhitetoMove) {
                turnIndicatorPanel.setStyle("-fx-background-color: #DCDCDC;");
                String name = (board.getAIController() != null && board.getAIController().aiPlaysWhite) ? opponentName
                        : playerName;
                turnLabel.setText(name + " (White's Turn)");
                turnLabel.setTextFill(Color.rgb(50, 50, 50));
            } else {
                turnIndicatorPanel.setStyle("-fx-background-color: #323232;");
                String name = (board.getAIController() != null && !board.getAIController().aiPlaysWhite) ? opponentName
                        : playerName;
                turnLabel.setText(name + " (Black's Turn)");
                turnLabel.setTextFill(Color.rgb(220, 220, 220));
            }
        });
    }

    private void updateTurnIndicatorOnline(String gameResult, String whiteName, String blackName) {
        Platform.runLater(() -> {
            if (gameResult != null) {
                turnIndicatorPanel.setStyle("-fx-background-color: #8B0000;"); // Dark red for game over
                turnLabel.setText(gameResult);
                turnLabel.setTextFill(Color.WHITE);
            } else if (board.isWhitetoMove) {
                turnIndicatorPanel.setStyle("-fx-background-color: #DCDCDC;");
                if (board.isOnlineWhite) {
                    turnLabel.setText("you" + " (White's Turn)");
                } else {
                    turnLabel.setText(whiteName + " (White's Turn)");
                }
                turnLabel.setTextFill(Color.rgb(50, 50, 50));
            } else {
                turnIndicatorPanel.setStyle("-fx-background-color: #323232;");
                if (!board.isOnlineWhite) {
                    turnLabel.setText("you" + " (Black's Turn)");
                } else {
                    turnLabel.setText(blackName + " (Black's Turn)");
                }
                turnLabel.setTextFill(Color.rgb(220, 220, 220));
            }
        });
    }

    private void showGameOverDialog(String result) {
        Platform.runLater(() -> {
            if (board.isOnline) {

                Stage dialogStage = new Stage();
                dialogStage.initModality(Modality.APPLICATION_MODAL);
                dialogStage.initOwner(primaryStage);
                dialogStage.setTitle("Game Over");

                VBox dialogVbox = new VBox(20);
                dialogVbox.setPadding(new Insets(20));
                dialogVbox.setAlignment(Pos.CENTER);
                dialogVbox.setStyle("-fx-background-color: #323232;");

                Label resultLabel = new Label(result);
                resultLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
                resultLabel.setTextFill(Color.WHITE);
                resultLabel.setOpacity(0);

                FadeTransition resultFade = new FadeTransition(Duration.millis(500), resultLabel);
                resultFade.setFromValue(0.0);
                resultFade.setToValue(1.0);
                resultFade.play();

                Button queueSceneButton = new Button("Back to online menu");
                queueSceneButton.setFont(Font.font("Arial", FontWeight.BOLD, 16));
                addButtonAnimation(queueSceneButton);
                queueSceneButton.setOnAction(e -> {
                    String elo = "";
                    HttpClient httpclient = HttpClients.createDefault();
                    HttpGet endRequest = new HttpGet("http://localhost:8080/elo/" + onlineUsername);
                    try {
                        HttpResponse endResponse = httpclient.execute(endRequest);
                        HttpEntity endResponseEntity = endResponse.getEntity();
                        if (endResponseEntity != null) {
                            try (InputStream instream = endResponseEntity.getContent()) {
                                Scanner sc = new Scanner(instream);
                                elo = sc.nextLine();
                                System.out.println("received elo from server : " + elo);
                                sc.close();
                            } catch (Exception ex) {
                                System.out.println("No reponse from Server!");
                                ex.printStackTrace();
                            }
                        }

                    } catch (Exception ex2) {
                        System.out.println("exception when sending request");
                        ex2.printStackTrace();
                    }
                    loginAction(onlineloginId, elo);
                    dialogStage.close();
                });

                dialogVbox.getChildren().add(queueSceneButton);

                Scene dialogScene = new Scene(dialogVbox, 400, 200);
                dialogStage.setScene(dialogScene);
                dialogStage.show();
                return;
            }
            playerGamesPlayed++;

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(primaryStage);
            dialogStage.setTitle("Game Over");

            VBox dialogVbox = new VBox(20);
            dialogVbox.setPadding(new Insets(20));
            dialogVbox.setAlignment(Pos.CENTER);
            dialogVbox.setStyle("-fx-background-color: #323232;");

            Label resultLabel = new Label(result);
            resultLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
            resultLabel.setTextFill(Color.WHITE);
            resultLabel.setOpacity(0);

            FadeTransition resultFade = new FadeTransition(Duration.millis(500), resultLabel);
            resultFade.setFromValue(0.0);
            resultFade.setToValue(1.0);
            resultFade.play();

            Button playAgainButton = new Button("Play Again");
            playAgainButton.setFont(Font.font("Arial", FontWeight.BOLD, 16));
            addButtonAnimation(playAgainButton);
            playAgainButton.setOnAction(e -> {
                dialogStage.close();
                board.reset();
                updateTurnIndicator(null);

                if (board.getAIController() != null &&
                        !board.getAIController().aiPlaysWhite &&
                        board.isWhitetoMove) {
                    PauseTransition pause = new PauseTransition(Duration.millis(500));
                    pause.setOnFinished(event -> {
                        board.getAIController().makeAIMove();
                        updateTurnIndicator(null);
                    });
                    pause.play();
                }
            });

            Button mainMenuButton = new Button("Back to Main Menu");
            mainMenuButton.setFont(Font.font("Arial", FontWeight.BOLD, 16));
            addButtonAnimation(mainMenuButton);
            mainMenuButton.setOnAction(e -> {
                dialogStage.close();
                showMainMenu();
            });

            HBox buttonBox = new HBox(20);
            buttonBox.setAlignment(Pos.CENTER);
            buttonBox.getChildren().addAll(playAgainButton, mainMenuButton);

            dialogVbox.getChildren().addAll(resultLabel, buttonBox);

            Scene dialogScene = new Scene(dialogVbox, 400, 200);
            dialogStage.setScene(dialogScene);
            dialogStage.show();
        });
    }
}
