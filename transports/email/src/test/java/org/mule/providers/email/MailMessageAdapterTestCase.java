/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.email;

import org.mule.tck.providers.AbstractMessageAdapterTestCase;
import org.mule.umo.MessagingException;
import org.mule.umo.provider.UMOMessageAdapter;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

public class MailMessageAdapterTestCase extends AbstractMessageAdapterTestCase
{
    private Message message;

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
