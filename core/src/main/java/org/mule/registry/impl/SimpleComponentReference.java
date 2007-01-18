/*
 * $Id: 
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.registry.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;

import org.mule.registry.*;

/**
 * The SimpleComponentReference provides a basic implementation
 * of the ComponentReference that defines a component reference
 * as a set of properties
 */
// TODO MERGE made abstract, does not implement every method
public abstract class SimpleComponentReference implements ComponentReference
{
    public static int COMPONENT_STATE_UNITIALISED = 0;

    // TODO MERGE long or String?
    protected String id = ""; //-1L;
    // TODO MERGE long or String?
    protected String parentId = ""; //-1L;
    protected String type = null;
    protected Object component = null;
    protected int state;
    protected HashMap properties = null;
    protected HashMap children = null;

    // TODO MERGE long or String?
    public SimpleComponentReference(String parentId, String type, Object component)
    {
        this.parentId = parentId;
        this.type = type;
        this.state = COMPONENT_STATE_UNITIALISED;
        this.component = component;
        this.children = new HashMap();
        loadProperties();
    }

    private void loadProperties() 
    {
        properties = new HashMap();
        try {
            Method[] methods = component.getClass().getMethods();
            for (int i = 0; i < methods.length; i++) {
                Method method = methods[i];
                if (!method.getName().startsWith("get")) continue;
                if (method.getParameterTypes().length > 0) continue;
                if (method.getReturnType().getName().equals("java.lang.String")) {
                    String name = 
                        method.getName().substring(3, 4).toLowerCase() +
                        method.getName().substring(4);
                    String value = method.invoke(component, null).toString();
                    properties.put(name, value);
                }
            }
        } catch (Exception e) {
        }
    }

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

    public HashMap getChildren()
    {
        return children;
    }

    public ComponentReference getChild(long childId)
    {
        return (ComponentReference)children.get(new Long(childId));
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

    public void addChild(ComponentReference component) 
    {
        children.put(new Long(component.getId()), component);
    }

    /*
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
