/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.email.transformers;

import org.mule.impl.AlreadyInitialisedException;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.tck.AbstractTransformerTestCase;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOMessageDispatcher;
import org.mule.umo.transformer.UMOTransformer;

import com.mockobjects.dynamic.Mock;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

public class MailMessageTransformersTestCase extends AbstractTransformerTestCase
{
    private Message message;

    public UMOTransformer getTransformer() throws Exception
    {
        return new EmailMessageToString();
    }

    public UMOTransformer getRoundTripTransformer() throws Exception
    {
        StringToEmailMessage trans = new StringToEmailMessage();
        UMOEndpoint endpoint = new MuleEndpoint("smtp://a:a@a.com", false);

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
        final Mock mockDispatcher = new Mock(UMOMessageDispatcher.class);
        mockDispatcher.expectAndReturn("getDelegateSession", Session.getDefaultInstance(new Properties()));
        mockDispatcher.expectAndReturn("getDelegateSession", Session.getDefaultInstance(new Properties()));

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
            Object objSrc = null;
            Object objRes = null;
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
}
