/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.registry.metadata;

import java.util.HashMap;
import java.util.Map;

public class ObjectMetadata extends Metadata
{
    protected String className = null;
    protected Map properties = null;

    public ObjectMetadata()
    {
        properties = new HashMap();
    }

    public ObjectMetadata(String className, Map properties)
    {
        this.className = className;
        this.properties = properties;
    }

    public ObjectMetadata(Class clazz, int flags, String[] propertyNames)
    {
        this.properties = new HashMap();
        this.className = clazz.getName();
        this.flags = flags;

        for (int i = 0; i < propertyNames.length; i++)
        {
            setProperty(new PropertyMetadata(propertyNames[i], 2));
        }
    }

    public void setProperty(PropertyMetadata property)
    {
        this.properties.put(property.getPropertyName(), property);
    }

    public PropertyMetadata getProperty(String propertyName)
    {
        return (PropertyMetadata)properties.get(propertyName);
    }

    public Map getProperties()
    {
        return properties;
    }

    public void setClassName(String className)
    {
        this.className = className;
    }

    public String getClassName()
    {
        return className;
    }

}

