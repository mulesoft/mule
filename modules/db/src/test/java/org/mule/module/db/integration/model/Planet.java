/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.model;

public class Planet
{

    public static final Planet VENUS = new Planet("Venus", 2);
    public static final Planet EARTH = new Planet("Earth", 3);
    public static final Planet MARS = new Planet("Mars", 4);

    private String name;
    private int position;

    public Planet(String name, int position)
    {
        this.name = name;
        this.position = position;
    }

    public String getName()
    {
        return name;
    }

    public int getPosition()
    {
        return position;
    }
}
