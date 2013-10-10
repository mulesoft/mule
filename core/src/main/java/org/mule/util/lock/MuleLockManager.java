/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.lock;

import org.mule.api.MuleContext;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;

public class MuleLockManager implements LockManager, MuleContextAware, Initialisable, Disposable
{
    private LockGroup lockGroup;
    private MuleContext muleContext;

    public synchronized Lock getLock(String lockId)
    {
        return new LockAdapter(lockId,lockGroup);
    }
    
    @Override
    public void dispose()
    {
        lockGroup.dispose();
    }

    @Override
    public void initialise() throws InitialisationException
    {
        LockProvider lockProvider = muleContext.getRegistry().get(MuleProperties.OBJECT_LOCK_PROVIDER);
        lockGroup = new InstanceLockGroup(lockProvider);

    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }
}
