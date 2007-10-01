/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.jaas;

import java.security.Principal;

public class MuleJaasPrincipal implements Principal
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
