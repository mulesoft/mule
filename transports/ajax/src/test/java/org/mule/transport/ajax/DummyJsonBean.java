/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
