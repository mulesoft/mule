/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.impl.container;

import java.io.Reader;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.ContainerException;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.umo.manager.UMOContainerContext;

/**
 * <code>MultiContainerContext</code> is a container that hosts other
 * containers from which components are queried.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MultiContainerContext implements UMOContainerContext
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(MultiContainerContext.class);

    private String name = "multi";
    private UMOContainerContext defaultContainer = new MuleContainerContext();
    private TreeMap containers = new TreeMap();

    public void setName(String name)
    {
        // noop
    }

    public String getName()
    {
        return name;
    }

    public void addContainer(UMOContainerContext container)
    {
        if (!(container instanceof MuleContainerContext)) {
            if (containers.containsKey(container.getName())) {
                throw new IllegalArgumentException(new Message(Messages.CONTAINER_X_ALREADY_REGISTERED,
                                                               container.getName()).toString());
            }
            containers.put(container.getName(), container);
        }
    }

    public UMOContainerContext removeContainer(String name)
    {
        return (UMOContainerContext) containers.remove(name);
    }

    public Object getComponent(Object key) throws ObjectNotFoundException
    {
        // first see if a particular container has been requested
        String containerName = null;
        Object realKey = key;
        if (key instanceof ContainerKeyPair) {
            containerName = ((ContainerKeyPair) key).getContaimerName();
            realKey = ((ContainerKeyPair) key).getKey();
        }

        Object component = null;
        UMOContainerContext container;
        if (containerName != null) {
            container = (UMOContainerContext) containers.get(containerName);
            if (container != null) {
                return container.getComponent(realKey);
            } else {
                throw new ObjectNotFoundException("Container: " + containerName);
            }
        }

        for (Iterator iterator = containers.values().iterator(); iterator.hasNext();) {
            container = (UMOContainerContext) iterator.next();
            try {
                component = container.getComponent(realKey);
            } catch (ObjectNotFoundException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Object: '" + realKey + "' not found in container: " + container.getName());
                }
            }
            if (component != null) {
                if (logger.isInfoEnabled()) {
                    logger.info("Object: '" + realKey + "' found in container: " + container.getName());
                }
                break;
            }
        }
        if (component == null) {
            component = defaultContainer.getComponent(realKey);
        }
        return component;
    }

    public void configure(Reader configuration, String doctype, String encoding) throws ContainerException
    {
        // noop
    }

    public void dispose()
    {
        UMOContainerContext container;
        for (Iterator iterator = containers.values().iterator(); iterator.hasNext();) {
            container = (UMOContainerContext) iterator.next();
            container.dispose();
        }
        defaultContainer.dispose();
        defaultContainer = null;
        containers.clear();
        containers = null;
    }

    public void initialise() throws InitialisationException
    {
    }
}
