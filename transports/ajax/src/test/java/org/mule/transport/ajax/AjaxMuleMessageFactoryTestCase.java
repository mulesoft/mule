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

import org.mule.api.MuleMessage;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.transport.AbstractMuleMessageFactoryTestCase;
import org.mule.transport.ajax.embedded.AjaxConnector;

import java.util.HashMap;
import java.util.Map;

import org.cometd.Bayeux;

public class AjaxMuleMessageFactoryTestCase extends AbstractMuleMessageFactoryTestCase
{
    private static final String JSON_STRING = "{\"value1\":\"foo\",\"value2\":\"bar\"}";

    public AjaxMuleMessageFactoryTestCase()
    {
        super();
        runUnsuppoprtedTransportMessageTest = false;
    }
    
    @Override
    protected MuleMessageFactory doCreateMuleMessageFactory()
    {
        return new AjaxMuleMessageFactory(muleContext);
    }

    @Override
    protected Object getValidTransportMessage() throws Exception
    {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Bayeux.DATA_FIELD, JSON_STRING);
        map.put(AjaxConnector.REPLYTO_PARAM, "/reply");
        map.put("message-property", "mp-value");
        
        return map;
    }

    @Override
    public void testValidPayload() throws Exception
    {
        MuleMessageFactory factory = createMuleMessageFactory();
        
        Object payload = getValidTransportMessage();
        MuleMessage message = factory.create(payload, encoding);
        assertNotNull(message);
        assertEquals(JSON_STRING, message.getPayload());
        assertEquals("/reply", message.getReplyTo());
        assertEquals("mp-value", message.getProperty("message-property", PropertyScope.INVOCATION));
    }
    
    public void testMapPayloadWithoutData() throws Exception
    {
        Map<?, ?> payload = (Map<?, ?>) getValidTransportMessage();
        payload.remove(Bayeux.DATA_FIELD);
        
        MuleMessageFactory factory = createMuleMessageFactory();
        try
        {
            factory.create(payload, encoding);
            fail("Creating a MuleMessage from a map without " + Bayeux.DATA_FIELD + " key must fail");
        }
        catch (IllegalArgumentException iae)
        {
            // this one was expected
        }
    }
    
    public void testJsonStringPayloadWithoutData() throws Exception
    {
        String payload = "{\"value1\" : \"foo\", \"value2\" : \"bar\"}";
        MuleMessageFactory factory = createMuleMessageFactory();
        MuleMessage message = factory.create(payload, encoding);
        assertNotNull(message);
        assertEquals(payload, message.getPayload());
        assertTrue(message.getPayload() instanceof String);
    }
    
    public void testJsonStringWithData() throws Exception
    {
        String data = JSON_STRING;
        String payload = String.format("{ \"data\" : %1s, \"%2s\" : \"/replyEndpoint\"}",
            data, AjaxConnector.REPLYTO_PARAM);
        
        MuleMessageFactory factory = createMuleMessageFactory();
        MuleMessage message = factory.create(payload, encoding);
        assertNotNull(message);
        assertEquals(data, message.getPayload());
        assertEquals("/replyEndpoint", message.getReplyTo());
    }
    
    public void testNonMapNonJsonPayload() throws Exception
    {
        FruitBowl payload = new FruitBowl(new Apple(), new Banana());
        MuleMessageFactory factory = createMuleMessageFactory();
        MuleMessage message = factory.create(payload, encoding);
        assertNotNull(message);
        assertEquals(payload, message.getPayload());
    }
}
