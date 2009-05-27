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

import org.mule.api.MuleRuntimeException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.api.registry.Registry;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.UUID;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public abstract class AbstractRegistry implements Registry
{
    /** the unique id for this Registry */
    private String id;

    protected transient Log logger = LogFactory.getLog(getClass());

    /** Default Constructor */
    protected AbstractRegistry(String id)
    {
        if (id == null)
        {
            throw new MuleRuntimeException(CoreMessages.objectIsNull("RegistryID"));
        }
        this.id = id;
    }

    public final synchronized void dispose()
    {
        try
        {
            doDispose();
        }
        catch (Exception e)
        {
            logger.error("Failed to cleanly dispose: " + e.getMessage(), e);
        }
    }

    abstract protected void doInitialise() throws InitialisationException;

    abstract protected void doDispose();

    public final void initialise() throws InitialisationException
    {
        if (id == null)
        {
            logger.warn("No unique id has been set on this registry");
            id = UUID.getUUID();
        }
        try
        {
            doInitialise();
        }
        catch (InitialisationException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
    }

    public <T> T lookupObject(Class<T> type) throws RegistrationException
    {
        // Accumulate objects from all registries.
        Collection<T> objects = lookupObjects(type);
        
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
