/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.registry.impl;

import org.mule.registry.Registration;
import org.mule.registry.ComponentVersion;
import org.mule.util.StringUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The MuleRegistration provides a basic implementation
 * of the Registration that defines a component reference
 * as a set of properties. It should be overridden.
 *
 */
public class MuleRegistration implements Registration
{
    public static int COMPONENT_STATE_UNITIALISED = 0;

    protected String id = null;
    protected ComponentVersion version;
    protected String parentId = null;
    protected String type = null;
    protected Object component = null;
    protected int state;
    protected HashMap properties = new HashMap();
    protected HashMap children = new HashMap();

    public MuleRegistration()
    {
    }

    /*
    public MuleRegistration(String parentId, String type, Object component)
    {
        this.parentId = parentId;
        this.type = type;
        this.state = COMPONENT_STATE_UNITIALISED;
        this.children = new HashMap();
        loadProperties(component);
    }

    public void setComponent(Object component) 
    {
        loadProperties(component);
    }

    private void loadProperties(Object component)
    {
        properties = new HashMap();
        properties.put("sourceObjectClassName", component.getClass().getName());

        try {
            Method[] methods = component.getClass().getMethods();
            for (int i = 0; i < methods.length; i++) {
                Method method = methods[i];

                // We only want getters
                if (!method.getName().startsWith("get")) continue;
                // We only can handle no argument getters
                if (method.getParameterTypes().length > 0) continue;
                // We don't want the registry ID (hasn't been set yet)
                if (method.getName().equals("getRegistryId")) continue;

                //System.out.println(type + ": " + method.getName() + " returns " + method.getReturnType().getName());

                String retType = method.getReturnType().getName();
                String name = method.getName().substring(3, 4).toLowerCase() +
                    method.getName().substring(4);

                if (doCapture(retType))
                {
                    Object value = method.invoke(component, null);
                    properties.put(name, value);
                }
                else if (retType.equals("java.util.Map"))
                {
                    Map map = (Map)method.invoke(component, null);
                    Iterator iter = map.keySet().iterator();
                    while (iter.hasNext())
                    {
                        Object key = iter.next();
                        Object val = map.get(key);
                        if (doCapture(val.getClass().getName()))
                            properties.put(key.toString(), val);
                    }
                }
            }
        } catch (Exception e) {
        }
    }
    */

    public String getType()
    {
        return type;
    }

    public String getId()
    {
        return id;
    }

    public String getParentId()
    {
        return parentId;
    }

    public HashMap getProperties()
    {
        return properties;
    }

    public Object getProperty(String key)
    {
        return properties.get(key);
    }

    public int getState()
    {
        return state;
    }

    /*
     * Non-standard getter so XStream doesn't grab it
     */
    public HashMap retrieveChildren()
    {
        return children;
    }

    /*
     * Non-standard getter so XStream doesn't grab it
     */
    public Registration retrieveChild(String childId)
    {
        return (Registration)children.get(childId);
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public void setParentId(String parentId)
    {
        this.parentId = parentId;
    }

    public void setProperties(HashMap properties)
    {
        this.properties.putAll(properties);
    }

    public void setProperty(String key, Object property)
    {
        this.properties.put(key, property);
    }

    public void addChild(Registration component)
    {
        children.put(component.getId(), component);
    }

    public ComponentVersion getVersion() {
        return version;
    }

    public void setVersion(ComponentVersion version) {
    	this.version = version;
    }

    /*
    private boolean doCapture(String retType)
    {
        for (int i = 0; i < GETTERS_TO_GET.length; i++)
            if (StringUtils.equals(GETTERS_TO_GET[i], retType)) return true;
        return false;
    }

    public void register()
    {
    }

    public void deploy()
    {
    }

    public void undeploy()
    {
    }

    public void unregister()
    {
    }
    */

}
