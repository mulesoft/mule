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
import org.mule.api.construct.FlowConstruct;
import org.mule.api.model.EntryPointResolverSet;
import org.mule.component.DefaultComponentLifecycleAdapter;

/** <code>TestComponentLifecycleAdapter</code> TODO document */
public class TestComponentLifecycleAdapter extends DefaultComponentLifecycleAdapter
{
    public TestComponentLifecycleAdapter(Object pojoService,
                                         JavaComponent service,
                                         FlowConstruct flowConstruct,
                                         MuleContext muleContext) throws MuleException
    {
        super(pojoService, service, flowConstruct, muleContext);
    }

    public TestComponentLifecycleAdapter(Object pojoService,
                                       JavaComponent service,
                                       FlowConstruct flowConstruct,
                                       EntryPointResolverSet epResolver,
                                       MuleContext muleContext) throws MuleException
    {
        super(pojoService, service, flowConstruct, epResolver, muleContext);
    }

}
