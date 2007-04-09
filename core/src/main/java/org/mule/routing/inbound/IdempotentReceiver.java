/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.inbound;

import org.mule.RegistryContext;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.MessagingException;
import org.mule.umo.UMOEvent;
import org.mule.umo.routing.RoutingException;
import org.mule.util.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * <code>IdempotentReceiver</code> ensures that only unique messages are received
 * by a component. It does this by checking the unique id of the incoming message.
 * Note that the underlying endpoint must support unique message Ids for this to
 * work, otherwise a <code>UniqueIdNotSupportedException</code> is thrown. This
 * implementation is simple and not suitable in a failover environment, this is
 * because previously received message Ids are stored in memory and not persisted.
 * 
 */
//TODO MULE-1300: fix memory leak
public class IdempotentReceiver extends SelectiveConsumer
{
    private static final String DEFAULT_STORE_PATH = "./idempotent";

    private Set messageIds;
    private File idStore;
    private String componentName;
    private boolean disablePersistence = false;
    private String storePath;

    public IdempotentReceiver()
    {
        messageIds = new HashSet();
        setStorePath(RegistryContext.getConfiguration().getWorkingDirectory() + DEFAULT_STORE_PATH);
    }

    // //@Override
    public boolean isMatch(UMOEvent event) throws MessagingException
    {
        if (idStore == null)
        {
            // we need to load this of fist request as we need the component
            // name
            this.load(event);
        }
        return !messageIds.contains(this.getIdForEvent(event));
    }

    // //@Override
    public UMOEvent[] process(UMOEvent event) throws MessagingException
    {
        if (isMatch(event))
        {
            try
            {
                checkComponentName(event.getComponent().getDescriptor().getName());
            }
            catch (IllegalArgumentException e)
            {
                throw new RoutingException(event.getMessage(), event.getEndpoint(), e);
            }

            Object id = this.getIdForEvent(event);
            try
            {
                this.storeId(id);
                return new UMOEvent[]{event};
            }
            catch (IOException e)
            {
                throw new RoutingException(
                    new Message(Messages.FAILED_TO_WRITE_X_TO_STORE_X, id, idStore.getAbsolutePath()), 
                    event.getMessage(), event.getEndpoint(), e);
            }
        }
        else
        {
            return null;
        }
    }

    protected Object getIdForEvent(UMOEvent event) throws MessagingException
    {
        return event.getMessage().getUniqueId();
    }

    private void checkComponentName(String name) throws IllegalArgumentException
    {
        if (!componentName.equals(name))
        {
            throw new IllegalArgumentException("This receiver is assigned to component: " + componentName
                                               + " but has received an event for component: " + name
                                               + ". Please check your config to make sure each component"
                                               + "has its own instance of IdempotentReceiver");
        }
    }

    protected synchronized void load(UMOEvent event) throws RoutingException
    {
        this.componentName = event.getComponent().getDescriptor().getName();
        
        if (idStore == null)
        {
            idStore = FileUtils.newFile(storePath + "/muleComponent_" + componentName + ".store");
        }

        if (disablePersistence)
        {
            return;
        }

        try
        {
            if (idStore.exists())
            {
                BufferedReader reader = null;
                try
                {
                    reader = new BufferedReader(new FileReader(idStore));
                    String id;
                    while ((id = reader.readLine()) != null)
                    {
                        messageIds.add(id);
                    }
                }
                finally
                {
                    if (reader != null)
                    {
                        reader.close();
                    }
                }
            }
            else
            {
                idStore = FileUtils.createFile(idStore.getAbsolutePath());
            }
        }
        catch (IOException e)
        {
            throw new RoutingException(
                new Message(Messages.FAILED_TO_READ_FROM_STORE_X, idStore.getAbsolutePath()),
                event.getMessage(), event.getEndpoint(), e);
        }
    }

    protected synchronized void storeId(Object id) throws IOException
    {
        messageIds.add(id);
        if (disablePersistence)
        {
            return;
        }
        FileUtils.stringToFile(idStore.getAbsolutePath(), id.toString(), true, true);
    }

    public boolean isDisablePersistence()
    {
        return disablePersistence;
    }

    public void setDisablePersistence(boolean disablePersistence)
    {
        this.disablePersistence = disablePersistence;
    }

    public String getStorePath()
    {
        return storePath;
    }

    public void setStorePath(String storePath)
    {
        if (storePath == null)
        {
            this.storePath = DEFAULT_STORE_PATH;
        }
        else if (storePath.endsWith("/"))
        {
            storePath = storePath.substring(0, storePath.length() - 1);
            this.storePath = storePath;
        }
        else
        {
            this.storePath = storePath;
        }
    }

}
