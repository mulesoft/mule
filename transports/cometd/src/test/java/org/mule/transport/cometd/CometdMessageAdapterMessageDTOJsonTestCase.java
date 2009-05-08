/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.cometd;

import org.mule.transport.AbstractMessageAdapterTestCase;
import org.mule.api.transport.MessageAdapter;
import org.mule.api.MessagingException;
import org.mule.message.DefaultMuleMessageDTO;


import java.util.Map;
import java.util.HashMap;

public class CometdMessageAdapterMessageDTOJsonTestCase extends AbstractMessageAdapterTestCase
{
    public Object getValidMessage() throws Exception
    {
        return "{'payload' : {'value1' : 'foo', 'value2' : 'bar'}, 'replyTo' : '/services/response'}";
    }

    public MessageAdapter createAdapter(Object payload) throws MessagingException
    {
        return new CometdMessageAdapter(payload);
    }

    protected void doTestMessageEqualsPayload(Object message, Object payload) throws Exception
    {
        assertTrue(payload instanceof Map);

        assertEquals("foo", ((Map)payload).get("value1"));
        assertEquals("bar", ((Map)payload).get("value2"));
    }
}