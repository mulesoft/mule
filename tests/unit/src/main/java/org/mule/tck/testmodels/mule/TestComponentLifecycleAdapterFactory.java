/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.mule;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.component.JavaComponent;
import org.mule.api.component.LifecycleAdapter;
import org.mule.api.component.LifecycleAdapterFactory;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.model.EntryPointResolverSet;

public class TestComponentLifecycleAdapterFactory implements LifecycleAdapterFactory
{
    public TestComponentLifecycleAdapterFactory()
    {
        super();
    }

    public LifecycleAdapter create(Object pojoService,
                                   JavaComponent service,
                                   FlowConstruct flowConstruct,
                                   EntryPointResolverSet resolver,
                                   MuleContext muleContext) throws MuleException
    {
        return new TestComponentLifecycleAdapter(pojoService, service, flowConstruct, resolver, muleContext);
    }

}
