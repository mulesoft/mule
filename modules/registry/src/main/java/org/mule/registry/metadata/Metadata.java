/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.registry.metadata;

public class Metadata
{
    protected String mappedName = null;
    protected int flags = 0;
    protected int datatype = 0;

    public Metadata()
    {
    }

    public void setMappedName(String mappedName)
    {
        this.mappedName = mappedName;
    }

    public String getMappedName()
    {
        return mappedName;
    }

}
