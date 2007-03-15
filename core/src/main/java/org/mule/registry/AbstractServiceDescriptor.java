/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the BSD style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.registry;

import org.mule.util.ClassUtils;

import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class AbstractServiceDescriptor implements ServiceDescriptor
{
    /**
     * logger used by this class
     */
    protected final Log logger = LogFactory.getLog(getClass());

    protected Properties properties;
    protected String service;

    public AbstractServiceDescriptor(String service, Properties props)
    {
        this.service = service;
        this.properties = props;
    }

    public abstract void setOverrides(Properties props);

    public String getService()
    {
        return service;
    }

    protected String removeProperty(String name)
    {
        String temp = (String)properties.remove(name);
        if (StringUtils.isEmpty(StringUtils.trim(temp)))
        {
            return null;
        }
        else
        {
            return temp;
        }
    }

    protected Class removeClassProperty(String name) throws ClassNotFoundException
    {
        String clazz = removeProperty(name);
        if(clazz==null) return null;
        
        return ClassUtils.loadClass(clazz, getClass());
    }

    

    /**
     * Unique key used to cache the service descriptors.
     */
    public static class Key
    {
        private final Map overrides;
        private final String service;

        public Key(String service, Map overrides)
        {
            this.overrides = overrides;
            this.service = service;
        }

        // //@Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof Key))
            {
                return false;
            }

            final Key key = (Key)o;

            if (overrides != null ? !overrides.equals(key.overrides) : key.overrides != null)
            {
                return false;
            }
            if (!service.equals(key.service))
            {
                return false;
            }

            return true;
        }

        // //@Override
        public int hashCode()
        {
            return 29 * (overrides != null ? overrides.hashCode() : 0) + service.hashCode();
        }
    }
}


