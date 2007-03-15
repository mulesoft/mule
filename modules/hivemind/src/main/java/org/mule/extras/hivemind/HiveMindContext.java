/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.hivemind;

import org.mule.impl.container.AbstractContainerContext;
import org.mule.impl.container.ContainerKeyPair;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.ContainerException;
import org.mule.umo.manager.ObjectNotFoundException;

import java.io.Reader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hivemind.ApplicationRuntimeException;
import org.apache.hivemind.Registry;
import org.apache.hivemind.impl.RegistryBuilder;

/**
 * <code>HiveMindContext</code> is a HiveMind Context that can expose HiveMind
 * managed services for use in the Mule framework.
 * 
 * @author <a href="mailto:massimo@datacode.it">Massimo Lusetti</a>
 * @version $Revision$
 */
public class HiveMindContext extends AbstractContainerContext
{
    private static final Log logger = LogFactory.getLog(HiveMindContext.class);

    /**
     * the hivemind registry that manages services
     */
    private Registry registry;

    public HiveMindContext()
    {
        super("hivemind");
        logger.debug("HiveMindContext built");
    }

    protected Registry getRegistry()
    {
        return this.registry;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.model.UMOContainerContext#getComponent(java.lang.Object)
     */
    public Object getComponent(Object key) throws ObjectNotFoundException
    {

        if (registry == null)
        {
            throw new IllegalStateException("HiveMind registry has not been set");
        }
        if (key == null)
        {
            throw new ObjectNotFoundException("Component not found for null key");
        }
        if (key instanceof ContainerKeyPair)
        {
            key = ((ContainerKeyPair)key).getKey();
        }
        Object component = null;

        if (key instanceof String)
        {
            try
            {
                component = registry.getService((String)key, Object.class);
                logger.debug("Called " + key + " obtained  " + component.getClass().getName());
            }
            catch (ApplicationRuntimeException are)
            {
                throw new ObjectNotFoundException("Component not found for " + key, are);
            }
        }
        else if (key instanceof Class)
        {
            try
            {
                component = registry.getService((Class)key);
                logger.debug("Called " + ((Class)key).getName() + " obtained  "
                             + component.getClass().getName());
            }
            catch (ApplicationRuntimeException are)
            {
                throw new ObjectNotFoundException("Component not found for " + key, are);
            }
        }

        if (component == null)
        {
            logger.debug("Component not found for key" + key);
            throw new ObjectNotFoundException("Component not found for key: " + key.toString());
        }
        return component;
    }

    /**
     * Just log that we don't need any configuration fragment.
     */
    public void configure(Reader configuration) throws ContainerException
    {
        logger.info("HiveMindContext doesn't need any configuration fragment. Configuration ignored");
    }

    /**
     * Here we build the registry from the standard deployment descriptors location.
     * //@Override
     */
    public void initialise() throws InitialisationException {
        if (registry == null)
        {
            logger.debug("About to initilise the registry...");
            registry = RegistryBuilder.constructDefaultRegistry();
            logger.debug(" ... registry initialized");
        }
        else
        {
            logger.debug("Registry already initialized...");
        }
    }

    /**
     * Shutdown the registry so to notify every
     * {@link org.apache.hivemind.events.RegistryShutdownListener}.
     */
    public void dispose()
    {
        if (registry != null)
        {
            registry.shutdown();
            logger.debug("Registry halted");
        }
    }
}
