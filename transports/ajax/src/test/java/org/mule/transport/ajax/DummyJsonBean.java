/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.ajax;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * TODO
 */
public class DummyJsonBean
{
    @JsonProperty
    private String name;

    public DummyJsonBean()
    {
    }

    public DummyJsonBean(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public String toString()
    {
        return "DummyJsonBean{" +
                "name='" + name + '\'' +
                '}';
    }
}
