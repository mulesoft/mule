/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport;

import org.mule.api.MuleMessage;
import org.mule.api.transport.MessageTypeNotSupportedException;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

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
        
        MuleMessage message = factory.create(null, encoding);
        assertNotNull(message);
        assertEquals(NullPayload.getInstance(), message.getPayload());
    }

    @Test
    public void testValidPayload() throws Exception
    {
        MuleMessageFactory factory = createMuleMessageFactory();
    
        Object payload = getValidTransportMessage();
        MuleMessage message = factory.create(payload, encoding);
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
            factory.create(invalidPayload, encoding);
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
