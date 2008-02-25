/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.registry;

import org.mule.MuleServer;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleManager;
import org.mule.api.lifecycle.LifecycleTransitionResult;
import org.mule.api.registry.RegistrationException;
import org.mule.api.registry.Registry;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.UUID;
import org.mule.util.properties.PropertyExtractorManager;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public abstract class AbstractRegistry implements Registry
{
    /** the unique id for this Registry */
    private String id;

    protected transient Log logger = LogFactory.getLog(getClass());

    protected LifecycleManager lifecycleManager;

    /** Default Constructor */
    protected AbstractRegistry(String id)
    {
        if (id == null)
        {
            throw new NullPointerException(CoreMessages.objectIsNull("RegistryID").getMessage());
        }
        this.id = id;
        lifecycleManager = createLifecycleManager();
    }

    protected abstract LifecycleManager createLifecycleManager();

    protected LifecycleManager getLifecycleManager()
    {
        return lifecycleManager;
    }

    public final synchronized void dispose()
    {
        // TODO lifecycleManager.checkPhase(Disposable.PHASE_NAME);

        if (isDisposed())
        {
            return;
        }

        try
        {
            doDispose();
            lifecycleManager.firePhase(MuleServer.getMuleContext(), Disposable.PHASE_NAME);
            PropertyExtractorManager.clear();
        }
        catch (MuleException e)
        {
            // TODO
            logger.error("Failed to cleanly dispose: " + e.getMessage(), e);
        }
    }

    abstract protected void doInitialise() throws InitialisationException;

    abstract protected void doDispose();

    public boolean isDisposed()
    {
        return lifecycleManager.isPhaseComplete(Disposable.PHASE_NAME);
    }

    public boolean isDisposing()
    {
        return Disposable.PHASE_NAME.equals(lifecycleManager.getExecutingPhase());
    }

    public boolean isInitialised()
    {
        return lifecycleManager.isPhaseComplete(Initialisable.PHASE_NAME);
    }

    public boolean isInitialising()
    {
        return Initialisable.PHASE_NAME.equals(lifecycleManager.getExecutingPhase());
    }

    public final LifecycleTransitionResult initialise() throws InitialisationException
    {
        lifecycleManager.checkPhase(Initialisable.PHASE_NAME);

//        if (getParent() != null)
//        {
//            parent.initialise();
//        }

        // I don't think it makes sense for the Registry to know about the MuleContext at this point.
        // MuleContext mc = MuleServer.getMuleContext();
        // if (mc != null)
        // {
        // mc.fireNotification(new RegistryNotification(this, RegistryNotification.REGISTRY_INITIALISING));
        // }

        if (id == null)
        {
            logger.warn("No unique id has been set on this registry");
            id = UUID.getUUID();
        }
        try
        {
            doInitialise();
            lifecycleManager.firePhase(MuleServer.getMuleContext(), Initialisable.PHASE_NAME);
        }
        catch (InitialisationException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
        return LifecycleTransitionResult.OK;
    }

    public Object lookupObject(Class type) throws RegistrationException
    {
        // Accumulate objects from all registries.
        Collection objects = lookupObjects(type);
        
        if (objects.size() == 1)
        {
            return objects.iterator().next();
        }
        else if (objects.size() > 1)
        {
            throw new RegistrationException("More than one object of type " + type + " registered but only one expected.");
        }
        else
        {
            return null;
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    // Registry Metadata
    // /////////////////////////////////////////////////////////////////////////

    public final String getRegistryId()
    {
        return id;
    }
}
