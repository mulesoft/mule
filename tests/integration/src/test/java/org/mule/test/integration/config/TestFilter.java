/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.config;

import org.mule.umo.UMOFilter;
import org.mule.umo.UMOMessage;

/**
 * TODO
 */
public class TestFilter implements UMOFilter
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

    public boolean accept(UMOMessage message)
    {
        return true;
    }
}
