package br.dev.guisleri.mototrack.mail;

public interface EmailSender {

    void send(String to, String subject, String text);

}
