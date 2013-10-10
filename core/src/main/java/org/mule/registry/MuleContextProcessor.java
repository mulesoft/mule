/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.registry;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.registry.InjectProcessor;

/**
 * Injects the MuleContext object for objects stored in the {@link TransientRegistry} where the object registered
 * implements {@link org.mule.api.context.MuleContextAware}.
 */
public class MuleContextProcessor implements InjectProcessor
{
    private MuleContext context;

    public MuleContextProcessor(MuleContext context)
    {
        this.context = context;
    }

    public Object process(Object object)
    {
        if (object instanceof MuleContextAware)
        {
            ((MuleContextAware)object).setMuleContext(context);
        }
        return object;
    }
}
