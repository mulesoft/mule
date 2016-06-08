/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MutableMessageProperties;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.transformer.types.DataTypeFactory;
import org.mule.runtime.core.transformer.types.TypedValue;
import org.mule.runtime.core.util.CopyOnWriteCaseInsensitiveMap;
import org.mule.runtime.core.util.MapUtils;
import org.mule.runtime.core.util.ObjectUtils;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This object maintains case-sensitive inbound and outbound scoped messages properties.
 * <ol>
 * <li> {@link PropertyScope#INBOUND} Contains properties that were on the message when
 * it was received by Mule. This scope is read-only.</li>
 * <li>{@link PropertyScope#OUTBOUND} Any properties set in this scope will be attached
 * to any outbound messages resulting from this message. This is the default scope.</li>
 * </ol>
 */
public class MessagePropertiesContext implements MutableMessageProperties, Serializable
{
    private static final long serialVersionUID = -5230693402768953742L;
    private static final Log logger = LogFactory.getLog(MessagePropertiesContext.class);


    protected CopyOnWriteCaseInsensitiveMap<String, TypedValue<? extends Serializable>> inboundMap;
    protected CopyOnWriteCaseInsensitiveMap<String, TypedValue<? extends Serializable>> outboundMap;

    public MessagePropertiesContext()
    {
        inboundMap = new CopyOnWriteCaseInsensitiveMap<>();
        outboundMap = new CopyOnWriteCaseInsensitiveMap<>();
    }

    public MessagePropertiesContext(MessagePropertiesContext previous)
    {
        inboundMap = previous.inboundMap.clone();
        outboundMap = previous.outboundMap.clone();
    }

    @Override
    public <T extends Serializable> T getInboundProperty(String name)
    {
        return getInboundProperty(name, null);
    }

    @Override
    public <T extends Serializable> T getInboundProperty(String name, T defaultValue)
    {
        TypedValue typedValue = inboundMap.get(name);
        return getValueOrDefault(typedValue == null ? null : (T) typedValue.getValue(), defaultValue);
    }

    @Override
    public <T extends Serializable> T getOutboundProperty(String name)
    {
        return getOutboundProperty(name, null);
    }

    @Override
    public <T extends Serializable> T getOutboundProperty(String name, T defaultValue)
    {
        TypedValue typedValue = outboundMap.get(name);
        return getValueOrDefault(typedValue == null ? null : (T) typedValue.getValue(), defaultValue);
    }

    @Override
    public void setInboundProperty(String key, Serializable value)
    {
        setInboundProperty(key, value, DataTypeFactory.createFromObject(value));
    }

    @Override
    public <T extends Serializable> void setInboundProperty(String key, T value, DataType<T> dataType)
    {
        if (key != null)
        {
            if (value == null || value instanceof NullPayload)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("setProperty(key, value) called with null value; removing key: " + key);
                }
                removeInboundProperty(key);
            }
            else
            {
                inboundMap.put(key, new TypedValue(value, dataType));
            }
        }
        else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("setProperty(key, value) invoked with null key. Ignoring this entry");
            }
        }
    }

    @Override
    public void addInboundProperties(Map<String, Serializable> properties)
    {
        if (properties != null)
        {
            synchronized (properties)
            {
                for (Map.Entry<String, Serializable> entry : properties.entrySet())
                {
                    setInboundProperty(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    @Override
    public void setOutboundProperty(String key, Serializable value)
    {
        setOutboundProperty(key, value, DataTypeFactory.createFromObject(value));
    }

    @Override
    public <T extends Serializable> void setOutboundProperty(String key, T value, DataType<T> dataType)
    {
        if (key != null)
        {
            if (value == null || value instanceof NullPayload)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("setProperty(key, value) called with null value; removing key: " + key);
                }
                removeOutboundProperty(key);
            }
            else
            {
                outboundMap.put(key, new TypedValue(value, dataType));
            }
        }
        else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("setProperty(key, value) invoked with null key. Ignoring this entry");
            }
        }
    }

    @Override
    public void addOutboundProperties(Map<String, Serializable> properties)
    {
        if (properties != null)
        {
            synchronized (properties)
            {
                for (Map.Entry<String, Serializable> entry : properties.entrySet())
                {
                    setOutboundProperty(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    @Override
    public <T extends Serializable> T removeInboundProperty(String key)
    {
        TypedValue value = inboundMap.remove(key);
        return value == null ? null : (T) value.getValue();
    }

    @Override
    public <T extends Serializable> T removeOutboundProperty(String key)
    {
        TypedValue value = outboundMap.remove(key);
        return value == null ? null : (T) value.getValue();
    }

    @Override
    public void clearInboundProperties()
    {
        inboundMap.clear();
    }

    @Override
    public void clearOutboundProperties()
    {
        outboundMap.clear();
    }

    @Override
    public void copyProperty(String key)
    {
        outboundMap.put(key, new TypedValue(getInboundProperty(key), getInboundPropertyDataType(key)));
    }

    @Override
    public DataType<? extends Serializable> getInboundPropertyDataType(String name)
    {
        TypedValue typedValue = inboundMap.get(name);
        return typedValue == null ? null : typedValue.getDataType();
    }

    @Override
    public DataType<? extends Serializable> getOutboundPropertyDataType(String name)
    {
        TypedValue typedValue = outboundMap.get(name);
        return typedValue == null ? null : typedValue.getDataType();
    }

    @Override
    public Set<String> getInboundPropertyNames()
    {
        return inboundMap.keySet();
    }

    @Override
    public Set<String> getOutboundPropertyNames()
    {
        return outboundMap.keySet();
    }

    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder(128);
        buf.append("Properties{");
        buf.append(PropertyScope.INBOUND_NAME).append(":");
        buf.append(MapUtils.toString(inboundMap, false));
        buf.append(", ");
        buf.append(PropertyScope.OUTBOUND_NAME).append(":");
        buf.append(MapUtils.toString(inboundMap, false));
        buf.append("}");
        return buf.toString();
    }

    private <T extends Serializable> T getValueOrDefault(T value, T defaultValue)
    {
        //Note that we need to keep the (redundant) casts in here because the compiler compiler complains
        //about primitive types being cast to a generic type
        if (defaultValue == null)
        {
            return value;
        }
        else if (defaultValue instanceof Boolean)
        {
            return  (T) (Boolean) ObjectUtils.getBoolean(value, (Boolean) defaultValue);
        }
        else if (defaultValue instanceof Byte)
        {
            return (T) (Byte) ObjectUtils.getByte(value, (Byte) defaultValue);
        }
        else if (defaultValue instanceof Integer)
        {
            return (T) (Integer) ObjectUtils.getInt(value, (Integer) defaultValue);
        }
        else if (defaultValue instanceof Short)
        {
            return (T) (Short) ObjectUtils.getShort(value, (Short) defaultValue);
        }
        else if (defaultValue instanceof Long)
        {
            return (T) (Long) ObjectUtils.getLong(value, (Long) defaultValue);
        }
        else if (defaultValue instanceof Float)
        {
            return (T) (Float) ObjectUtils.getFloat(value, (Float) defaultValue);
        }
        else if (defaultValue instanceof Double)
        {
            return (T) (Double) ObjectUtils.getDouble(value, (Double) defaultValue);
        }
        else if (defaultValue instanceof String)
        {
            return (T) ObjectUtils.getString(value, (String) defaultValue);
        }
        else
        {
            if (value == null)
            {
                return defaultValue;
            }
            //If defaultValue is set and the result is not null, then validate that they are assignable
            else if (defaultValue.getClass().isAssignableFrom(value.getClass()))
            {
                return value;
            }
            else
            {
                throw new IllegalArgumentException(CoreMessages.objectNotOfCorrectType(value.getClass(), defaultValue.getClass()).getMessage());
            }
        }
    }

}
