/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.routing.inbound;

import org.mule.MuleManager;
import org.mule.umo.UMOEvent;
import org.mule.umo.provider.UniqueIdNotSupportedException;
import org.mule.umo.routing.RoutingException;
import org.mule.util.Utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * <code>IdempotentReceiver</code> ensures that only unique messages are received by a component.
 * It does this by checking the unique id of the incoming message. Note that the underlying endpoint
 * must support unique message Ids for this to work, otherwise a <code>UniqueIdNotSupportedException</code>
 * is thrown.
 *
 * This implementation is simple and not suitable in a failover environment, this is because previously received
 * message Ids are stored in memory and not persisted.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class IdempotentReceiver extends SelectiveConsumer
{
    private static String DEFAULT_STORE_PATH = "./idempotent";

    private Set messageIds;
    private File idStore;
    private String componentName;
    private boolean disablePersistence = false;
    private String storePath;

    public IdempotentReceiver() {
        messageIds = new HashSet();
        DEFAULT_STORE_PATH = MuleManager.getConfiguration().getWorkingDirectoy() + "/idempotent";
        setStorePath(DEFAULT_STORE_PATH);
    }

    public boolean isMatch(UMOEvent event) throws RoutingException
    {
        if(idStore==null) {
            //we need to load this of fist request as we need the component name
            load(event.getComponent().getDescriptor().getName());
        }


        try
        {
            return !messageIds.contains(event.getMessage().getUniqueId());
        } catch (UniqueIdNotSupportedException e)
        {
            throw new RoutingException(e.getMessage(), e);
        }
    }

    public UMOEvent[] process(UMOEvent event) throws RoutingException
    {
        if(isMatch(event)) {
            checkComponentName(event.getComponent().getDescriptor().getName());
            try
            {
                storeId(event.getMessage().getUniqueId());
                return new UMOEvent[]{event};
            } catch (UniqueIdNotSupportedException e)
            {
                throw new RoutingException(e.getMessage(), e);
            }
        } else {
            return null;
        }
    }

    private void checkComponentName(String name) throws RoutingException
    {
        if(!componentName.equals(name)) {
            throw new RoutingException("This receiver is assigned to component: " + componentName +
                    " but has received an event for component: " + name + ". Please check your config to make sure each component" +
                    "has its own instance of IdempotentReceiver");
        }
    }

    protected synchronized void load(String componentName) throws RoutingException
    {
        this.componentName = componentName;
        idStore = new File(storePath + "/muleComponent_" + componentName + ".store");
        if(disablePersistence) return;
        try
        {
            if(idStore.exists()) {
                BufferedReader reader = null;
                try
                {
                    reader = new BufferedReader(new FileReader(idStore));
                    String id;
                    while((id = reader.readLine())!=null) {
                        messageIds.add(id);
                    }
                } finally
                {
                    if(reader!=null) reader.close();
                }
            } else {
                idStore = Utility.createFile(idStore.getAbsolutePath());
            }
        } catch (IOException e)
        {
            throw new RoutingException("Failed to load Idempotent receiver message id store from: " +
                    idStore.getAbsolutePath() + ". " + e.getMessage(), e);
        }
    }
    protected synchronized void storeId(Object id) throws RoutingException
    {
        messageIds.add(id);
        if(disablePersistence) return;

        try
        {
            Utility.stringToFile(idStore.getAbsolutePath(), id.toString(), true, true);
        } catch (IOException e)
        {
            throw new RoutingException("Failed to write message id: " + id + " to Idempotent receiver store at: " +
                    idStore.getAbsolutePath() + ". " + e.getMessage(), e);
        }
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
        if(storePath==null) {
            this.storePath = DEFAULT_STORE_PATH;
        }else if(storePath.endsWith("/")) {
            storePath = storePath.substring(0, storePath.length() - 1);
            this.storePath = storePath;
        } else {
            this.storePath = storePath;
        }
    }
}
