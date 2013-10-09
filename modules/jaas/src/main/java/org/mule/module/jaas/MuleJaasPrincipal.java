/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
