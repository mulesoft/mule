/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.lifecycle.phases;

import org.mule.MuleServer;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.lifecycle.DefaultLifecyclePhase;

import java.util.Collection;

/**
 * Objects are disposed of via the Registry since the Registry manages the creation/initialisation of the objects
 * it must also take care of disposing them. However, a user may want to initiate a dispose via the
 * {@link org.mule.DefaultMuleContext} so the dispose Lifecycle phase for the {@link org.mule.DefaultMuleContext}
 * needs to call dispose on the Registry.
 */
public class MuleContextDisposePhase extends DefaultLifecyclePhase
{
    public MuleContextDisposePhase()
    {
        super(Disposable.PHASE_NAME, Disposable.class, Initialisable.PHASE_NAME);
    }

//    public void applyLifecycle(Collection objects, String currentPhase) throws MuleException
//    {        
//        //Delegate this to the Registry
//        MuleServer.getMuleContext().getRegistry().dispose();
//    }
}
