/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension;

import org.mule.extension.annotations.Parameter;

public class Door
{
    @Parameter
    private String victim;

    @Parameter
    private String address;

    @Parameter
    private Door previous;

    public String getVictim()
    {
        return victim;
    }

    public String getAddress()
    {
        return address;
    }

    public Door getPrevious()
    {
        return previous;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof Door)
        {
            return victim.equals(((Door) obj).victim);
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        return victim.hashCode();
    }
}
