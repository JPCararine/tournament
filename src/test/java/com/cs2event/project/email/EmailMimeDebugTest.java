package com.cs2event.project.email;

import jakarta.activation.DataHandler;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

class EmailMimeDebugTest {

    private static final String PNG_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";

    @Test
    void compararAbordagens() throws Exception {
        System.out.println(">>> HEADLESS=" + java.awt.GraphicsEnvironment.isHeadless());
        byte[] bytes = Base64.getDecoder().decode(PNG_BASE64);

        System.out.println(">>> [A] addInline com ByteArrayDataSource(image/png):");
        System.out.println(">>>     CONTEUDO_PRESENTE=" + buildWithAddInline(bytes));

        System.out.println(">>> [B] MimeBodyPart manual octet-stream + header image/png:");
        System.out.println(">>>     CONTEUDO_PRESENTE=" + buildWithOctetStream(bytes));
    }

    private boolean buildWithAddInline(byte[] bytes) throws Exception {
        MimeMessage message = mensagemBase();
        MimeMessageHelper helper = new MimeMessageHelper(
                message, MimeMessageHelper.MULTIPART_MODE_RELATED, StandardCharsets.UTF_8.name());
        prepara(helper);
        ByteArrayDataSource ds = new ByteArrayDataSource(bytes, "image/png");
        helper.addInline("pixQrCode", ds);
        return contemPng(message);
    }

    private boolean buildWithOctetStream(byte[] bytes) throws Exception {
        MimeMessage message = mensagemBase();
        MimeMessageHelper helper = new MimeMessageHelper(
                message, MimeMessageHelper.MULTIPART_MODE_RELATED, StandardCharsets.UTF_8.name());
        prepara(helper);
        MimeBodyPart imagePart = new MimeBodyPart();
        imagePart.setDataHandler(new DataHandler(new ByteArrayDataSource(bytes, "application/octet-stream")));
        imagePart.setContentID("<pixQrCode>");
        imagePart.setDisposition(MimeBodyPart.INLINE);
        imagePart.setHeader("Content-Type", "image/png; name=pix-qr-code.png");
        helper.getRootMimeMultipart().addBodyPart(imagePart);
        return contemPng(message);
    }

    private void prepara(MimeMessageHelper helper) throws Exception {
        helper.setFrom("a@b.com");
        helper.setTo("c@d.com");
        helper.setSubject("debug");
        helper.setText("plain", "<img src=\"cid:pixQrCode\">");
    }

    private MimeMessage mensagemBase() {
        return new JavaMailSenderImpl().createMimeMessage();
    }

    private boolean contemPng(MimeMessage message) throws Exception {
        message.saveChanges();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        message.writeTo(out);
        return out.toString(StandardCharsets.UTF_8).contains("iVBOR");
    }
}
