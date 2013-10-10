/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
