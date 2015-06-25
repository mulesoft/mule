/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Creates a Map facade around a {@link org.mule.api.MuleMessage} instance to allow access to outbound
 * headers from within components and transformers without the these objects needing access to the Mule Message
 */
public class OutboundHeadersExpressionEvaluator extends AbstractExpressionEvaluator
{
    public static final String NAME = "outboundHeaders";

    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(OutboundHeadersExpressionEvaluator.class);


    public Object evaluate(String expression, MuleMessage message)
    {
        if (message == null)
        {
            return null;
        }
        return new SendHeadersMap(message);
    }

    /**
     * {@inheritDoc}
     */
    public String getName()
    {
        return NAME;
    }

    /**
     * {@inheritDoc}
     */
    public void setName(String name)
    {
        throw new UnsupportedOperationException("name");
    }


    public static class SendHeadersMap implements Map<String, Object>
    {
        private MuleMessage message;

        public SendHeadersMap(MuleMessage message)
        {
            this.message = message;
        }

        public int size()
        {
            return message.getOutboundPropertyNames().size();
        }

        public boolean isEmpty()
        {
            return message.getOutboundPropertyNames().size() == 0;
        }

        public boolean containsKey(Object key)
        {
            return message.getOutboundPropertyNames().contains(key.toString());
        }

        public boolean containsValue(Object value)
        {
            return values().contains(value);
        }

        public Object get(Object key)
        {
            return message.getOutboundProperty(key.toString());
        }

        public Object put(String key, Object value)
        {
            message.setOutboundProperty(key, value);
            return value;
        }


        public Object remove(Object key)
        {
            return message.removeProperty(key.toString(), PropertyScope.OUTBOUND);
        }


        public void putAll(Map<? extends String, ?> t)
        {
            for (Entry<? extends String, ?> entry : t.entrySet())
            {
                put(entry.getKey(), entry.getValue());
            }
        }

        public void clear()
        {
            message.clearProperties(PropertyScope.OUTBOUND);
        }

        public Set<String> keySet()
        {
            return message.getOutboundPropertyNames();
        }

        public Collection<Object> values()
        {
            return getPropertiesInScope(PropertyScope.OUTBOUND).values();
        }

        public Set<Entry<String, Object>> entrySet()
        {
            return getPropertiesInScope(PropertyScope.OUTBOUND).entrySet();
        }

        //TODO Could optimise this to cache if no writes are made
        private Map<String, Object> getPropertiesInScope(PropertyScope scope)
        {
            Map<String, Object> props = new HashMap<String, Object>();
            for (String s : message.getPropertyNames(scope))
            {
                props.put(s, message.getProperty(s, scope));
            }
            return props;
        }
    }
}
