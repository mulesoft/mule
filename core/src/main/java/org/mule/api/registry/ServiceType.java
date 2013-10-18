/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.registry;

/**
 * TODO
 */
public enum ServiceType
{
    TRANSPORT("transport", "org/mule/transport"),
    MODEL("model", "org/mule/model"),
    EXCEPTION("exception", "org/mule/config");

    private String name;
    private String path;

    ServiceType(String name, String path)
    {
        this.name = name;
        this.path = path;
    }

    @Override
    public String toString()
    {
        return name + ": " + path;
    }

    public String getPath()
    {
        return path;
    }

    public String getName()
    {
        return name;
    }
}


