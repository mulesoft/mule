/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jaas;

import java.io.Serializable;
import java.security.Principal;

public class MuleJaasPrincipal implements Principal, Serializable
{
    private final String name;

    public MuleJaasPrincipal(String name)
    {
        if (name == null)
        {
            throw new IllegalArgumentException("Null name");
        }
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public String toString()
    {
        return name;
    }

    public int hasCode()
    {
        return name.hashCode();
    }
}
