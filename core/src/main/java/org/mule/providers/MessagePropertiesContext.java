/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers;

import org.mule.umo.provider.PropertyScope;
import org.mule.util.MapUtils;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** TODO */
public class MessagePropertiesContext implements Serializable
{
    protected Map scopedMap;
    protected Set keySet;

    //TODO RM*: Map these to a RegistryMapView, currently in another branch :(
    //Treat Application properites as a special call
    Map applicationProperties = new ConcurrentHashMap(0);

    private PropertyScope defaultScope = PropertyScope.OUTBOUND;

    public MessagePropertiesContext()
    {
        keySet = new TreeSet();
        scopedMap = new TreeMap(new PropertyScope.ScopeComarator());
        
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
     * if a property is not available in any ther scope, should we check the registry.
     * Note there will be performance implementations is this is enabled
     */
    private boolean fallbackToRegistry = false;

    protected Map getScopedProperties(PropertyScope scope)
    {
        Map map = (Map) scopedMap.get(scope);
        if (map == null)
        {
            throw new IllegalArgumentException("Scope not registered: " + scope);
        }
        return map;
    }

    void registerInvocationProperties(Map properties)
    {
        if (properties != null)
        {
            getScopedProperties(PropertyScope.INVOCATION).putAll(properties);
            keySet.addAll(properties.keySet());
        }
    }

    void addInboundProperties(Map properties)
    {
        if (properties != null)
        {
            getScopedProperties(PropertyScope.INBOUND).putAll(properties);
            keySet.addAll(properties.keySet());
        }
    }

    void registerSessionProperties(Map properties)
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
        if (value == null && fallbackToRegistry)
        {
            value = applicationProperties.get(key);
        }
        return value;
    }

    public Object getProperty(String key, PropertyScope scope)
    {
        if (PropertyScope.APPLICATION.equals(scope))
        {
            return applicationProperties.get(key);
        }

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
        if(value!=null)
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
     * @see org.mule.umo.provider.PropertyScope
     */
    public void setProperty(String key, Object value, PropertyScope scope)
    {
        checkScopeForWriteAccess(scope);
        getScopedProperties(scope).put(key, value);
        keySet.add(key);
    }

    /**
     * @return all property keys on this message
     */
    public Set getPropertyNames()
    {
        return Collections.unmodifiableSet(keySet);
    }

    /**
     * @return all property keys on this message for the given scope
     */
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
        if(value==null)
        {
            value = defaultValue;
        }
        return value;
    }

    public int getIntProperty(String name, int defaultValue)
    {
        return MapUtils.getIntValue(getScopedProperties(defaultScope), name, defaultValue);
    }

    public long getLongProperty(String name, long defaultValue)
    {
        return MapUtils.getLongValue(getScopedProperties(defaultScope), name, defaultValue);
    }

    public double getDoubleProperty(String name, double defaultValue)
    {
        return MapUtils.getDoubleValue(getScopedProperties(defaultScope), name, defaultValue);
    }

    public boolean getBooleanProperty(String name, boolean defaultValue)
    {
        return MapUtils.getBooleanValue(getScopedProperties(defaultScope), name, defaultValue);
    }

    public String getStringProperty(String name, String defaultValue)
    {
        Object value = getProperty(name);
        if(value==null)
        {
            return defaultValue;
        }
        return value.toString();
    }

    public String toString()
    {
        StringBuffer buf = new StringBuffer(128);
        buf.append("Properites{");
        for (Iterator iterator = scopedMap.entrySet().iterator(); iterator.hasNext();)
        {
            Map.Entry entry = (Map.Entry) iterator.next();
            buf.append(entry.getKey()).append(":");
            buf.append(MapUtils.toString((Map)entry.getValue(), false));
            buf.append(", ");
        }
        buf.append("}");
        return buf.toString();
    }
}
