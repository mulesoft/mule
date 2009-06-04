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

import org.mule.api.transport.MessageAdapter;
import org.mule.transport.AbstractMessageAdapterSerializationTestCase;

import org.jivesoftware.smack.packet.Message;

public class XmppMessageAdapterSerializationTestCase extends AbstractMessageAdapterSerializationTestCase
{

    @Override
    protected MessageAdapter createMessageAdapter() throws Exception
    {
        Message message = new Message();
        message.setBody(PAYLOAD);
        message.setProperty(STRING_PROPERTY_KEY, STRING_PROPERTY_VALUE);
        return new XmppMessageAdapter(message);
    }
    
}


