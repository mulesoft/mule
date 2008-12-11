/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport;

import org.mule.api.transport.PropertyScope;
import org.mule.util.MapUtils;
import org.mule.util.ObjectUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * This object maintains a scoped map of properties.  This means that certian properties will only be visiable under some
 * scopes. The scopes support by Mule are:
 * <ol>
 * <li> {@link org.mule.api.transport.PropertyScope.INBOUND} Contains properties that were on the message when it was
 * received by Mule. this scope is read-only.</li>
 * <li>{@link org.mule.api.transport.PropertyScope.INVOCATION} Any properties set on the invocation scope will be
 * available to the current service but will not be attached to any outound messages.  This is the default scope.</li>
 * <li>{@link org.mule.api.transport.PropertyScope.OUTBOUND} Any properties set in this scope will be attached to any
 * outbound messages resulting from this message</li>
 * <li>{@link org.mule.api.transport.PropertyScope.SESSION} Any properties set on this scope will be added to the session.
 * Note that this is a convinience scope in that you cannot directly access session properties from this scope.  Session
 * properties can be accessed from the {@link MuleEvent}</li>
 * </ol>
 */
public class MessagePropertiesContext implements Serializable
{
    protected Map scopedMap;
    protected Set keySet;

    protected PropertyScope defaultScope = PropertyScope.OUTBOUND;

    public MessagePropertiesContext()
    {
        keySet = new TreeSet();
        scopedMap = new TreeMap(new PropertyScope.ScopeComparator());

        scopedMap.put(PropertyScope.INVOCATION, new HashMap(6));
        scopedMap.put(PropertyScope.INBOUND, new HashMap(6));
        scopedMap.put(PropertyScope.OUTBOUND, new HashMap(6));
        scopedMap.put(PropertyScope.SESSION, new HashMap(6));

    }

    public MessagePropertiesContext(PropertyScope defaultScope)
    {
        this();
        //We can't set a read only scope as default
        checkScopeForWriteAccess(defaultScope);
        this.defaultScope = defaultScope;
    }

    /**
     * Ctor used for copying only
     * @param defaultScope
     * @param keySet
     * @param scopedMap
     */
    private MessagePropertiesContext(PropertyScope defaultScope, Set keySet, Map scopedMap)
    {
        this.keySet = keySet;
        this.scopedMap = scopedMap;
        this.defaultScope = defaultScope;
    }

    protected Map getScopedProperties(PropertyScope scope)
    {
        Map map = (Map) scopedMap.get(scope);
        if (map == null)
        {
            throw new IllegalArgumentException("Scope not registered: " + scope);
        }
        return map;
    }

    protected void registerInvocationProperties(Map properties)
    {
        if (properties != null)
        {
            getScopedProperties(PropertyScope.INVOCATION).putAll(properties);
            keySet.addAll(properties.keySet());
        }
    }

    public PropertyScope getDefaultScope()
    {
        return defaultScope;
    }

    protected void addInboundProperties(Map properties)
    {
        if (properties != null)
        {
            getScopedProperties(PropertyScope.INBOUND).putAll(properties);
            keySet.addAll(properties.keySet());
        }
    }

    protected void registerSessionProperties(Map properties)
    {
        if (properties != null)
        {
            getScopedProperties(PropertyScope.SESSION).putAll(properties);
            keySet.addAll(properties.keySet());
        }
    }


    public Object getProperty(String key)
    {
        Object value = null;
        for (Iterator iterator = scopedMap.values().iterator(); iterator.hasNext();)
        {
            Map props = (Map) iterator.next();
            value = props.get(key);
            if (value != null)
            {
                break;
            }
        }
        return value;
    }

    public Object getProperty(String key, PropertyScope scope)
    {
        Map props = getScopedProperties(scope);
        return props.get(key);
    }

    public void clearProperties()
    {
        Map props = getScopedProperties(PropertyScope.INVOCATION);
        keySet.removeAll(props.keySet());
        props.clear();
        props = getScopedProperties(PropertyScope.OUTBOUND);
        keySet.removeAll(props.keySet());
        props.clear();
        props = getScopedProperties(PropertyScope.SESSION);
        keySet.removeAll(props.keySet());
        props.clear();
        //inbound are read Only
    }

    public void clearProperties(PropertyScope scope)
    {
        checkScopeForWriteAccess(scope);
        Map props = getScopedProperties(scope);
        keySet.removeAll(props.keySet());
        props.clear();
    }

    /**
     * Removes a property on this message
     *
     * @param key the property key to remove
     * @return the removed property value or null if the property did not exist
     */
    public Object removeProperty(String key)
    {
        Object value = getScopedProperties(PropertyScope.INVOCATION).remove(key);
        if (value == null)
        {
            value = getScopedProperties(PropertyScope.OUTBOUND).remove(key);
        }
        if (value == null)
        {
            value = getScopedProperties(PropertyScope.SESSION).remove(key);
        }
        if (value != null)
        {
            keySet.remove(key);
        }
        return value;
    }

    /**
     * Set a property on the message
     *
     * @param key   the key on which to associate the value
     * @param value the property value
     */
    public void setProperty(String key, Object value)
    {
        getScopedProperties(defaultScope).put(key, value);
        keySet.add(key);
    }

    /**
     * Set a property on the message
     *
     * @param key   the key on which to associate the value
     * @param value the property value
     * @param scope the scope to se the property on
     * @see org.mule.api.transport.PropertyScope
     */
    public void setProperty(String key, Object value, PropertyScope scope)
    {
        checkScopeForWriteAccess(scope);
        getScopedProperties(scope).put(key, value);
        keySet.add(key);
    }

    /** @return all property keys on this message */
    public Set getPropertyNames()
    {
        return Collections.unmodifiableSet(keySet);
    }

    /** @return all property keys on this message for the given scope */
    public Set getPropertyNames(PropertyScope scope)
    {
        return Collections.unmodifiableSet(getScopedProperties(scope).keySet());
    }

    protected void checkScopeForWriteAccess(PropertyScope scope)
    {
        if (scope == null || PropertyScope.INBOUND.equals(scope) || PropertyScope.APPLICATION.equals(scope))
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

    public String getStringProperty(String name, String defaultValue)
    {
        return ObjectUtils.getString(getProperty(name), defaultValue);
    }

    protected MessagePropertiesContext copy()
    {
        Set<String> keySet = new TreeSet<String>(getPropertyNames());

        Map scopedMap = new TreeMap(new PropertyScope.ScopeComparator());

        scopedMap.put(PropertyScope.INVOCATION, new HashMap(getScopedProperties(PropertyScope.INVOCATION)));
        scopedMap.put(PropertyScope.INBOUND, new HashMap(getScopedProperties(PropertyScope.INBOUND)));
        scopedMap.put(PropertyScope.OUTBOUND, new HashMap(getScopedProperties(PropertyScope.OUTBOUND)));
        scopedMap.put(PropertyScope.SESSION, new HashMap(getScopedProperties(PropertyScope.SESSION)));

        return new MessagePropertiesContext(getDefaultScope(), keySet, scopedMap);
    }

    public String toString()
    {
        StringBuffer buf = new StringBuffer(128);
        buf.append("Properites{");
        for (Iterator iterator = scopedMap.entrySet().iterator(); iterator.hasNext();)
        {
            Map.Entry entry = (Map.Entry) iterator.next();
            buf.append(entry.getKey()).append(":");
            buf.append(MapUtils.toString((Map) entry.getValue(), false));
            buf.append(", ");
        }
        buf.append("}");
        return buf.toString();
    }
}
