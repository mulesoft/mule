/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextFactory;
import org.mule.context.DefaultMuleContextFactory;

/**
 * Static util methods for use in benchmark setup/teardown.  Benchmark methods themselves should ideally be self-contained for
 * clarity.
 */
public class BenchmarkUtils
{

    public static MuleContext createMuleContext() throws MuleException
    {
        MuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
        return muleContextFactory.createMuleContext();
    }

}
