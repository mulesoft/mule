/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.assembly.configuration;

import java.util.Map;

/**
 * Wrap a PropertyConfiguration so that changes are kept in the wrapper
 */
public class TempWrapperPropertyConfiguration implements PropertyConfiguration
{

    protected PropertyConfiguration delegate;
    protected SimplePropertyConfiguration extra = new SimplePropertyConfiguration();
    private boolean greedyIgnore;

    public TempWrapperPropertyConfiguration(PropertyConfiguration delegate)
    {
        this(delegate, true);
    }

    public TempWrapperPropertyConfiguration(PropertyConfiguration delegate, boolean greedyIgnore)
    {
        this.delegate = delegate;
        this.greedyIgnore = greedyIgnore;
    }

    public void addReference(String propertyName)
    {
        extra.addReference(propertyName);
    }

    public void addMapping(String propertyName, Map mappings)
    {
        extra.addMapping(propertyName, mappings);
    }

    public void addMapping(String propertyName, String mappings)
    {
        extra.addMapping(propertyName, mappings);
    }

    public void addMapping(String propertyName, ValueMap mappings)
    {
        extra.addMapping(propertyName, mappings);
    }

    public void addAlias(String alias, String propertyName)
    {
        extra.addAlias(alias, propertyName);
    }

    public void addCollection(String propertyName)
    {
        extra.addCollection(propertyName);
    }

    public void addIgnored(String propertyName)
    {
        extra.addIgnored(propertyName);
    }

    public void removeIgnored(String propertyName)
    {
        extra.removeIgnored(propertyName);
    }

    public void setIgnoredDefault(boolean ignoreAll)
    {
        extra.setIgnoredDefault(ignoreAll);
    }

    public String getAttributeMapping(String alias)
    {
        return extra.getAttributeMapping(alias, delegate.getAttributeMapping(alias));
    }

    public String getAttributeAlias(String mapping)
    {
        return extra.getAttributeMapping(mapping, delegate.getAttributeAlias(mapping));
    }

    public boolean isCollection(String propertyName)
    {
        return extra.isCollection(propertyName) || delegate.isCollection(propertyName);
    }

    public boolean isIgnored(String propertyName)
    {
        if (greedyIgnore)
        {
            return extra.isIgnored(propertyName) || delegate.isIgnored(propertyName);
        }
        else
        {
            return extra.isIgnored(propertyName) && delegate.isIgnored(propertyName);            
        }
    }

    public boolean isReference(String attributeName)
    {
        return extra.isReference(attributeName) || delegate.isReference(attributeName);
    }

    public SingleProperty getSingleProperty(String propertyName)
    {
        return new SinglePropertyWrapper(propertyName, this);
    }

    public String translateName(String oldName)
    {
        return extra.translateName(delegate.translateName(oldName));
    }

    public Object translateValue(String name, String value)
    {
        Object intermediate = delegate.translateValue(name, value);
        if (intermediate != null && intermediate.equals(value))
        {
            return extra.translateValue(name, value);
        }
        else
        {
            return intermediate;
        }
    }

}
