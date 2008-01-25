/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.container;

import org.mule.api.context.ContainerContext;
import org.mule.api.context.ContainerException;
import org.mule.api.context.ObjectNotFoundException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.i18n.CoreMessages;

import java.io.Reader;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>MultiContainerContext</code> is a container that hosts other containers
 * from which components are queried.
 */
public class MultiContainerContext implements ContainerContext
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
    }

    public void setName(String name)
    {
        // noop
    }

    public String getName()
    {
        return name;
    }

    public void addContainer(ContainerContext container)
    {
        if (containers.containsKey(container.getName()))
        {
            throw new IllegalArgumentException(
                CoreMessages.containerAlreadyRegistered(container.getName()).toString());
        }
        containers.put(container.getName(), container);
    }

    public ContainerContext removeContainer(String name)
    {
        return (ContainerContext) containers.remove(name);
    }

    public Object getComponent(Object key) throws ObjectNotFoundException
    {
        ContainerKeyPair realKey = null;
        StringBuffer cause = new StringBuffer();
        Throwable finalCause = null;

        // first see if a particular container has been requested
        if (key instanceof String)
        {
            realKey = new ContainerKeyPair(null, key);
        }
        else
        {
            realKey = (ContainerKeyPair) key;
        }

        if (realKey == null)
        {
            throw new ObjectNotFoundException(null);
        }
        
        Object component = null;
        ContainerContext container;
        if (realKey.getContainerName() != null)
        {
            container = (ContainerContext) containers.get(realKey.getContainerName());
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
            container = (ContainerContext) iterator.next();
            try
            {
                component = container.getComponent(realKey);
            }
            catch (ObjectNotFoundException e)
            {
                if (e.getCause() != null)
                {
                    finalCause = e.getCause();
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Object: '" + realKey + "' not found in container: " + container.getName());
                    }
                }
                else
                {
                    finalCause = e;
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Object: '" + realKey + "' not found in container: " + container.getName());
                    }
                }

                if (cause.length() > 0)
                {
                    cause.append("; ");
                }
                cause.append(finalCause.toString());
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
                throw new ObjectNotFoundException(realKey.toString() + " " + cause, finalCause);
            }
            else if (logger.isDebugEnabled())
            {
                logger.debug("Service reference not found: " + realKey.toFullString());
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
        ContainerContext container;
        for (Iterator iterator = containers.values().iterator(); iterator.hasNext();)
        {
            container = (ContainerContext) iterator.next();
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
