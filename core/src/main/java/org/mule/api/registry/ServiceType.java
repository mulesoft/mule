/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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


