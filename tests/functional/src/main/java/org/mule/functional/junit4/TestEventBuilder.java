/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4;

import static org.mockito.Mockito.spy;
import static org.mule.MessageExchangePattern.ONE_WAY;
import static org.mule.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.PropertyScope.SESSION;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.connector.ReplyToHandler;
import org.mule.session.DefaultMuleSession;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.Transformer;
import org.mockito.Mockito;

/**
 * Provides a fluent API for building events for testing.
 */
public class TestEventBuilder
{

    private Object payload;

    private Map<String, Object> inboundProperties = new HashMap<>();
    private Map<String, Object> outboundProperties = new HashMap<>();
    private Map<String, Object> sessionProperties = new HashMap<>();

    private Map<String, Object> flowVariables = new HashMap<>();

    private MessageExchangePattern exchangePattern = REQUEST_RESPONSE;

    private boolean transacted = false;

    private ReplyToHandler replyToHandler;

    private Transformer spyTransformer = new Transformer()
    {

        @Override
        public Object transform(Object input)
        {
            return input;
        }
    };

    /**
     * Prepares the given data to be sent as the payload of the product.
     * 
     * @param payload the payload to use in the message
     * @return this {@link TestEventBuilder}
     */
    public TestEventBuilder withPayload(Object payload)
    {
        this.payload = payload;

        return this;
    }

    /**
     * Prepares a property with the given key and value to be sent as an inbound property of the product.
     * 
     * @param key the key of the inbound property to add
     * @param value the value of the inbound property to add
     * @return this {@link TestEventBuilder}
     */
    public TestEventBuilder withInboundProperty(String key, Object value)
    {
        inboundProperties.put(key, value);

        return this;
    }

    /**
     * Prepares the given properties map to be sent as inbound properties of the product.
     * 
     * @param properties the inbound properties to add
     * @return this {@link TestEventBuilder}
     */
    public TestEventBuilder withInboundProperties(Map<String, Object> properties)
    {
        inboundProperties.putAll(properties);

        return this;
    }

    /**
     * Prepares a property with the given key and value to be sent as an outbound property of the product.
     * 
     * @param key the key of the outbound property to add
     * @param value the value of the outbound property to add
     * @return this {@link TestEventBuilder}
     */
    public TestEventBuilder withOutboundProperty(String key, Object value)
    {
        outboundProperties.put(key, value);

        return this;
    }

    /**
     * Prepares a property with the given key and value to be sent as a session property of the product.
     * 
     * @param key the key of the session property to add
     * @param value the value of the session property to add
     * @return this {@link TestEventBuilder}
     */
    public TestEventBuilder withSessionProperty(String key, Object value)
    {
        sessionProperties.put(key, value);

        return this;
    }

    /**
     * Prepares a flow variable with the given key and value to be set in the product.
     * 
     * @param key the key of the flow variable to put
     * @param value the value of the flow variable to put
     * @return this {@link TestEventBuilder}
     */
    public TestEventBuilder withFlowVariable(String key, Object value)
    {
        flowVariables.put(key, value);

        return this;
    }

    public TestEventBuilder transactionally()
    {
        transacted = true;

        return this;
    }

    /**
     * Configures the product event to run as one-way.
     * 
     * @return this {@link TestEventBuilder}
     */
    public TestEventBuilder asynchronously()
    {
        exchangePattern = ONE_WAY;

        return this;
    }

    /**
     * Configures the product event to have the provided {@link ReplyToHandler}.
     * 
     * @return this {@link TestEventBuilder}
     */
    public TestEventBuilder withReplyToHandler(ReplyToHandler replyToHandler)
    {
        this.replyToHandler = replyToHandler;

        return this;
    }

    /**
     * Will spy the built {@link MuleMessage} and {@link MuleEvent}. See {@link Mockito#spy(Object) spy}.
     * 
     * @return this {@link TestEventBuilder}
     */
    public TestEventBuilder spyObjects()
    {
        spyTransformer = new Transformer()
        {

            @Override
            public Object transform(Object input)
            {
                return spy(input);
            }
        };

        return this;
    }

    /**
     * Produces an event with the specified configuration.
     * 
     * @param muleContext the context of the mule application
     * @param flow the recipient for the event to be built.
     * @return an event with the specified configuration.
     */
    public MuleEvent build(MuleContext muleContext, FlowConstruct flow)
    {
        final DefaultMuleMessage muleMessage = new DefaultMuleMessage(payload, inboundProperties, outboundProperties, Collections.emptyMap(), muleContext);
        DefaultMuleEvent event = new DefaultMuleEvent(
                (DefaultMuleMessage) spyTransformer.transform(muleMessage), URI.create("none"), "none", exchangePattern, flow, new DefaultMuleSession(),
                muleContext.getConfiguration().getDefaultResponseTimeout(), null, null, "UTF-8", transacted,
                null, replyToHandler);

        for (Entry<String, Object> sessionPropertyEntry : sessionProperties.entrySet())
        {
            muleMessage.setProperty(sessionPropertyEntry.getKey(), sessionPropertyEntry.getValue(), SESSION);
        }

        for (Entry<String, Object> flowVarEntry : flowVariables.entrySet())
        {
            event.setFlowVariable(flowVarEntry.getKey(), flowVarEntry.getValue());
        }

        return (MuleEvent) spyTransformer.transform(event);
    }

}
