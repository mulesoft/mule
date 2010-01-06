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

import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleSession;
import org.mule.api.transport.PropertyScope;
import org.mule.util.MapUtils;
import org.mule.util.ObjectUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * This object maintains a scoped map of properties.  This means that certain properties will only be visible under some
 * scopes. The scopes supported by Mule are:
 * <ol>
 * <li> {@link org.mule.api.transport.PropertyScope#INBOUND} Contains properties that were on the message when it was
 * received by Mule.  This scope is read-only.</li>
 * <li>{@link org.mule.api.transport.PropertyScope#INVOCATION} Any properties set on the invocation scope will be
 * available to the current service but will not be attached to any outbound messages.</li>
 * <li>{@link org.mule.api.transport.PropertyScope#OUTBOUND} Any properties set in this scope will be attached to any
 * outbound messages resulting from this message.  This is the default scope.</li>
 * <li>{@link org.mule.api.transport.PropertyScope#SESSION} Any properties set on this scope will be added to the session.
 * Note that this is a convenience scope in that you cannot directly access session properties from this scope.  Session
 * properties can be accessed from the {@link MuleEvent}</li>
 * </ol>
 */
public class MessagePropertiesContext implements Serializable
{
    private static final long serialVersionUID = -5230693402768953742L;

    /**
     * The order that properties should be read in.
     */
    private final static List<PropertyScope> SCOPE_ORDER = new ArrayList<PropertyScope>();
    
    static
    {
        SCOPE_ORDER.add(PropertyScope.OUTBOUND);
        SCOPE_ORDER.add(PropertyScope.INVOCATION);
        SCOPE_ORDER.add(PropertyScope.INBOUND);
        SCOPE_ORDER.add(PropertyScope.SESSION);
    }
    
    /**
     * Map of maps containing the scoped properties, each scope has its own Map.
     */
    protected Map<PropertyScope, Map<String, Object>> scopedMap;
    
    /**
     * The union of all property names from all scopes.
     */
    protected Set<String> keySet;

    protected PropertyScope defaultScope = PropertyScope.OUTBOUND;
    
    public MessagePropertiesContext()
    {
        keySet = new TreeSet<String>();
        scopedMap = new TreeMap<PropertyScope, Map<String, Object>>(new PropertyScope.ScopeComparator());

        scopedMap.put(PropertyScope.INVOCATION, new HashMap<String, Object>(6));
        scopedMap.put(PropertyScope.INBOUND, new HashMap<String, Object>(6));
        scopedMap.put(PropertyScope.OUTBOUND, new HashMap<String, Object>(6));
    }

    /**
     * Ctor used for copying only
     * @param defaultScope
     * @param keySet
     * @param scopedMap
     */
    private MessagePropertiesContext(PropertyScope defaultScope, Set<String> keySet, 
        Map<PropertyScope, Map<String, Object>> scopedMap)
    {
        this.keySet = keySet;
        this.scopedMap = scopedMap;
        this.defaultScope = defaultScope;
    }

    protected Map<String, Object> getScopedProperties(PropertyScope scope)
    {
        Map<String, Object> map = scopedMap.get(scope);
        if (map == null)
        {
            map = null;
            throw new IllegalArgumentException("Scope not registered: " + scope);
        }
        return map;
    }

    protected void registerInvocationProperties(Map<String, Object> properties)
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

    protected void addInboundProperties(Map<String, Object> properties)
    {
        if (properties != null)
        {
            getScopedProperties(PropertyScope.INBOUND).putAll(properties);
            keySet.addAll(properties.keySet());
        }
    }

    protected void registerSessionProperties(Map<String, Object> properties)
    {
        if (properties != null)
        {
            if (RequestContext.getEvent() != null)
            {
                for (Object key : properties.keySet())
                {
                    RequestContext.getEvent().getSession().setProperty(key, properties.get(key));
                }
            }
        }
    }

    public Object getProperty(String key)
    {
        Object value = null;
        for (PropertyScope scope : SCOPE_ORDER)
        {
            if (PropertyScope.SESSION.equals(scope))
            {
                if (RequestContext.getEvent() != null)
                {
                    value = RequestContext.getEvent().getSession().getProperty(key);
                }
            }
            else
            {
                value = scopedMap.get(scope).get(key);
            }
            if (value != null)
            {
                break;
            }
        }
        return value;
    }

    public Object getProperty(String key, PropertyScope scope)
    {
        if (scope == null)
        {
            return getProperty(key);
        }
        
        Object value = null;        
        if (PropertyScope.SESSION.equals(scope))
        {
            if (RequestContext.getEvent() != null)
            {
                value = RequestContext.getEvent().getSession().getProperty(key);
            }
        }
        else
        {
            value = scopedMap.get(scope).get(key);
        }
        return value;
    }

    /**
     * Removes all properties from all scopes except for SESSION and INBOUND (which is read-only).
     * You may explicitly clear the session properties by calling clearProperties(PropertyScope.SESSION)
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
        
        checkScopeForWriteAccess(scope);
        if (PropertyScope.SESSION.equals(scope))
        {
            if (RequestContext.getEvent() != null)
            {
                MuleSession session = RequestContext.getEvent().getSession();
                for (Object key : session.getPropertyNamesAsSet())
                {
                    session.removeProperty(key);
                }
            }
        }
        else
        {
            Map<String, Object> props = getScopedProperties(scope);
            keySet.removeAll(props.keySet());
            props.clear();
        }
    }

    /**
     * Removes a property from all scopes except for SESSION and INBOUND (which is read-only).
     * You may explicitly remove a session property by calling removeProperty(key, PropertyScope.SESSION)
     *
     * @param key the property key to remove
     * @return the removed property value or null if the property did not exist
     */
    public Object removeProperty(String key)
    {
        Object value = getScopedProperties(PropertyScope.OUTBOUND).remove(key);
        Object inv = getScopedProperties(PropertyScope.INVOCATION).remove(key);
    
        keySet.remove(key);
      
        if (value == null) value = inv;
        
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
        
        Object value = null;
        if (PropertyScope.SESSION.equals(scope))
        {
            if (RequestContext.getEvent() != null)
            {
                value = RequestContext.getEvent().getSession().removeProperty(key);
            }
        }
        else
        {
            value = getScopedProperties(scope).remove(key);
        }

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
        if (scope == null)
        {
            setProperty(key, value);
            return;
        }
        
        checkScopeForWriteAccess(scope);
        if (PropertyScope.SESSION.equals(scope))
        {
            if (RequestContext.getEvent() != null)
            {
                RequestContext.getEvent().getSession().setProperty(key, value);
            }
        }
        else
        {
            getScopedProperties(scope).put(key, value);
            keySet.add(key);
        }
    }

    /** @return all property keys on this message */
    public Set<String> getPropertyNames()
    {
        Set<String> allProps = new HashSet<String>();
        allProps.addAll(keySet);
        if (RequestContext.getEvent() != null)
        {
            allProps.addAll(RequestContext.getEvent().getSession().getPropertyNamesAsSet());
        }
        return allProps;
    }

    /** @return all property keys on this message for the given scope */
    public Set<String> getPropertyNames(PropertyScope scope)
    {
        if (scope == null)
        {
            return getPropertyNames();
        }
        
        if (PropertyScope.SESSION.equals(scope))
        {
            if (RequestContext.getEvent() != null)
            {
                return RequestContext.getEvent().getSession().getPropertyNamesAsSet();
            }
            else
            {
                return Collections.emptySet();
            }
        }
        else
        {
            return Collections.unmodifiableSet(getScopedProperties(scope).keySet());
        }
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

    public String getStringProperty(String name, String defaultValue)
    {
        return ObjectUtils.getString(getProperty(name), defaultValue);
    }

    protected MessagePropertiesContext copy()
    {
        Set<String> keys = new TreeSet<String>(getPropertyNames());

        Map<PropertyScope, Map<String, Object>> map = new TreeMap<PropertyScope, 
            Map<String, Object>>(new PropertyScope.ScopeComparator());

        map.put(PropertyScope.INVOCATION, 
            new HashMap<String, Object>(getScopedProperties(PropertyScope.INVOCATION)));
        map.put(PropertyScope.INBOUND, 
            new HashMap<String, Object>(getScopedProperties(PropertyScope.INBOUND)));
        map.put(PropertyScope.OUTBOUND, 
            new HashMap<String, Object>(getScopedProperties(PropertyScope.OUTBOUND)));

        return new MessagePropertiesContext(getDefaultScope(), keys, scopedMap);
    }

    @Override
    public String toString()
    {
        StringBuffer buf = new StringBuffer(128);
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
}
