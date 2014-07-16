/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.transformers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.AbstractTransformerTestCase;
import org.mule.transformer.types.DataTypeFactory;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.junit.Test;

public class MailMessageTransformersTestCase extends AbstractTransformerTestCase
{
    private Message message;

    @Override
    public Transformer getTransformer() throws Exception
    {
        return createObject(EmailMessageToString.class);
    }

    @Override
    public Transformer getRoundTripTransformer() throws Exception
    {
        StringToEmailMessage trans = createObject(StringToEmailMessage.class);
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getOutboundEndpoint(
            "smtp://a:a@a.com");

        // We need to init the connector without actually connecting for this test
        // case
        try
        {
            endpoint.getConnector().initialise();
        }
        catch (IllegalStateException e)
        {
            assertNotNull(e);
        }

        trans.setEndpoint(endpoint);

        return trans;
    }

    @Override
    public Object getTestData()
    {
        if (message == null)
        {
            message = new MimeMessage(Session.getDefaultInstance(new Properties()));
            try
            {
                message.setContent(getResultData(), getContentType());
            }
            catch (MessagingException e)
            {
                throw new RuntimeException("Failed to create Mime Message: " + e.getMessage(), e);
            }
        }
        return message;
    }

    
    protected String getContentType()
    {
        return "text/plain";
    }
    
    @Override
    public Object getResultData()
    {
        return "Test Email Message";
    }

    @Override
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

    @Test
    public void testStringSourceType() throws Exception
    {
        try
        {
            assertFalse(getTransformer().isSourceDataTypeSupported(DataTypeFactory.STRING));
            getTransformer().transform(getResultData());
            fail("Should throw exception for string source type");
        }
        catch (TransformerException e)
        {
            // expected
            assertTrue(true);
        }
    }

    @Test
    public void testStringSourceTypeWithIgnoreBadInput() throws Exception
    {
        AbstractTransformer trans = (AbstractTransformer) getTransformer();
        trans.setIgnoreBadInput(true);
        ((DefaultMuleConfiguration) muleContext.getConfiguration()).setUseExtendedTransformations(false);
        Object result = trans.transform(getResultData());
        trans.setIgnoreBadInput(false);
        assertEquals(result, getResultData());
    }
}
