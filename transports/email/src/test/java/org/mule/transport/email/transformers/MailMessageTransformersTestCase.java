/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.email.transformers;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transformer.Transformer;
import org.mule.lifecycle.AlreadyInitialisedException;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.AbstractTransformerTestCase;
import org.mule.transport.email.transformers.EmailMessageToString;
import org.mule.transport.email.transformers.StringToEmailMessage;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

public class MailMessageTransformersTestCase extends AbstractTransformerTestCase
{
    private Message message;

    public Transformer getTransformer() throws Exception
    {
        return new EmailMessageToString();
    }

    public Transformer getRoundTripTransformer() throws Exception
    {
        StringToEmailMessage trans = new StringToEmailMessage();
        ImmutableEndpoint endpoint = muleContext.getRegistry().lookupEndpointFactory().getOutboundEndpoint(
            "smtp://a:a@a.com");

        // We need to init the connector without actually connecting for this test
        // case
        try
        {
            endpoint.getConnector().initialise();
        }
        catch (AlreadyInitialisedException e)
        {
            assertNotNull(e);
        }

        trans.setEndpoint(endpoint);

        return trans;
    }

    public Object getTestData()
    {
        if (message == null)
        {
            message = new MimeMessage(Session.getDefaultInstance(new Properties()));
            try
            {
                message.setContent(getResultData(), "text/plain");
            }
            catch (MessagingException e)
            {
                throw new RuntimeException("Failed to create Mime Message: " + e.getMessage(), e);
            }
        }
        return message;
    }

    public Object getResultData()
    {
        return "Test Email Message";
    }

    public boolean compareResults(Object src, Object result)
    {
        if (src instanceof Message)
        {
            Object objSrc;
            Object objRes;
            try
            {
                objSrc = ((Message) src).getContent();
                objRes = ((Message) result).getContent();
            }
            catch (Exception e)
            {
                throw new RuntimeException(e.getMessage(), e);
            }
            if (objSrc == null || objRes == null)
            {
                return false;
            }
            return objRes.equals(objSrc);
        }
        return super.compareResults(src, result);
    }

    public void testStringSourceType() throws Exception
    {
        try
        {
            assertFalse(getTransformer().isSourceTypeSupported(String.class));
            getTransformer().transform(getResultData());
            fail("Should throw exception for string source type");
        }
        catch (TransformerException e)
        {
            // expected
            assertTrue(true);
        }
    }

    public void testStringSourceTypeWithIgnoreBadInput() throws Exception
    {
        AbstractTransformer trans = (AbstractTransformer) getTransformer();
        trans.setIgnoreBadInput(true);
        Object result = trans.transform(getResultData());
        trans.setIgnoreBadInput(false);
        assertSame(result, getResultData());
    }
}
