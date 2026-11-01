package br.dev.guisleri.mototrack.mail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SmtpEmailSenderTest {

    @Mock
    private JavaMailSender mailSender;

    @Test
    void shouldSendAPlainTextMessageFromTheConfiguredAddress() {
        SmtpEmailSender sender = new SmtpEmailSender(mailSender, "MotoTrack <nao-responda@mototrack.local>");
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        sender.send("marcos@example.com", "Assunto", "Corpo com o código 123456");

        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertEquals("MotoTrack <nao-responda@mototrack.local>", message.getFrom());
        assertArrayEquals(new String[]{"marcos@example.com"}, message.getTo());
        assertEquals("Assunto", message.getSubject());
        assertEquals("Corpo com o código 123456", message.getText());
    }

    @Test
    void shouldNotFailTheCallerWhenTheMailServerIsDown() {
        SmtpEmailSender sender = new SmtpEmailSender(mailSender, "from@example.com");
        doThrow(new MailSendException("down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> sender.send("marcos@example.com", "Assunto", "Corpo"));
    }
}
