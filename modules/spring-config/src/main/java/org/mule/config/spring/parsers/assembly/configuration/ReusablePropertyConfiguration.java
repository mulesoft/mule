/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.assembly.configuration;

import java.util.Map;

/**
 * Allow initial mutation; first call of reset stores values as reference and allows
 * further temporary mutation; further calls to reset return to reference.
 */
public class ReusablePropertyConfiguration implements PropertyConfiguration
{
    private PropertyConfiguration reference;
    private PropertyConfiguration delegate;

    public ReusablePropertyConfiguration()
    {
        this(new SimplePropertyConfiguration());
    }

    public ReusablePropertyConfiguration(PropertyConfiguration delegate)
    {
        this.delegate = delegate;
    }

    public void reset()
    {
        if (null == reference)
        {
            reference = delegate;
        }
        delegate = new TempWrapperPropertyConfiguration(reference);
    }

    public void addReference(String propertyName)
    {
        delegate.addReference(propertyName);
    }

    public void addMapping(String propertyName, Map mappings)
    {
        delegate.addMapping(propertyName, mappings);
    }

    public void addMapping(String propertyName, String mappings)
    {
        delegate.addMapping(propertyName, mappings);
    }

    public void addMapping(String propertyName, ValueMap mappings)
    {
        delegate.addMapping(propertyName, mappings);
    }

    public void addAlias(String alias, String propertyName)
    {
        delegate.addAlias(alias, propertyName);
    }

    public void addCollection(String propertyName)
    {
        delegate.addCollection(propertyName);
    }

    public void addIgnored(String propertyName)
    {
        delegate.addIgnored(propertyName);
    }

    public void removeIgnored(String propertyName)
    {
        delegate.removeIgnored(propertyName);
    }

    public void setIgnoredDefault(boolean ignoreAll)
    {
        delegate.setIgnoredDefault(ignoreAll);
    }

    public String getAttributeMapping(String alias)
    {
        return delegate.getAttributeMapping(alias);
    }

    public String getAttributeAlias(String mapping)
    {
        return delegate.getAttributeAlias(mapping);
    }

    public boolean isCollection(String propertyName)
    {
        return delegate.isCollection(propertyName);
    }

    public boolean isIgnored(String propertyName)
    {
        return delegate.isIgnored(propertyName);
    }

    public boolean isReference(String attributeName)
    {
        return delegate.isReference(attributeName);
    }

    public SingleProperty getSingleProperty(String propertyName)
    {
        return new SinglePropertyWrapper(propertyName, this);
    }

    public String translateName(String oldName)
    {
        return delegate.translateName(oldName);
    }

    public Object translateValue(String name, String value)
    {
        return delegate.translateValue(name, value);
    }

}
