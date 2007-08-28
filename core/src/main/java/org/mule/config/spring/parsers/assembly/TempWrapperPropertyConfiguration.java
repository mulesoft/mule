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
 * Wrap a PropertyConfiguration so that changes are kept in the wrapper
 */
public class TempWrapperPropertyConfiguration implements PropertyConfiguration
{

    protected PropertyConfiguration delegate;
    protected SimplePropertyConfiguration extra = new SimplePropertyConfiguration();

    public TempWrapperPropertyConfiguration(PropertyConfiguration delegate)
    {
        this.delegate = delegate;
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

    public String getAttributeMapping(String alias)
    {
        return extra.getAttributeMapping(alias, delegate.getAttributeMapping(alias));
    }

    public boolean isCollection(String propertyName)
    {
        return extra.isCollection(propertyName) || delegate.isCollection(propertyName);
    }

    public boolean isIgnored(String propertyName)
    {
        return extra.isIgnored(propertyName) || delegate.isIgnored(propertyName);
    }

    public boolean isBeanReference(String attributeName)
    {
        return extra.isBeanReference(attributeName) || delegate.isBeanReference(attributeName);
    }

    public String translateName(String oldName)
    {
        return extra.translateName(delegate.translateName(oldName));
    }

    public String translateValue(String name, String value)
    {
        return extra.translateValue(name, delegate.translateValue(name, value));
    }

}
