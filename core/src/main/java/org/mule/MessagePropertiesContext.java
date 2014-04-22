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
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.CaseInsensitiveHashMap;
import org.mule.util.MapUtils;
import org.mule.util.ObjectUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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

    /**
     * Map of maps containing the scoped properties, each scope has its own Map.
     */
    protected Map<PropertyScope, Map<String, Object>> scopedMap;

    protected Map<String, Object> invocationMap = new UndefinedInvocationPropertiesMap();
    protected transient Map<String, Object> sessionMap = new UndefinedSessionPropertiesMap();

    /**
     * The union of all property names from all scopes.
     */
    protected Set<String> keySet;

    @SuppressWarnings("unchecked")
    public MessagePropertiesContext()
    {
        keySet = new TreeSet<String>();
        scopedMap = new TreeMap<PropertyScope, Map<String, Object>>(new PropertyScope.ScopeComparator());
        scopedMap.put(PropertyScope.INBOUND, new CaseInsensitiveHashMap/* <String, Object> */(6));
        scopedMap.put(PropertyScope.OUTBOUND, new CaseInsensitiveHashMap/* <String, Object> */(6));
    }

    protected Map<String, Object> getScopedProperties(PropertyScope scope)
    {
        if (PropertyScope.SESSION.equals(scope))
        {
            return sessionMap;
        }
        else if (PropertyScope.INVOCATION.equals(scope))
        {
            return invocationMap;
        }
        else
        {
            Map<String, Object> map = scopedMap.get(scope);
            if (map == null)
            {
                throw new IllegalArgumentException("Scope not registered: " + scope);
            }
            return map;
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
            getScopedProperties(PropertyScope.INBOUND).putAll(properties);
            keySet.addAll(properties.keySet());
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

        return (T) getScopedProperties(scope).get(key);
    }

    /**
     * Removes all properties from all scopes except for SESSION and INBOUND (which is read-only). You may
     * explicitly clear the session properties by calling clearProperties(PropertyScope.SESSION)
     */
    public void clearProperties()
    {
        Map<String, Object> props = getScopedProperties(PropertyScope.INVOCATION);
        keySet.removeAll(props.keySet());
        props.clear();
        props = getScopedProperties(PropertyScope.OUTBOUND);
        keySet.removeAll(props.keySet());
        props.clear();
    }

    public void clearProperties(PropertyScope scope)
    {
        if (scope == null)
        {
            clearProperties();
            return;
        }

        Map<String, Object> props = getScopedProperties(scope);
        keySet.removeAll(props.keySet());
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
        Object value = getScopedProperties(PropertyScope.OUTBOUND).remove(key);
        Object inv = getScopedProperties(PropertyScope.INVOCATION).remove(key);

        keySet.remove(key);

        if (value == null)
        {
            value = inv;
        }

        return value;
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

        Object value = getScopedProperties(scope).remove(key);

        // Only remove the property from the keySet if it does not exist in any other scope besides this one.
        if (getProperty(key, PropertyScope.OUTBOUND) == null
            && getProperty(key, PropertyScope.INVOCATION) == null
            && getProperty(key, PropertyScope.INBOUND) == null)
        {
            keySet.remove(key);
        }

        return value;
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
        getScopedProperties(DEFAULT_SCOPE).put(key, value);
        keySet.add(key);
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
        if (!(value instanceof Serializable) && PropertyScope.SESSION.equals(scope))
        {
            logger.warn(CoreMessages.sessionPropertyNotSerializableWarning(key));
        }

        getScopedProperties(scope).put(key, value);
        keySet.add(key);
    }

    /**
     * @deprecated use {@link #getPropertyNames(org.mule.api.transport.PropertyScope)}
     */
    @Deprecated
    public Set<String> getPropertyNames()
    {
        Set<String> allProps = new HashSet<String>();
        allProps.addAll(keySet);
        return allProps;
    }

    /**
     * @return all property keys on this message for the given scope
     */
    public Set<String> getPropertyNames(PropertyScope scope)
    {
        return Collections.unmodifiableSet(getScopedProperties(scope).keySet());
    }

    protected void checkScopeForWriteAccess(PropertyScope scope)
    {
        if (scope == null || PropertyScope.INBOUND.equals(scope))
        {
            throw new IllegalArgumentException("Scope is invalid for writing properties: " + scope);
        }
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
        for (Map.Entry<PropertyScope, Map<String, Object>> entry : scopedMap.entrySet())
        {
            buf.append(entry.getKey()).append(":");
            buf.append(MapUtils.toString(entry.getValue(), false));
            buf.append(", ");
        }
        buf.append("}");
        return buf.toString();
    }

    /**
     * Check for properties that can't be serialized
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException
    {
        for (PropertyScope scope : new PropertyScope[]{PropertyScope.INBOUND, PropertyScope.OUTBOUND})
        {
            for (Map.Entry<String, Object> entry : scopedMap.get(scope).entrySet())
            {
                Object value = entry.getValue();
                if (value != null && !(value instanceof Serializable))
                {
                    String message = String.format(
                        "Unable to serialize the %s message property %s, which is of type %s ", scope,
                        entry.getKey(), value);
                    logger.error(message);
                    throw new IOException(message);
                }
            }
        }
        if (invocationMap instanceof UndefinedInvocationPropertiesMap)
        {
            for (Map.Entry<String, Object> entry : invocationMap.entrySet())
            {
                Object value = entry.getValue();
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

    private static class UndefinedSessionPropertiesMap extends AbstractMap<String, Object>
        implements Serializable
    {

        private static final long serialVersionUID = -7982608304570908737L;

        @Override
        public Set<java.util.Map.Entry<String, Object>> entrySet()
        {
            return Collections.emptySet();
        }

        @Override
        public Object put(String key, Object value)
        {
            throw new IllegalStateException(
                String.format(
                    "Detected an attempt to set a invocation or session property, "
                                    + "but a MuleEvent hasn't been created using this message yet. Key/value: %s=%s",
                    key, value));
        }

        @Override
        public Object get(Object key)
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

}
