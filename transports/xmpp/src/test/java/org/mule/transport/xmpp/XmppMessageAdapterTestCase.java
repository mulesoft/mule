/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.xmpp;

import org.mule.api.MessagingException;
import org.mule.api.transport.MessageAdapter;
import org.mule.transport.AbstractMessageAdapterTestCase;

import org.jivesoftware.smack.packet.Message;

public class XmppMessageAdapterTestCase extends AbstractMessageAdapterTestCase
{

    @Override
    public MessageAdapter createAdapter(Object payload) throws MessagingException
    {
        return new XmppMessageAdapter(payload);
    }

    @Override
    public Object getValidMessage() throws Exception
    {
        Message msg = new Message();
        msg.setBody(TEST_MESSAGE);
        return msg;
    }

}


