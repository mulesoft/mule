/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.transport.MessageTypeNotSupportedException;
import org.mule.runtime.core.api.transport.MuleMessageFactory;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public abstract class AbstractMuleMessageFactoryTestCase extends AbstractMuleContextTestCase
{
    protected String encoding;
    
    /**
     * Subclasses can set this flag to false, disabling the test for unsupported transport
     * message types.
     */
    protected boolean runUnsuppoprtedTransportMessageTest = true;

    public AbstractMuleMessageFactoryTestCase()
    {
        super();
        setStartContext(false);
    }
    
    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        encoding = muleContext.getConfiguration().getDefaultEncoding();
    }
    
    @Test
    public void testNullPayload() throws Exception
    {
        MuleMessageFactory factory = createMuleMessageFactory();
        
        MuleMessage message = factory.create(null, encoding, muleContext);
        assertNotNull(message);
        assertEquals(NullPayload.getInstance(), message.getPayload());
    }

    @Test
    public void testValidPayload() throws Exception
    {
        MuleMessageFactory factory = createMuleMessageFactory();
    
        Object payload = getValidTransportMessage();
        MuleMessage message = factory.create(payload, encoding, muleContext);
        assertNotNull(message);
        assertEquals(payload, message.getPayload());
    }
    
    @Test // this test cannot use expected=MessageTypeNotSupportedException as it is not always exectued
    public void testUnsupportedPayloadType() throws Exception
    {
        if (runUnsuppoprtedTransportMessageTest == false)
        {
            return;
        }
        
        MuleMessageFactory factory = createMuleMessageFactory();
        
        Object invalidPayload = getUnsupportedTransportMessage();
        try
        {
            factory.create(invalidPayload, encoding, muleContext);
            fail("Creating a MuleMessageFactory from an invalid transport message must fail"); 
        }
        catch (MessageTypeNotSupportedException mtnse)
        {
            // this one was expected
        }
    }

    protected MuleMessageFactory createMuleMessageFactory()
    {
        MuleMessageFactory factory = doCreateMuleMessageFactory();
        assertNotNull(factory);
        return factory;
    }

    protected Object getUnsupportedTransportMessage()
    {
        throw new AssertionError("Subclasses must properly implement this method");
    }

    protected abstract MuleMessageFactory doCreateMuleMessageFactory();
    
    protected abstract Object getValidTransportMessage() throws Exception;
}
