/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el.context;

import org.mule.api.MuleEvent;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.CoreMessages;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MessagePropertyMapContext extends AbstractMapContext<String, Object>
{
    private MuleEvent event;
    private PropertyScope propertyScope;

    public MessagePropertyMapContext(MuleEvent event, PropertyScope propertyScope)
    {
        this.event = event;
        this.propertyScope = propertyScope;
    }

    @Override
    public Object get(Object key)
    {
        if (!(key instanceof String))
        {
            return null;
        }
        return event.getMessage().getProperty((String) key, propertyScope);
    }

    @Override
    public Object put(String key, Object value)
    {
        if (PropertyScope.INBOUND.equals(propertyScope))
        {
            throw new UnsupportedOperationException(CoreMessages.inboundMessagePropertiesImmutable(key)
                .getMessage());
        }
        else
        {
            Object previousValue = get(key);
            event.getMessage().setProperty(key, value, propertyScope);
            return previousValue;
        }
    }

    @Override
    public Object remove(Object key)
    {
        if (!(key instanceof String))
        {
            return null;
        }

        if (PropertyScope.INBOUND.equals(propertyScope))
        {
            throw new UnsupportedOperationException(CoreMessages.inboundMessagePropertiesImmutable(key)
                .getMessage());
        }
        else
        {
            return event.getMessage().removeProperty((String) key, propertyScope);
        }
    }

    @Override
    public Set<String> keySet()
    {
        return event.getMessage().getPropertyNames(propertyScope);
    }

    @Override
    public void clear()
    {
        if (PropertyScope.INBOUND.equals(propertyScope))
        {
            throw new UnsupportedOperationException(CoreMessages.inboundMessagePropertiesImmutable()
                .getMessage());
        }
        event.getMessage().clearProperties(propertyScope);
    }

    @Override
    public String toString()
    {
        Map<String, Object> map = new HashMap<String, Object>();
        for (String key : event.getMessage().getPropertyNames(propertyScope))
        {
            Object value = event.getMessage().getProperty(key, propertyScope);
            map.put(key, value);
        }
        return map.toString();
    }
}
