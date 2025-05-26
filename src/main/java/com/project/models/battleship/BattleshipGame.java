package com.project.models.battleship;

import java.io.Serializable;

// UPROSZCZONA WERSJA - tylko do odczytu stanu gry
public class BattleshipGame implements Serializable {
    private final String gameId;
    private GameState state;
    private int currentPlayer;
    private int winner;

    public BattleshipGame(String gameId) {
        this.gameId = gameId;
        this.state = GameState.WAITING_FOR_PLAYERS;
        this.currentPlayer = -1;
        this.winner = -1;
    }

    // Gettery - KLIENT tylko odczytuje stan
    public String getGameId() { return gameId; }
    public GameState getState() { return state; }
    public int getCurrentPlayer() { return currentPlayer; }
    public int getWinner() { return winner; }

    // Settery - używane przez deserializację
    public void setState(GameState state) { this.state = state; }
    public void setCurrentPlayer(int currentPlayer) { this.currentPlayer = currentPlayer; }
    public void setWinner(int winner) { this.winner = winner; }
}