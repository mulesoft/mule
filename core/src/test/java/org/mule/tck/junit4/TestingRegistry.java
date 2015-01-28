/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4;

import org.mule.api.MuleContext;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.lifecycle.phases.NotInLifecyclePhase;
import org.mule.registry.TransientRegistry;

public class TestingRegistry extends TransientRegistry
{

    public TestingRegistry(MuleContext muleContext)
    {
        super(muleContext);
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        initialiseObjects();

        super.doInitialise();
    }

    private void injectDependencies() throws InitialisationException {
        for (Object object : lookupObjects(Object.class)) {

        }
    }

    private void initialiseObjects() throws InitialisationException
    {
        try
        {
            for (Initialisable initialisable : lookupObjects(Initialisable.class))
            {
                getLifecycleManager().applyPhase(initialisable, NotInLifecyclePhase.PHASE_NAME, Initialisable.PHASE_NAME);
            }
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
    }
}
