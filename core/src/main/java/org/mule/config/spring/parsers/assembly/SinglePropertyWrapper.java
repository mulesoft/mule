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
