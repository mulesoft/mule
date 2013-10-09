/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.assembly.configuration;

public class SinglePropertyWrapper implements SingleProperty
{

    private String oldName;
    private PropertyConfiguration config;

    public SinglePropertyWrapper(String oldName, PropertyConfiguration config)
    {
        this.oldName = oldName;
        this.config = config;
    }

    public boolean isCollection()
    {
        return config.isCollection(oldName);
    }

    public void setCollection()
    {
        config.addCollection(oldName);
    }

    public boolean isIgnored()
    {
        return config.isIgnored(oldName);
    }

    public void setIgnored()
    {
        config.addIgnored(oldName);
    }

    public boolean isReference()
    {
        return config.isReference(oldName);
    }

    public void setReference()
    {
        config.addReference(oldName);
    }
    
}
