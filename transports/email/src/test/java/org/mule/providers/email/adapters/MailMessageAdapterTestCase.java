/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.email.adapters;

import org.mule.providers.email.MailMessageAdapter;
import org.mule.tck.providers.AbstractMessageAdapterTestCase;
import org.mule.umo.MessagingException;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.registry.ServiceDescriptorFactory;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

public class MailMessageAdapterTestCase extends AbstractMessageAdapterTestCase
{
    private Message message;

    protected void doSetUp() throws Exception
    {
        //wee need to load the transport descriptor in order to tes tthe message Adapter
        managementContext.getRegistry().lookupServiceDescriptor(ServiceDescriptorFactory.PROVIDER_SERVICE_TYPE, "pop3", null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractMessageAdapterTestCase#createAdapter()
     */
    public UMOMessageAdapter createAdapter(Object payload) throws MessagingException
    {
        return new MailMessageAdapter(payload);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractMessageAdapterTestCase#getValidMessage()
     */
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
