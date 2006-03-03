/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Massimo Lusetti. All rights reserved.
 * http://www.datacode.it
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */
package org.mule.extras.hivemind;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hivemind.ApplicationRuntimeException;
import org.apache.hivemind.Registry;
import org.apache.hivemind.impl.RegistryBuilder;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.container.AbstractContainerContext;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.RecoverableException;
import org.mule.umo.manager.ContainerException;
import org.mule.umo.manager.ObjectNotFoundException;

import java.io.Reader;

/**
 * <code>HiveMindContext</code> is a HiveMind Context that can expose
 * HiveMind managed services for use in the Mule framework.
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
	
        if (registry == null) {
            throw new IllegalStateException("HiveMind registry has not been set");
        }
        if (key == null) {
            throw new ObjectNotFoundException("Component not found for null key");
        }
        
        Object component = null;
        
        if (key instanceof String) {
            try {
                component = registry.getService((String)key, Object.class);
                logger.debug("Called " + key + " obtained  " + component.getClass().getName());
            } catch (ApplicationRuntimeException are) {
                throw new ObjectNotFoundException("Component not found for " + key, are);
            }
        } else if (key instanceof Class) {
            try {
                component = registry.getService((Class) key);
                logger.debug("Called " + ((Class) key).getName() + " obtained  " + component.getClass().getName());
            } catch (ApplicationRuntimeException are) {
                throw new ObjectNotFoundException("Component not found for " + key, are);
            }
        }
        
        if (component == null) {
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
        logger.info("HiveMindContext don't need any configuration fragment. Configuration ignored");
    }

    /**
     * Here we build the registry from the standard deployment descriptors
     * location.
     */
    public void initialise() throws InitialisationException, RecoverableException
    {
    	   if (registry == null) {
    	        logger.debug("About to initilise the registry...");
    	    	try {
    	    		registry = RegistryBuilder.constructDefaultRegistry();
    	    		
    	    	} catch (Exception e) {
    	    		throw new InitialisationException(new Message(Messages.FAILED_TO_CONFIGURE_CONTAINER),e,this);
    	    	}
    	        logger.debug(" ... registry initialized");
    	   } else {
    	        logger.debug("Registry already initialized...");
    	   }
    }

    /**
     * Shutdown the registry so to notify every
     * {@link org.apache.hivemind.events.RegistryShutdownListener}.
     */
    public void dispose()
    {
        if (registry != null) {
            registry.shutdown();
            logger.debug("Registry halted");
        }
    }
}
