/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el.mvel;

import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.CoreMessages;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.mvel2.ImmutableElementException;

class MessagePropertyWrapperMap implements Map<String, Object>
{
    private MuleMessage message;
    private PropertyScope propertyScope;

    public MessagePropertyWrapperMap(MuleMessage message, PropertyScope propertyScope)
    {
        this.message = message;
        this.propertyScope = propertyScope;
    }

    @Override
    public void clear()
    {
        message.clearProperties(propertyScope);
    }

    @Override
    public boolean containsKey(Object key)
    {
        return message.getPropertyNames(propertyScope).contains(key);
    }

    @Override
    public boolean containsValue(Object value)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<java.util.Map.Entry<String, Object>> entrySet()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object get(Object key)
    {
        return message.getProperty((String) key, propertyScope);
    }

    @Override
    public boolean isEmpty()
    {
        return message.getPropertyNames(propertyScope).isEmpty();
    }

    @Override
    public Set<String> keySet()
    {
        return message.getPropertyNames(propertyScope);
    }

    @Override
    public Object put(String key, Object value)
    {
        if (PropertyScope.INBOUND.equals(propertyScope))
        {
            throw new ImmutableElementException(CoreMessages.inboundMessagePropertiesImmutable(key)
                .getMessage());
        }
        else
        {
            Object previousValue = get(key);
            message.setProperty(key, value, propertyScope);
            return previousValue;
        }
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> m)
    {
        for (Map.Entry<? extends String, ? extends Object> entry : m.entrySet())
        {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public Object remove(Object key)
    {
        if (PropertyScope.INBOUND.equals(propertyScope))
        {
            throw new UnsupportedOperationException(CoreMessages.inboundMessagePropertiesImmutable(key)
                .getMessage());
        }
        else
        {
            return message.removeProperty((String) key, propertyScope);
        }
    }

    @Override
    public int size()
    {
        return message.getPropertyNames(propertyScope).size();
    }

    @Override
    public Collection<Object> values()
    {
        throw new UnsupportedOperationException();
    }

}
