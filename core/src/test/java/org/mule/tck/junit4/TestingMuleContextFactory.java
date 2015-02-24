/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextFactory;
import org.mule.context.DefaultMuleContextBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.registry.SimpleRegistry;

/**
 * An implementation of {@link MuleContextFactory} to use for testing.
 * It will create a {@link MuleContext} which uses a {@link SimpleRegistry}
 *
 * @since 3.7.0
 */
public class TestingMuleContextFactory extends DefaultMuleContextFactory
{

    @Override
    protected DefaultMuleContextBuilder newMuleContextBuilder()
    {
        return new DefaultMuleContextBuilder(true);
    }
}
