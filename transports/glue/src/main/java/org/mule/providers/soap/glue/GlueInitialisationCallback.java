/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.glue;

import org.mule.config.i18n.Message;
import org.mule.impl.InitialisationCallback;
import org.mule.umo.lifecycle.InitialisationException;

import electric.glue.context.ServiceContext;
import electric.registry.Registry;
import electric.registry.RegistryException;
import electric.service.IService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>GlueInitialisationCallback</code> is invoked when an Glue service
 * component is created from its descriptor.
 */
public class GlueInitialisationCallback implements InitialisationCallback
{
    protected static final Log logger = LogFactory.getLog(GlueInitialisationCallback.class);

    private final IService service;
    private final ServiceContext context;
    private final String servicePath;
    private volatile boolean invoked = false;

    public GlueInitialisationCallback(IService service, String path, ServiceContext context)
    {
        this.service = service;
        this.servicePath = path;

        if (context != null)
        {
            this.context = context;
        }
        else
        {
            this.context = new ServiceContext();
        }
    }

    public void initialise(Object component) throws InitialisationException
    {
        // only call this once
        if (invoked)
        {
            return;
        }
        if (component instanceof GlueInitialisable)
        {
            logger.debug("Calling Glue initialisation for component: " + component.getClass().getName());
            ((GlueInitialisable)component).initialise(service, context);
        }
        invoked = true;
        try
        {
            logger.debug("Publishing service " + servicePath + " to Glue registry.");
            Registry.publish(servicePath, service, context);
        }
        catch (RegistryException e)
        {
            throw new InitialisationException(new Message("soap", 3, component.getClass().getName()), e, this);
        }
    }
}
