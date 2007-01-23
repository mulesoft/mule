/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.registry.metadata;

public class PropertyMetadata extends Metadata
{
    private String propertyName = null;

    public PropertyMetadata()
    {
        super();
    }

    public PropertyMetadata(String propertyName, int flags)
    {
        this.propertyName = propertyName;
        this.flags = flags;
    }

    public void setPropertyName(String propertyName)
    {
        this.propertyName = propertyName;
    }

    public String getPropertyName()
    {
        return propertyName;
    }

}
