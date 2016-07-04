/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.model;

public class Region
{
    public static final Region SOUTHWEST = new Region("Southwest", new String[] {"94105", "90049", "92027"});
    public static final Region NORTHWEST = new Region("Northwest", new String[] {"93101", "97201", "99210"});

    private String name;
    private String[] zips;

    public Region(String name, String[] zips)
    {
        this.name = name;
        this.zips = zips;
    }

    public String getName()
    {
        return name;
    }

    public String[] getZips()
    {
        return zips;
    }
}
