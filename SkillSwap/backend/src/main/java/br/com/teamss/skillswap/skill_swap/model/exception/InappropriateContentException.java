package br.com.teamss.skillswap.skill_swap.model.exception;

public class InappropriateContentException extends RuntimeException {
    public InappropriateContentException(String message) {
        super(message);
    }
}