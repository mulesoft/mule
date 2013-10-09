/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
