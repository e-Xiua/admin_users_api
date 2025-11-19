package com.iwellness.admin_users_api.Servicios;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas de EmailService")
public class EmailServiceTest {

    @InjectMocks
    private EmailService emailService;

    @Mock
    private JavaMailSender mailSender;

    private String destinatario;
    private String asunto;
    private String cuerpo;

    @BeforeEach
    void setUp() {
        destinatario = "test@example.com";
        asunto = "Test Subject";
        cuerpo = "Test Body Content";
    }

    @Test
    @DisplayName("enviarCorreo - Debería enviar correo con todos los campos correctos")
    void testEnviarCorreo_Success() {
        // Arrange
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // Act
        emailService.enviarCorreo(destinatario, asunto, cuerpo);

        // Assert
        verify(mailSender, times(1)).send(messageCaptor.capture());
        SimpleMailMessage capturedMessage = messageCaptor.getValue();
        
        assertNotNull(capturedMessage);
        assertEquals(destinatario, capturedMessage.getTo()[0]);
        assertEquals(asunto, capturedMessage.getSubject());
        assertEquals(cuerpo, capturedMessage.getText());
        assertEquals("juandavidh76@gmail.com", capturedMessage.getFrom());
    }

    @Test
    @DisplayName("enviarCorreo - Debería configurar el remitente correctamente")
    void testEnviarCorreo_VerifyFromAddress() {
        // Arrange
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        // Act
        emailService.enviarCorreo(destinatario, asunto, cuerpo);

        // Assert
        verify(mailSender).send(messageCaptor.capture());
        assertEquals("juandavidh76@gmail.com", messageCaptor.getValue().getFrom());
    }

    @Test
    @DisplayName("enviarCorreo - Debería manejar múltiples envíos")
    void testEnviarCorreo_MultipleEmails() {
        // Arrange
        String destinatario1 = "user1@example.com";
        String destinatario2 = "user2@example.com";
        String asunto1 = "Subject 1";
        String asunto2 = "Subject 2";

        // Act
        emailService.enviarCorreo(destinatario1, asunto1, "Body 1");
        emailService.enviarCorreo(destinatario2, asunto2, "Body 2");

        // Assert
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("enviarCorreo - Debería enviar correo de recuperación de contraseña")
    void testEnviarCorreo_PasswordRecovery() {
        // Arrange
        String usuario = "Juan Pérez";
        String token = "abc123xyz";
        String enlaceRecuperacion = "http://localhost:4200/restablecer?token=" + token;
        String cuerpoRecuperacion = "Hola " + usuario + ",\n\n"
            + "Haz clic en el siguiente enlace para restablecer tu contraseña:\n"
            + enlaceRecuperacion + "\n\n"
            + "Este enlace expirará en 15 minutos.\n\n"
            + "Saludos,\nEquipo I-Wellness";

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        // Act
        emailService.enviarCorreo(destinatario, "Recuperación de contraseña", cuerpoRecuperacion);

        // Assert
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage capturedMessage = messageCaptor.getValue();
        
        assertTrue(capturedMessage.getText().contains("Hola " + usuario));
        assertTrue(capturedMessage.getText().contains(enlaceRecuperacion));
        assertTrue(capturedMessage.getText().contains("15 minutos"));
        assertEquals("Recuperación de contraseña", capturedMessage.getSubject());
    }

    @Test
    @DisplayName("enviarCorreo - Debería propagar excepción del mailSender")
    void testEnviarCorreo_ThrowsException() {
        // Arrange
        doThrow(new RuntimeException("Mail server error")).when(mailSender).send(any(SimpleMailMessage.class));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            emailService.enviarCorreo(destinatario, asunto, cuerpo);
        });
    }

    @Test
    @DisplayName("enviarCorreo - Debería manejar asunto vacío")
    void testEnviarCorreo_EmptySubject() {
        // Arrange
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        // Act
        emailService.enviarCorreo(destinatario, "", cuerpo);

        // Assert
        verify(mailSender).send(messageCaptor.capture());
        assertEquals("", messageCaptor.getValue().getSubject());
    }

    @Test
    @DisplayName("enviarCorreo - Debería manejar cuerpo vacío")
    void testEnviarCorreo_EmptyBody() {
        // Arrange
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        // Act
        emailService.enviarCorreo(destinatario, asunto, "");

        // Assert
        verify(mailSender).send(messageCaptor.capture());
        assertEquals("", messageCaptor.getValue().getText());
    }

    @Test
    @DisplayName("enviarCorreo - Debería manejar contenido HTML como texto")
    void testEnviarCorreo_HTMLContent() {
        // Arrange
        String htmlContent = "<h1>Bienvenido</h1><p>Este es un correo de prueba</p>";
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        // Act
        emailService.enviarCorreo(destinatario, asunto, htmlContent);

        // Assert
        verify(mailSender).send(messageCaptor.capture());
        assertEquals(htmlContent, messageCaptor.getValue().getText());
    }

    @Test
    @DisplayName("enviarCorreo - Debería manejar caracteres especiales en el asunto")
    void testEnviarCorreo_SpecialCharactersInSubject() {
        // Arrange
        String asuntoEspecial = "¡Bienvenido! - Recuperación 100% exitosa";
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        // Act
        emailService.enviarCorreo(destinatario, asuntoEspecial, cuerpo);

        // Assert
        verify(mailSender).send(messageCaptor.capture());
        assertEquals(asuntoEspecial, messageCaptor.getValue().getSubject());
    }

    @Test
    @DisplayName("enviarCorreo - Debería manejar líneas múltiples en el cuerpo")
    void testEnviarCorreo_MultilineBody() {
        // Arrange
        String cuerpoMultilinea = "Primera línea\nSegunda línea\nTercera línea\n\nCuarta línea";
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        // Act
        emailService.enviarCorreo(destinatario, asunto, cuerpoMultilinea);

        // Assert
        verify(mailSender).send(messageCaptor.capture());
        assertEquals(cuerpoMultilinea, messageCaptor.getValue().getText());
        assertTrue(messageCaptor.getValue().getText().contains("\n"));
    }
}
