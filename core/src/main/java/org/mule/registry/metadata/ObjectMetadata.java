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
    protected boolean persistable = true;
    protected String className = null;
    protected Map properties = null;

    public ObjectMetadata()
    {
        properties = new HashMap();
    }

    // This will probably go
    public ObjectMetadata(Class clazz)
    {
        this(clazz, true, new String[] {});
    }

    public ObjectMetadata(String[] propertyNames)
    {
        this(true, propertyNames);
    }

    public ObjectMetadata(boolean isPersistable, String[] propertyNames)
    {
        this(null, true, propertyNames);
    }

    public ObjectMetadata(Class clazz, boolean persistable, String[] propertyNames)
    {
        this.properties = new HashMap();

        if (clazz != null)
        {
            this.className = clazz.getName();
        }

        setPersistable(persistable);

        for (int i = 0; i < propertyNames.length; i++)
        {
            setProperty(new PropertyMetadata(propertyNames[i], PropertyMetadata.PROPERTY_PERSISTABLE));
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

    public void setPersistable(boolean persistable)
    {
        this.persistable = persistable;
    }

    public boolean getPersistable()
    {
	return persistable;
    }


}

