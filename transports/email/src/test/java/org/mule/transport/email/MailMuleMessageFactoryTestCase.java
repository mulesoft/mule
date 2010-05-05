/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.email;

import org.mule.api.transport.MuleMessageFactory;
import org.mule.transport.AbstractMuleMessageFactoryTestCase;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;

public class MailMuleMessageFactoryTestCase extends AbstractMuleMessageFactoryTestCase
{
    @Override
    protected MuleMessageFactory doCreateMuleMessageFactory()
    {
        return new MailMuleMessageFactory(muleContext);
    }

    @Override
    protected Object getValidTransportMessage() throws Exception
    {
        MimeMessage message = new MimeMessage((Session) null);
        message.setContent(TEST_MESSAGE, "text/plain; charset=ISO-8859-1");
        return message;
    }

    @Override
    protected Object getUnsupportedTransportMessage()
    {
        return "this is not a valid transport message for MailMuleMessageFactory";
    }
    
    public void _testAttachments()
    {
        // TODO MessageAdapterRemoval: implement me
    }
}


