/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ajax;

import org.mule.api.MuleException;
import org.mule.api.transport.MessageAdapter;
import org.mule.transport.AbstractMessageAdapterTestCase;

public class AjaxMessageAdapterMessageDTOJsonTestCase extends AbstractMessageAdapterTestCase
{
    public Object getValidMessage() throws Exception
    {
        return "{\"data\":{\"value1\":\"foo\",\"value2\":\"bar\"},\"replyTo\":\"/response\"}";
    }

    public MessageAdapter createAdapter(Object payload) throws MuleException
    {
        return new AjaxMessageAdapter(payload);
    }

    public void testCustomMessageProps() throws Exception
    {
        MessageAdapter adapter = createAdapter(getValidMessage());
        assertEquals("/response", adapter.getReplyTo());
    }

    @Override
    protected void doTestMessageEqualsPayload(Object payload1, Object payload2) throws Exception
    {
        //Mule will set the data field as the payload
        assertEquals("{\"value1\":\"foo\",\"value2\":\"bar\"}", payload2);
    }
}