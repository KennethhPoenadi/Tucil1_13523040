import javafx.application.Application;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class Main extends Application {
    private int N, M; // Ukuran papan
    private String caseType;
    private List<char[][]> pieces = new ArrayList<>();
    private TextArea textArea;
    private VBox boardContainer;
    private char[][]board;

    @Override
    public void start(Stage primaryStage) {
        Label titleLabel = new Label("Puzzler SIGMA Solver");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #333;");

        //textarea scrollpane
        textArea = new TextArea();
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefHeight(150);
        ScrollPane scrollPane = new ScrollPane(textArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(160);

        //container untuk papan
        boardContainer = new VBox();
        boardContainer.setAlignment(Pos.CENTER);
        boardContainer.setStyle("-fx-padding: 10; -fx-border-color: #aaa; -fx-border-width: 2px;");

        //tombolbwtload
        Button loadButton = new Button("Load File");
        styleButton(loadButton);
        loadButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                loadPuzzle(file);
                drawBoard();
            }
        });
        
        Button solveButton = new Button("🧩 Solve");
        styleButton(solveButton);
        solveButton.setOnAction(e -> {
            if (pieces.isEmpty()) {
                showAlert("Error", "Silakan load file terlebih dahulu!");
            } else {
                resetBoard(board);
                if (solvePuzzle(new ArrayList<>(pieces), board)) {
                    drawBoard();  // Update GUI dengan solusi
                    textArea.appendText("Puzzle berhasil diselesaikan!\n");
                    // Debug: print board ke console
                    for (int i = 0; i < board.length; i++) {
                        for (int j = 0; j < board[0].length; j++) {
                            System.out.print(board[i][j] + " ");
                        }
                        System.out.println();
                    }
                } else {
                    textArea.appendText("Tidak ada solusi untuk puzzle ini.\n");
                }
            }
        });

        HBox buttonBox = new HBox(10, loadButton, solveButton);
        buttonBox.setAlignment(Pos.CENTER);

        VBox layout = new VBox(10, titleLabel, scrollPane, buttonBox, boardContainer);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-padding: 20; -fx-background-color: #f8f8f8;");

        Scene scene = new Scene(layout, 650, 550);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Puzzle Solver SIGMA");
        primaryStage.show();
    }

    //loadpuzzlenya
    private void loadPuzzle(File file) {
        try (Scanner scanner = new Scanner(file)) {
            N = scanner.nextInt();
            M = scanner.nextInt();
            int P = scanner.nextInt();
            scanner.nextLine();
            caseType = scanner.nextLine().trim();

            if (!caseType.equals("DEFAULT")) {
                throw new IllegalArgumentException("Tipe kasus tidak valid. Harus 'DEFAULT', hehe ga bikin bonus yang lain.");
            }

            board = new char[N][M];
            //inisialisasi
            for (int i=0; i< N; i++) {
                for (int j=0; j < M;j++) {
                    board[i][j] = '.';
                    System.out.print(board[i][j]); //buangcotar
                    System.out.print(" ");//buangcotar
                }
                System.out.println("\n");//buangcotar
            }

            pieces.clear();

            List<String> shapeLines = new ArrayList<>();
            char currentChar = '\0'; //inisialisasi karakter pertama dl

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;

                char firstChar = line.charAt(0);

                if (currentChar == '\0' || firstChar == currentChar) {
                    //cek karakter pertama masih sama dengan yang sebelumnya jd lanjut ke piece yang sama
                    shapeLines.add(line);
                } else {
                    //cek karakter pertama beda jd simpen piece sebelumnya dan mulai yang baru
                    savePiece(shapeLines);
                    shapeLines.clear();
                    shapeLines.add(line);
                }
                currentChar = firstChar;
            }

            savePiece(shapeLines);

            //bwt debug dlu
            for (int i = 0; i < pieces.size(); i++) {
                System.out.println("Piece " + (i + 1) + ":");
                for (char[] row : pieces.get(i)) {
                    System.out.println(Arrays.toString(row));
                }
                System.out.println();
            }


        } catch (FileNotFoundException e) {
            showAlert("Error", "File tidak ditemukan.");
        } catch (IllegalArgumentException e) {
            showAlert("Error", e.getMessage());
        } catch (Exception e) {
            showAlert("Error", "Format file tidak valid.");
        }
    }

    // Fungsi untuk merotasi piece 90 derajat clockwise
    private char[][] rotasipiece(char[][] piece) {
        int rows = piece.length;
        int cols = piece[0].length;
        char[][] rotated = new char[cols][rows];
        
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                rotated[j][rows - 1 - i] = piece[i][j];
            }
        }
        return rotated;
    }

// Fungsi untuk mengecek apakah piece bisa ditaruh di posisi tertentu
    private boolean taruhPiece(char[][] board, char[][] piece, int startRow, int startCol) {
        int pieceRows = piece.length;
        int pieceCols = piece[0].length;
        
        // Cek apakah piece keluar dari board
        if (startRow + pieceRows > board.length || startCol + pieceCols > board[0].length) {
            return false;
        }
        
        // Cek apakah ada overlap dengan piece lain
        for (int i = 0; i < pieceRows; i++) {
            for (int j = 0; j < pieceCols; j++) {
                if (piece[i][j] != '.') {  // Jika bagian piece bukan kosong
                    if (board[startRow + i][startCol + j] != '.') {  // Dan board sudah terisi
                        return false;  // Berarti overlap
                    }
                }
            }
        }
        return true;
    }

    // Fungsi untuk menaruh piece di board
    private void placePiece(char[][] board, char[][] piece, int startRow, int startCol) {
        for (int i = 0; i < piece.length; i++) {
            for (int j = 0; j < piece[0].length; j++) {
                if (piece[i][j] != '.') {
                    board[startRow + i][startCol + j] = piece[i][j];
                }
            }
        }
    }

    // Fungsi untuk menghapus piece dari board
    private void removePiece(char[][] board, char[][] piece, int startRow, int startCol) {
        for (int i = 0; i < piece.length; i++) {
            for (int j = 0; j < piece[0].length; j++) {
                if (piece[i][j] != '.') {
                    board[startRow + i][startCol + j] = '.';
                }
            }
        }
    }

    // Fungsi rekursif untuk solve puzzle
    private boolean solvePuzzle(List<char[][]> remainingPieces, char[][] board) {
        // Jika semua piece sudah ditaruh, cek apakah board sudah terisi penuh
        if (remainingPieces.isEmpty()) {
            return isBoardComplete();
        }
        
        char[][] currentPiece = remainingPieces.get(0);
        List<char[][]> nextPieces = new ArrayList<>(remainingPieces.subList(1, remainingPieces.size()));
        
        char[][] rotatedPiece = currentPiece;
        for (int rotation = 0; rotation < 4; rotation++) {
            for (int i = 0; i < board.length; i++) {
                for (int j = 0; j < board[0].length; j++) {
                    if (taruhPiece(board, rotatedPiece, i, j)) {
                        placePiece(board, rotatedPiece, i, j);
                        
                        if (solvePuzzle(nextPieces, board)) {
                            return true;
                        }
                        
                        removePiece(board, rotatedPiece, i, j);
                    }
                }
            }
            rotatedPiece = rotasipiece(rotatedPiece);
        }
        
        return false;
    }

    private void savePiece(List<String> shapeLines) {
        if (shapeLines.isEmpty()) return;
    
        int rows = shapeLines.size();
        int cols = shapeLines.stream().mapToInt(String::length).max().orElse(0);
        char[][] pieceMatrix = new char[rows][cols];
    
        for (int i = 0; i < rows; i++) {
            String row = shapeLines.get(i);
            Arrays.fill(pieceMatrix[i], '.'); // Default kosong
            for (int j = 0; j < row.length(); j++) {
                pieceMatrix[i][j] = row.charAt(j);
            }
        }
    
        pieces.add(pieceMatrix);
    }

    private void resetBoard(char[][]board) {
        for (int i=0;i<board.length;i++) {
            for (int j=0;j<board[0].length;j++) {
                board[i][j] = '.';
            }
        }
    }
    
    //fungsi untuk menggambar papan puzzle di GUI
    private void drawBoard() {
        boardContainer.getChildren().clear();

        switch (caseType) {
            default:
                drawGridBoard();
                break;
        }
    }

    // Fungsi untuk menggambar papan kotak biasa
    private void drawGridBoard() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                StackPane cell = new StackPane();
                Rectangle rect = new Rectangle(35, 35, Color.WHITE);
                rect.setStroke(Color.BLACK);
                
                Label letter = new Label(String.valueOf(board[i][j]));
                if (board[i][j] != '.') {
                    // Beri warna berbeda untuk setiap huruf yang berbeda
                    Color pieceColor = Color.hsb(
                        (board[i][j] - 'A') * 25 % 360, // Hue
                        0.3, // Saturation
                        0.9  // Brightness
                    );
                    rect.setFill(pieceColor);
                    letter.setStyle("-fx-font-weight: bold");
                }
                
                cell.getChildren().addAll(rect, letter);
                grid.add(cell, j, i);
            }
        }
        boardContainer.getChildren().add(grid);
    }

    private boolean isBoardComplete() {
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                if (board[i][j] == '.') {
                    return false;
                }
            }
        }
        return true;
    }

    // Fungsi untuk menampilkan alert
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Fungsi untuk styling tombol
    private void styleButton(Button button) {
        button.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10;");
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #45a049; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10;"));
    }
    

    public static void main(String[] args) {
        launch(args);
    }
}