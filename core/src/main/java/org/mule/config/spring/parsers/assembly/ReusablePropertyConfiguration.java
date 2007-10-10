/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.assembly;

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

    public String translateValue(String name, String value)
    {
        return delegate.translateValue(name, value);
    }

}
