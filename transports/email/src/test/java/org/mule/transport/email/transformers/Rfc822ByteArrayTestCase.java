/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.transformers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.email.SmtpConnector;

import java.io.IOException;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.Test;

public class Rfc822ByteArrayTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "rfc822-byte-array-test.xml";
    }

    @Test
    public void testToByteArray() throws MessagingException, TransformerException
    {
        mimeMessageToByteArray(newMimeMessage());
    }

    @Test
    public void testToByteArrayAndBack() throws MessagingException, MuleException, IOException
    {
        MimeMessage first = newMimeMessage();
        byte[] bytes = mimeMessageToByteArray(first);
        MimeMessage second = byteArrayToMimeMessage(bytes);
        assertEquals(first.getSubject(), second.getSubject());
        assertEquals(first.getContent(), second.getContent());
        assertEquals(1, second.getFrom().length);
        assertEquals(first.getFrom().length, second.getFrom().length);
        assertEquals(first.getFrom()[0], second.getFrom()[0]);
    }

    protected MimeMessage byteArrayToMimeMessage(byte[] bytes) throws MuleException
    {
        Rfc822ByteArraytoMimeMessage transformer = new Rfc822ByteArraytoMimeMessage();
        ImmutableEndpoint endpoint =
            muleContext.getEndpointFactory().getOutboundEndpoint(SmtpConnector.SMTP);
        transformer.setEndpoint(endpoint);
        Object result = transformer.transform(bytes);
        assertTrue(result instanceof MimeMessage);
        return (MimeMessage) result;
    }

    protected byte[] mimeMessageToByteArray(MimeMessage mimeMessage) throws TransformerException
    {
        Object result = new MimeMessageToRfc822ByteArray().transform(mimeMessage);
        assertTrue(result instanceof byte[]);
        return (byte[]) result;
    }

    protected MimeMessage newMimeMessage() throws MessagingException
    {
        MimeMessage message = new MimeMessage(newSession());
        message.setText("text");
        message.setSubject("text");
        message.setFrom(new InternetAddress("bob@example.com"));
        return message;
    }

    protected Session newSession()
    {
        return Session.getDefaultInstance(new Properties(), null);
    }
}
