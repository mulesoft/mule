/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.processors;

import org.mule.api.MuleContext;
import org.mule.api.config.PropertyFactory;
import org.mule.api.context.MuleContextAware;
import org.mule.config.i18n.CoreMessages;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

/**
 * TODO
 */
public class PropertyPlaceholderProcessor extends PropertyPlaceholderConfigurer implements MuleContextAware
{
    private MuleContext muleContext;
    private Map factories = new HashMap();

    public PropertyPlaceholderProcessor()
    {
        setValueSeparator(null);
    }

    @Override
    protected Properties mergeProperties() throws IOException
    {
        RegistryProperties props = new RegistryProperties();
        props.putAll(super.mergeProperties());

        // MuleContext/MuleConfiguration properties
        props.put("mule.working.dir", muleContext.getConfiguration().getWorkingDirectory());
        
        if (factories != null)
        {
            for (Iterator iterator = factories.entrySet().iterator(); iterator.hasNext();)
            {
                Map.Entry entry = (Map.Entry) iterator.next();
                if (entry.getKey() == null)
                {
                    throw new NullPointerException(CoreMessages.objectIsNull("Factories.Key").getMessage());
                }
                if (entry.getValue() == null)
                {
                    throw new NullPointerException(CoreMessages.objectIsNull("Factories.Value").getMessage());
                }
                try
                {
                    props.put(entry.getKey(), ((PropertyFactory) entry.getValue()).create(props));
                }
                catch (Exception e)
                {
                    throw new IOException("Failed to invoke PropertyFactory: " + entry.getValue() + ". Error is: " + e.toString());
                }
            }
        }
        return props;
    }

    public Map getFactories()
    {
        return factories;
    }

    public void setFactories(Map factories)
    {
        this.factories = factories;
    }

    private class RegistryProperties extends Properties
    {
        public String getProperty(String key)
        {
            Object oval = super.get(key);
            if (oval == null)
            {
                oval = muleContext.getRegistry().lookupObject(key);
            }
            String sval = (oval instanceof String) ? (String)oval : null;
            return ((sval == null) && (defaults != null)) ? defaults.getProperty(key) : sval;
        }
    }

    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
        
    }

}
