/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import org.mule.api.MuleMessage;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.transport.AbstractMuleMessageFactoryTestCase;

import java.util.Arrays;
import java.util.List;

import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.message.MessageImpl;

public class CxfMuleMessageFactoryTestCase extends AbstractMuleMessageFactoryTestCase
{
    @Override
    protected MuleMessageFactory doCreateMuleMessageFactory()
    {
        return new CxfMuleMessageFactory(muleContext);
    }

    @Override
    protected Object getValidTransportMessage() throws Exception
    {
        MessageImpl message = new MessageImpl();
        message.setContent(Object.class, TEST_MESSAGE);
        return message;
        
    }

    @Override
    protected Object getUnsupportedTransportMessage()
    {
        return "this is an invalid transport message for CxfMuleMessageFactory";
    }
    
    @Override
    public void testValidPayload() throws Exception
    {
        MuleMessageFactory factory = createMuleMessageFactory();
        Object payload = getValidTransportMessage();
        MuleMessage message = factory.create(payload, encoding);
        assertNotNull(message);
        assertEquals(TEST_MESSAGE, message.getPayload());
    }

    public void testListMessageContentWithSingleEntry() throws Exception
    {
        Message payload = createPayload(TEST_MESSAGE);
        MuleMessageFactory factory = createMuleMessageFactory();
        MuleMessage message = factory.create(payload, encoding);
        assertEquals(String.class, message.getPayload().getClass());
    }
    
    public void testListMessageContentWithMultipleEntries() throws Exception
    {        
        Message payload = createPayload(TEST_MESSAGE, TEST_MESSAGE);   
        MuleMessageFactory factory = createMuleMessageFactory();
        MuleMessage message = factory.create(payload, encoding);
        assertEquals(Object[].class, message.getPayload().getClass());
    }
    
    private Message createPayload(Object... contents)
    {
        List<Object> list = Arrays.asList(contents);
        MessageContentsList messageContentsList = new MessageContentsList(list);
        
        MessageImpl message = new MessageImpl();
        message.setContent(List.class, messageContentsList);
        return message;
    }
}
