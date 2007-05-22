/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extras.spring.config;

/**
 * TODO
 */
public class LegacyManagerPlaceholder
{
     private String managerId;

    public LegacyManagerPlaceholder(String managerId)
    {
        this.managerId = managerId;
    }

    public String getManagerId()
    {
        return managerId;
    }
}
