/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.config;

import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;

/**
 * TODO
 */
public class TestFilter implements Filter
{
    private String foo;
    private int bar;

    public int getBar()
    {
        return bar;
    }

    public void setBar(int bar)
    {
        this.bar = bar;
    }

    public String getFoo()
    {
        return foo;
    }

    public void setFoo(String foo)
    {
        this.foo = foo;
    }

    public boolean accept(MuleMessage message)
    {
        return true;
    }
}
