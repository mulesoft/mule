/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
