/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.util;

import static java.util.Collections.singletonList;
import static javax.mail.Message.RecipientType.TO;
import static javax.mail.Part.ATTACHMENT;
import static javax.mail.Part.INLINE;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.transformer.types.MimeTypes.TEXT;
import org.mule.extension.email.internal.builder.MessageBuilder;
import org.mule.runtime.core.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class EmailTestUtils
{
    public static final String EMAIL_SUBJECT = "Email Subject";
    public static final String EMAIL_CONTENT = "Email Content";
    public static final String EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT = "This is the email text attachment";
    public static final String EMAIL_TEXT_PLAIN_ATTACHMENT_NAME = "text-attachment";
    public static final String EMAIL_JSON_ATTACHMENT_CONTENT =  "{\"key\": \"value\"}";
    public static final String EMAIL_JSON_ATTACHMENT_NAME =  "attachment.json";

    public static final String PABLON_EMAIL = "pablo.musumeci@mulesoft.com";
    public static final String ESTEBAN_EMAIL = "esteban.wasinger@mulesoft.com";
    public static final String JUANI_EMAIL = "juan.desimoni@mulesoft.com";
    public static final String ALE_EMAIL = "ale.g.marra@mulesoft.com";
    public static final String MG_EMAIL = "mariano.gonzalez@mulesoft.com";
    public static final String[] EMAILS = {JUANI_EMAIL, ESTEBAN_EMAIL, PABLON_EMAIL, ALE_EMAIL};

    public static final Session testSession = Session.getDefaultInstance(new Properties());


    public static MimeMessage buildMultipartMessage() throws Exception
    {
        Multipart multipart = new MimeMultipart();
        MimeBodyPart bodyPart = new MimeBodyPart();
        bodyPart.setContent(EMAIL_CONTENT, TEXT);
        bodyPart.setDisposition(INLINE);
        multipart.addBodyPart(bodyPart);

        URL resource = Thread.currentThread().getContextClassLoader().getResource(EMAIL_JSON_ATTACHMENT_NAME);
        assertThat(resource, is(not(nullValue())));
        bodyPart = new MimeBodyPart();
        bodyPart.setDisposition(ATTACHMENT);
        bodyPart.setFileName(EMAIL_JSON_ATTACHMENT_NAME);
        bodyPart.setDataHandler(new DataHandler(resource));
        multipart.addBodyPart(bodyPart);

        bodyPart = new MimeBodyPart();
        bodyPart.setDisposition(ATTACHMENT);
        bodyPart.setFileName(EMAIL_TEXT_PLAIN_ATTACHMENT_NAME);
        bodyPart.setContent(EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT, TEXT);
        multipart.addBodyPart(bodyPart);

        MimeMessage message = new MimeMessage(testSession);
        message.setContent(multipart);
        message.setSubject(EMAIL_SUBJECT);
        message.setRecipient(TO, new InternetAddress(ESTEBAN_EMAIL));

        return message;
    }

    public static Message buildSinglePartMessage() throws IOException, MessagingException
    {
        Message message = new MimeMessage(testSession);
        message.setDataHandler(new DataHandler(EMAIL_CONTENT, TEXT));
        message.setSubject(EMAIL_SUBJECT);
        message.setRecipient(TO, new InternetAddress(ESTEBAN_EMAIL));

        return message;
    }

    public static MessageBuilder createDefaultMimeMessageBuilder(String to) throws MessagingException
    {
        return MessageBuilder.newMessage(testSession)
                .to(singletonList(to))
                .cc(singletonList(ALE_EMAIL))
                .withContent(EMAIL_CONTENT)
                .withSubject(EMAIL_SUBJECT);
    }

    public static void assertAttachmentContent(Map<String, DataHandler> attachments, String attachmentKey, Object expectedResult) throws IOException
    {
        DataHandler attachment = attachments.get(attachmentKey);
        String attachmentAsString = IOUtils.toString((InputStream) attachment.getContent());
        assertThat(attachmentAsString, is(expectedResult));
    }
}
