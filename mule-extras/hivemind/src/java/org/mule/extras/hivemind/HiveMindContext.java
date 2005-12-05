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

import java.io.Reader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hivemind.Registry;
import org.apache.hivemind.impl.RegistryBuilder;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.container.AbstractContainerContext;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.RecoverableException;
import org.mule.umo.manager.ContainerException;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.util.ClassHelper;

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
                Class keyClass = ClassHelper.loadClass((String) key, getClass());
                component = registry.getService(keyClass);
            } catch (ClassNotFoundException e) {
            	   throw new ObjectNotFoundException("Component class not found: " + key.toString());
            }
        } else if (key instanceof Class) {
            component = registry.getService((Class) key);
        }
        
        if (component == null) {
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
    	    	try {
    	    		registry = RegistryBuilder.constructDefaultRegistry();
    	    		
    	    	} catch (Exception e) {
    	    		throw new InitialisationException(new Message(Messages.FAILED_TO_CONFIGURE_CONTAINER),e,this);
    	    	}
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
        }
    }
}
