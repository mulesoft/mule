/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.registry;

import org.mule.api.lifecycle.LifecycleState;
import org.mule.api.lifecycle.LifecycleStateAware;
import org.mule.api.registry.InjectProcessor;

/**
 * Injects the MuleContext object for objects stored in the {@link org.mule.registry.TransientRegistry} where the object registered
 * implements {@link org.mule.api.context.MuleContextAware}.
 */
public class LifecycleStateInjectorProcessor implements InjectProcessor
{
    private LifecycleState state;

    public LifecycleStateInjectorProcessor(LifecycleState state)
    {
        this.state = state;
    }

    public Object process(Object object)
    {
        if (object instanceof LifecycleStateAware)
        {
            ((LifecycleStateAware)object).setLifecycleState(state);
        }
        return object;
    }
}
