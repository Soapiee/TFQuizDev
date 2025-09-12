package me.soapiee.common.instance.logic;

public class Question {

    private final String question;
    private final String correctionMessage;

    public Question(String question, String correctionMessage) {
        this.question = question;
        this.correctionMessage = correctionMessage;
    }

    public String getQuestion() {
        return this.question;
    }

    public String getCorrectionMessage() {
        return this.correctionMessage;
    }
}
