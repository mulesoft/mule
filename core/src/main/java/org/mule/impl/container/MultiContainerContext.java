/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.container;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.ContainerException;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.umo.manager.UMOContainerContext;

import java.io.Reader;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>MultiContainerContext</code> is a container that hosts other containers
 * from which components are queried.
 */
public class MultiContainerContext implements UMOContainerContext
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(MultiContainerContext.class);

    private String name = "multi";
    private TreeMap containers = new TreeMap();

    public MultiContainerContext()
    {
        addContainer(new MuleContainerContext());
        addContainer(new DescriptorContainerContext());
    }

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
        if (containers.containsKey(container.getName()))
        {
            throw new IllegalArgumentException(new Message(Messages.CONTAINER_X_ALREADY_REGISTERED,
                container.getName()).toString());
        }
        containers.put(container.getName(), container);
    }

    public UMOContainerContext removeContainer(String name)
    {
        return (UMOContainerContext)containers.remove(name);
    }

    public Object getComponent(Object key) throws ObjectNotFoundException
    {
        // first see if a particular container has been requested
        ContainerKeyPair realKey = null;
        String cause = null;
        if (key instanceof String)
        {
            realKey = new ContainerKeyPair(null, key);
        }
        else
        {
            realKey = (ContainerKeyPair)key;
        }

        Object component = null;
        UMOContainerContext container;
        if (realKey.getContainerName() != null)
        {
            container = (UMOContainerContext)containers.get(realKey.getContainerName());
            if (container != null)
            {
                return container.getComponent(realKey);
            }
            else
            {
                throw new ObjectNotFoundException("Container: " + realKey.getContainerName());
            }
        }

        for (Iterator iterator = containers.values().iterator(); iterator.hasNext();)
        {
            container = (UMOContainerContext)iterator.next();
            try
            {
                component = container.getComponent(realKey);
            }
            catch (ObjectNotFoundException e)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Object: '" + realKey + "' not found in container: " + container.getName(),
                        e.getCause());
                }
                if (e.getCause() != null){
                    cause = cause + " " + e.getCause().toString();
                }
            }
            if (component != null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Object: '" + realKey + "' found in container: " + container.getName());
                }
                break;
            }
        }
        if (component == null)
        {
            if (realKey.isRequired())
            {
                throw new ObjectNotFoundException(realKey.toString() + " " + cause);
            }
            else if (logger.isDebugEnabled())
            {
                logger.debug("Component reference not found: " + realKey.toFullString());
                return null;
            }
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
        for (Iterator iterator = containers.values().iterator(); iterator.hasNext();)
        {
            container = (UMOContainerContext)iterator.next();
            container.dispose();
        }
        containers.clear();
        containers = null;
    }

    public void initialise() throws InitialisationException
    {
        // no op
    }

}
