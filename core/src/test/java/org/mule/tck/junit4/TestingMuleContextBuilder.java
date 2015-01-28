/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4;

import org.mule.api.MuleContext;
import org.mule.context.DefaultMuleContextBuilder;
import org.mule.registry.DefaultRegistryBroker;
import org.mule.registry.TransientRegistry;

public class TestingMuleContextBuilder extends DefaultMuleContextBuilder
{

    @Override
    protected DefaultRegistryBroker createRegistryBroker(MuleContext muleContext)
    {
        DefaultRegistryBroker broker = super.createRegistryBroker(muleContext);
        broker.addRegistry(new TransientRegistry(muleContext));

        return broker;
    }
}
