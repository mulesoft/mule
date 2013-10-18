/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.registry;

import org.mule.util.ClassUtils;
import org.mule.util.StringUtils;

import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class AbstractServiceDescriptor implements ServiceDescriptor
{

    /**
     * logger used by this class
     */
    protected final Log logger = LogFactory.getLog(getClass());

    protected String service;

    public AbstractServiceDescriptor(String service)
    {
        this.service = service;
    }

    public String getService()
    {
        return service;
    }

    protected String removeProperty(String name, Properties properties)
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

    protected Class<?> removeClassProperty(String name, Properties properties) throws ClassNotFoundException
    {
        String clazz = removeProperty(name, properties);
        if (clazz == null)
        {
            return null;
        }
        else
        {
            return ClassUtils.loadClass(clazz, getClass());
        }
    }



    /**
     * Unique key used to cache the service descriptors.  This uses the service and the
     * overrides, but since it is generated externally by the factory that instantiates
     * the service descriptor we do not need to keep overrides or properties anywhere else.
     */
    public static class Key
    {
        
        private final Map<?, ?> overrides;
        private final String service;

        public Key(String service, Map<?, ?> overrides)
        {
            this.overrides = overrides;
            this.service = service;
        }

        @Override
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

        @Override
        public int hashCode()
        {
            return 29 * (overrides != null ? overrides.hashCode() : 0) + (service != null ? service.hashCode(): 0);
        }

        public String getKey()
        {
            return service + ":" + Integer.toString(hashCode()); 
        }

    }

}


