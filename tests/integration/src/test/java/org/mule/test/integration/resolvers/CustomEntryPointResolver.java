/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.resolvers;

import org.mule.api.MuleEventContext;
import org.mule.api.model.EntryPointResolver;
import org.mule.api.model.InvocationResult;

public class CustomEntryPointResolver implements EntryPointResolver
{

    public InvocationResult invoke(Object component, MuleEventContext context) throws Exception
    {
        return new InvocationResult(this,
                ((Target) component).custom(context.getMessage().getPayload()),
                Target.class.getMethod("custom", new Class[]{Object.class}));
    }

}
