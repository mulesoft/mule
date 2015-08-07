/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.transformer.DataType;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.TypedValue;
import org.mule.util.CaseInsensitiveHashMap;
import org.mule.util.CopyOnWriteCaseInsensitiveMap;
import org.mule.util.MapUtils;
import org.mule.util.ObjectUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This object maintains a scoped map of properties. This means that certain properties will only be visible
 * under some scopes. The scopes supported by Mule are:
 * <ol>
 * <li> {@link org.mule.api.transport.PropertyScope#INBOUND} Contains properties that were on the message when
 * it was received by Mule. This scope is read-only.</li>
 * <li>{@link org.mule.api.transport.PropertyScope#INVOCATION} Any properties set on the invocation scope will
 * be available to the current service but will not be attached to any outbound messages.</li>
 * <li>{@link org.mule.api.transport.PropertyScope#OUTBOUND} Any properties set in this scope will be attached
 * to any outbound messages resulting from this message. This is the default scope.</li>
 * <li>{@link org.mule.api.transport.PropertyScope#SESSION} Any properties set on this scope will be added to
 * the session. Note Session properties are not stored on the {@link MuleMessage}. This scope should only be
 * used once a {@link MuleEvent} has been created as there is no {@link MuleSession} and therefore Session
 * scope properties before this time</li>
 * </ol>
 */
public class MessagePropertiesContext implements Serializable
{
    private static final long serialVersionUID = -5230693402768953742L;
    private static final PropertyScope DEFAULT_SCOPE = PropertyScope.OUTBOUND;

    private static Log logger = LogFactory.getLog(MessagePropertiesContext.class);

    protected CopyOnWriteCaseInsensitiveMap<String, TypedValue> inboundMap;
    protected CopyOnWriteCaseInsensitiveMap<String, TypedValue> outboundMap;

    protected Map<String, TypedValue> invocationMap = new UndefinedInvocationPropertiesMap();
    protected transient Map<String, TypedValue> sessionMap = new UndefinedSessionPropertiesMap();

    public MessagePropertiesContext()
    {
        inboundMap = new CopyOnWriteCaseInsensitiveMap<>();
        outboundMap = new CopyOnWriteCaseInsensitiveMap<>();
    }

    public MessagePropertiesContext(MessagePropertiesContext previous)
    {
        inboundMap = previous.inboundMap.clone();
        outboundMap = previous.outboundMap.clone();
        invocationMap = previous.invocationMap;
        sessionMap = previous.sessionMap;
    }

    protected Map<String, TypedValue> getScopedProperties(PropertyScope scope)
    {
        if (PropertyScope.SESSION.equals(scope))
        {
            return sessionMap;
        }
        else if (PropertyScope.INVOCATION.equals(scope))
        {
            return invocationMap;
        }
        else if (PropertyScope.INBOUND.equals(scope))
        {
            return inboundMap;
        }
        else if (PropertyScope.OUTBOUND.equals(scope))
        {
            return outboundMap;
        }
        else
        {
            throw new IllegalArgumentException("Scope not registered: " + scope);
        }
    }

    public PropertyScope getDefaultScope()
    {
        return DEFAULT_SCOPE;
    }

    protected void addInboundProperties(Map<String, Object> properties)
    {
        if (properties != null)
        {
            Map<String, TypedValue> propertyDatas = new HashMap<>();
            for (String key : properties.keySet())
            {
                propertyDatas.put(key, new TypedValue(properties.get(key), DataType.OBJECT_DATA_TYPE));
            }

            getScopedProperties(PropertyScope.INBOUND).putAll(propertyDatas);
        }
    }

    /**
     * @deprecated use the overloaded version with an explicit lookup scope. This method will now use only the
     *             outbound scope.
     */
    @Deprecated
    public Object getProperty(String key)
    {
        return getProperty(key, PropertyScope.OUTBOUND);
    }

    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, PropertyScope scope)
    {
        if (scope == null)
        {
            scope = PropertyScope.OUTBOUND;
        }

        TypedValue typedValue = getScopedProperties(scope).get(key);

        return typedValue == null ? null : (T) typedValue.getValue();
    }

    public DataType<?> getPropertyDataType(String key, PropertyScope scope)
    {
        if (scope == null)
        {
            scope = PropertyScope.OUTBOUND;
        }

        TypedValue typedValue = getScopedProperties(scope).get(key);

        return typedValue == null ? null : typedValue.getDataType();
    }

    /**
     * Removes all properties from all scopes except for SESSION and INBOUND (which is read-only). You may
     * explicitly clear the session properties by calling clearProperties(PropertyScope.SESSION)
     */
    public void clearProperties()
    {
        Map<String, TypedValue> props = getScopedProperties(PropertyScope.INVOCATION);
        props.clear();
        props = getScopedProperties(PropertyScope.OUTBOUND);
        props.clear();
    }

    public void clearProperties(PropertyScope scope)
    {
        if (scope == null)
        {
            clearProperties();
            return;
        }

        Map<String, TypedValue> props = getScopedProperties(scope);
        props.clear();
    }

    /**
     * Removes a property from all scopes except for SESSION and INBOUND (which is read-only). You may
     * explicitly remove a session property by calling removeProperty(key, PropertyScope.SESSION)
     *
     * @param key the property key to remove
     * @return the removed property value or null if the property did not exist
     */
    public Object removeProperty(String key)
    {
        TypedValue value = getScopedProperties(PropertyScope.OUTBOUND).remove(key);
        TypedValue inv = getScopedProperties(PropertyScope.INVOCATION).remove(key);

        if (value == null)
        {
            value = inv;
        }

        return value == null ? null : value.getValue();
    }

    /**
     * Removes a property from the specified property scope.
     *
     * @param key the property key to remove
     * @return the removed property value or null if the property did not exist
     */
    public Object removeProperty(String key, PropertyScope scope)
    {
        if (scope == null)
        {
            return removeProperty(key);
        }

        TypedValue value = getScopedProperties(scope).remove(key);

        return value == null ? null : value.getValue();
    }

    /**
     * Set a property on the message
     *
     * @param key the key on which to associate the value
     * @param value the property value
     * @deprecated use {@link #setProperty(String, Object, org.mule.api.transport.PropertyScope)}
     */
    @Deprecated
    public void setProperty(String key, Object value)
    {
        getScopedProperties(DEFAULT_SCOPE).put(key, new TypedValue(value, DataTypeFactory.createFromObject(value)));
    }

    /**
     * Set a property on the message
     *
     * @param key the key on which to associate the value
     * @param value the property value
     * @param scope the scope to se the property on
     * @see org.mule.api.transport.PropertyScope
     */
    public void setProperty(String key, Object value, PropertyScope scope)
    {
        setProperty(key, value, scope, DataTypeFactory.createFromObject(value));
    }

    /**
     * Set a property on the message
     *
     * @param key the key on which to associate the value
     * @param value the property value
     * @param scope the scope to se the property on
     * @see org.mule.api.transport.PropertyScope
     */
    public void setProperty(String key, Object value, PropertyScope scope, DataType<?> dataType)
    {
        if (!(value instanceof Serializable) && PropertyScope.SESSION.equals(scope))
        {
            logger.warn(CoreMessages.sessionPropertyNotSerializableWarning(key));
        }

        getScopedProperties(scope).put(key, new TypedValue(value, dataType));
    }

    /**
     * @return all property keys on this message for the given scope
     */
    public Set<String> getPropertyNames(PropertyScope scope)
    {
        return Collections.unmodifiableSet(getScopedProperties(scope).keySet());
    }

    public Object getProperty(String key, Object defaultValue)
    {
        Object value = getProperty(key);
        if (value == null)
        {
            value = defaultValue;
        }
        return value;
    }

    public byte getByteProperty(String name, byte defaultValue)
    {
        return ObjectUtils.getByte(getProperty(name), defaultValue);
    }

    public short getShortProperty(String name, short defaultValue)
    {
        return ObjectUtils.getShort(getProperty(name), defaultValue);
    }

    public int getIntProperty(String name, int defaultValue)
    {
        return ObjectUtils.getInt(getProperty(name), defaultValue);
    }

    public long getLongProperty(String name, long defaultValue)
    {
        return ObjectUtils.getLong(getProperty(name), defaultValue);
    }

    public float getFloatProperty(String name, float defaultValue)
    {
        return ObjectUtils.getFloat(getProperty(name), defaultValue);
    }

    public double getDoubleProperty(String name, double defaultValue)
    {
        return ObjectUtils.getDouble(getProperty(name), defaultValue);
    }

    public boolean getBooleanProperty(String name, boolean defaultValue)
    {
        return ObjectUtils.getBoolean(getProperty(name), defaultValue);
    }

    @Deprecated
    public String getStringProperty(String name, String defaultValue)
    {
        return getStringProperty(name, PropertyScope.OUTBOUND, defaultValue);
    }

    public String getStringProperty(String name, PropertyScope scope, String defaultValue)
    {
        return ObjectUtils.getString(getProperty(name, scope), defaultValue);
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

    /**
     * Check for properties that can't be serialized
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException
    {
        for (Map.Entry<String, TypedValue> entry : inboundMap.entrySet())
        {
            Object value = entry.getValue().getValue();
            if (value != null && !(value instanceof Serializable))
            {
                String message = String.format(
                    "Unable to serialize the %s message property %s, which is of type %s ",
                    PropertyScope.INBOUND, entry.getKey(), value);
                logger.error(message);
                throw new IOException(message);
            }
        }
        for (Map.Entry<String, TypedValue> entry : outboundMap.entrySet())
        {
            Object value = entry.getValue().getValue();
            if (value != null && !(value instanceof Serializable))
            {
                String message = String.format(
                    "Unable to serialize the %s message property %s, which is of type %s ",
                    PropertyScope.OUTBOUND, entry.getKey(), value);
                logger.error(message);
                throw new IOException(message);
            }
        }
        if (invocationMap instanceof UndefinedInvocationPropertiesMap)
        {
            for (Map.Entry<String, TypedValue> entry : invocationMap.entrySet())
            {
                Object value = entry.getValue().getValue();
                if (value != null && !(value instanceof Serializable))
                {
                    String message = String.format(
                        "Unable to serialize the invocation message property %s, which is of type %s ",
                        entry.getKey(), value);
                    logger.error(message);
                    throw new IOException(message);
                }
            }
        }
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        sessionMap = new UndefinedSessionPropertiesMap();
    }

    private static class UndefinedSessionPropertiesMap extends AbstractMap<String, TypedValue>
        implements Serializable
    {

        private static final long serialVersionUID = -7982608304570908737L;

        @Override
        public Set<java.util.Map.Entry<String, TypedValue>> entrySet()
        {
            return Collections.emptySet();
        }

        @Override
        public TypedValue put(String key, TypedValue value)
        {
            throw new IllegalStateException(
                String.format(
                    "Detected an attempt to set a invocation or session property, "
                                    + "but a MuleEvent hasn't been created using this message yet. Key/value: %s=%s",
                    key, value));
        }

        @Override
        public TypedValue get(Object key)
        {
            logger.warn(String.format(
                "Detected an attempt to get a invocation or session property, "
                                + "but a MuleEvent hasn't been created using this message yet. Key: %s", key));
            return null;
        }
    }

    private static class UndefinedInvocationPropertiesMap extends CaseInsensitiveHashMap
    {
        private static final long serialVersionUID = 8400889672358403911L;

    }

    protected Map<String, TypedValue> getOrphanFlowVariables()
    {
        if (invocationMap instanceof UndefinedInvocationPropertiesMap)
        {
            return invocationMap;
        }
        else
        {
            return Collections.emptyMap();
        }
    }
}
