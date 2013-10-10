/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
