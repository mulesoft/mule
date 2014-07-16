/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.transformers;

import org.mule.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.transport.email.GreenMailUtilities;

import java.io.StringReader;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EmailMessageToStringTestCase extends AbstractMuleTestCase
{
    private EmailMessageToString transformer;
    private static String TEXT = "text";
    private static String TO = "me@me.com";

    @Before
    public void createTransformer()
    {
        transformer = new EmailMessageToString();
    }

    @Test
    public void testSimpleStringMessage() throws MessagingException, TransformerException
    {
        Message msg = GreenMailUtilities.toMessage(TEXT, TO, null);
        assertEquals(TEXT, transformer.transform(msg));
    }

    @Test
    public void testSimpleNonTextMessage() throws MessagingException, TransformerException
    {
        MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
        message.setContent(new StringReader(TEXT), "application/octet-stream");
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(TO));
        assertEquals("", transformer.transform(message));
    }

    @Test
    public void testMultipartFirstPartTextMessage() throws MessagingException, TransformerException
    {
        MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
        MimeMultipart mimeMultipart = new MimeMultipart();
        MimeBodyPart bp1 = new MimeBodyPart();
        bp1.setContent(TEXT, "test/plain");
        mimeMultipart.addBodyPart(bp1);
        message.setContent(mimeMultipart);
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(TO));
        assertEquals(TEXT, transformer.transform(message));
    }

    @Test
    public void testMultipartMessage2() throws MessagingException, TransformerException
    {
        MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
        MimeMultipart mimeMultipart = new MimeMultipart();
        MimeBodyPart bp1 = new MimeBodyPart();
        bp1.setContent(new StringReader(TEXT), "binary");
        mimeMultipart.addBodyPart(bp1);
        message.setContent(mimeMultipart);
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(TO));
        assertEquals("", transformer.transform(message));
    }
}
