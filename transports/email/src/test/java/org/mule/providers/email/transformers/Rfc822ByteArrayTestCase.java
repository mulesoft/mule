/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.email.transformers;

import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.transformer.TransformerException;

import java.io.IOException;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class Rfc822ByteArrayTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "rfc822-byte-array-test.xml";
    }

    public void testToByteArray() throws MessagingException, TransformerException
    {
        mimeMessageToByteArray(newMimeMessage());
    }

    public void testToByteArrayAndBack() throws MessagingException, UMOException, IOException
    {
        MimeMessage first = newMimeMessage();
        byte[] bytes = mimeMessageToByteArray(first);
        MimeMessage second = byteArrayToMimeMessage(bytes);
        assertEquals(first.getContent(), second.getContent());
        assertEquals(1, second.getFrom().length);
        assertEquals(first.getFrom().length, second.getFrom().length);
        assertEquals(first.getFrom()[0], second.getFrom()[0]);
    }

    protected MimeMessage byteArrayToMimeMessage(byte[] bytes) throws UMOException
    {
        Rfc822ByteArraytoMimeMessage transformer = new Rfc822ByteArraytoMimeMessage();
        UMOImmutableEndpoint endpoint = managementContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            "smtp");
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
        message.setFrom(new InternetAddress("bob@example.com"));
        return message;
    }

    protected Session newSession()
    {
        return Session.getDefaultInstance(new Properties(), null);
    }

}
