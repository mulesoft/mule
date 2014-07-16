/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ajax;

import org.mule.api.MuleMessage;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.transport.AbstractMuleMessageFactoryTestCase;
import org.mule.transport.ajax.embedded.AjaxConnector;

import java.util.HashMap;
import java.util.Map;

import org.cometd.Bayeux;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
        return new AjaxMuleMessageFactory();
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
        MuleMessage message = factory.create(payload, encoding, muleContext);
        assertNotNull(message);
        assertEquals(JSON_STRING, message.getPayload());
        assertEquals("/reply", message.getReplyTo());
        assertEquals("mp-value", message.getInvocationProperty("message-property"));
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testMapPayloadWithoutData() throws Exception
    {
        Map<?, ?> payload = (Map<?, ?>) getValidTransportMessage();
        payload.remove(Bayeux.DATA_FIELD);
        
        MuleMessageFactory factory = createMuleMessageFactory();
        factory.create(payload, encoding, muleContext);
    }
    
    @Test
    public void testJsonStringPayloadWithoutData() throws Exception
    {
        String payload = "{\"value1\" : \"foo\", \"value2\" : \"bar\"}";
        MuleMessageFactory factory = createMuleMessageFactory();
        MuleMessage message = factory.create(payload, encoding, muleContext);
        assertNotNull(message);
        assertEquals(payload, message.getPayload());
        assertTrue(message.getPayload() instanceof String);
    }
    
    @Test
    public void testJsonStringWithData() throws Exception
    {
        String data = JSON_STRING;
        String payload = String.format("{ \"data\" : %1s, \"%2s\" : \"/replyEndpoint\"}",
            data, AjaxConnector.REPLYTO_PARAM);
        
        MuleMessageFactory factory = createMuleMessageFactory();
        MuleMessage message = factory.create(payload, encoding, muleContext);
        assertNotNull(message);
        assertEquals(data, message.getPayload());
        assertEquals("/replyEndpoint", message.getReplyTo());
    }
    
    @Test
    public void testNonMapNonJsonPayload() throws Exception
    {
        FruitBowl payload = new FruitBowl(new Apple(), new Banana());
        MuleMessageFactory factory = createMuleMessageFactory();
        MuleMessage message = factory.create(payload, encoding, muleContext);
        assertNotNull(message);
        assertEquals(payload, message.getPayload());
    }
}
