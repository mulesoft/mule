/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.util;

import static java.lang.Thread.currentThread;
import static javax.mail.Message.RecipientType.TO;
import static javax.mail.Part.ATTACHMENT;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.metadata.MediaType.TEXT;

import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.core.message.DefaultMultiPartPayload;
import org.mule.runtime.core.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
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

import com.icegreen.greenmail.util.ServerSetup;

public class EmailTestUtils {

  public static final String EMAIL_SUBJECT = "Email Subject";
  public static final String EMAIL_CONTENT = "Email Content";
  public static final String EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT = "This is the email text attachment";
  public static final String EMAIL_TEXT_PLAIN_ATTACHMENT_NAME = "text-attachment";
  public static final String EMAIL_JSON_ATTACHMENT_CONTENT = "{\"key\": \"value\"}";
  public static final String EMAIL_JSON_ATTACHMENT_NAME = "attachment.json";

  public static final String PABLON_EMAIL = "pablo.musumeci@mulesoft.com";
  public static final String ESTEBAN_EMAIL = "esteban.wasinger@mulesoft.com";
  public static final String JUANI_EMAIL = "juan.desimoni@mulesoft.com";
  public static final String ALE_EMAIL = "ale.g.marra@mulesoft.com";
  public static final String MG_EMAIL = "mariano.gonzalez@mulesoft.com";
  public static final String[] EMAILS = {JUANI_EMAIL, ESTEBAN_EMAIL, PABLON_EMAIL, ALE_EMAIL};

  public static final long SERVER_STARTUP_TIMEOUT = 5000;

  public static final Session testSession = Session.getDefaultInstance(new Properties());


  public static MimeMessage getMultipartTestMessage() throws Exception {
    MimeBodyPart body = new MimeBodyPart();
    body.setContent(EMAIL_CONTENT, TEXT.toString());

    MimeBodyPart textAttachment = new MimeBodyPart();
    textAttachment.setDisposition(ATTACHMENT);
    textAttachment.setFileName(EMAIL_TEXT_PLAIN_ATTACHMENT_NAME);
    textAttachment.setContent(EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT, TEXT.toString());

    MimeBodyPart jsonAttachment = new MimeBodyPart();
    URL resource = currentThread().getContextClassLoader().getResource(EMAIL_JSON_ATTACHMENT_NAME);
    jsonAttachment.setFileName(EMAIL_JSON_ATTACHMENT_NAME);
    jsonAttachment.setDataHandler(new DataHandler(resource));

    Multipart multipart = new MimeMultipart();
    multipart.addBodyPart(body);
    multipart.addBodyPart(textAttachment);
    multipart.addBodyPart(jsonAttachment);

    MimeMessage message = new MimeMessage(testSession);
    message.setContent(multipart);
    message.setSubject(EMAIL_SUBJECT);
    message.setRecipient(TO, new InternetAddress(ESTEBAN_EMAIL));
    return message;
  }

  public static Message getSinglePartTestMessage() throws IOException, MessagingException {
    Message message = new MimeMessage(testSession);
    message.setText(EMAIL_CONTENT);
    message.setSubject(EMAIL_SUBJECT);
    message.setRecipient(TO, new InternetAddress(ESTEBAN_EMAIL));
    return message;
  }

  public static void assertAttachmentContent(List<MuleMessage> attachments, String attachmentKey, Object expectedResult)
      throws IOException {
    final MultiPartPayload multiPartPayload = new DefaultMultiPartPayload(attachments);

    MuleMessage attachment = multiPartPayload.getPart(attachmentKey);
    String attachmentAsString = IOUtils.toString((InputStream) attachment.getPayload());
    assertThat(attachmentAsString, is(expectedResult));
  }

  public static ServerSetup setUpServer(int port, String protocol) {
    ServerSetup serverSetup = new ServerSetup(port, null, protocol);
    serverSetup.setServerStartupTimeout(SERVER_STARTUP_TIMEOUT);
    return serverSetup;
  }
}
