/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.email.adapters;

import org.mule.api.MessagingException;
import org.mule.api.registry.ServiceDescriptorFactory;
import org.mule.api.transport.MessageAdapter;
import org.mule.transport.AbstractMessageAdapterTestCase;
import org.mule.transport.email.MailMessageAdapter;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

public class MailMessageAdapterTestCase extends AbstractMessageAdapterTestCase
{
    private Message message;

    protected void doSetUp() throws Exception
    {
        //wee need to load the transport descriptor in order to test the message Adapter
        muleContext.getRegistry().lookupServiceDescriptor(ServiceDescriptorFactory.TRANSPORT_SERVICE_TYPE, "pop3", null);
    }

    public MessageAdapter createAdapter(Object payload) throws MessagingException
    {
        return new MailMessageAdapter(payload);
    }

    public Object getValidMessage() throws Exception
    {
        if (message == null)
        {
            message = new MimeMessage(Session.getDefaultInstance(new Properties()));
            message.setContent("Test Email Message", "text/plain");
        }

        return message;
    }

}
