/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config;

import org.mule.api.MuleContext;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.store.ListableObjectStore;
import org.mule.api.store.QueueObjectStore;
import org.mule.util.queue.QueueConfiguration;
import org.mule.util.queue.QueueManager;

import java.io.Serializable;

/**
 * <code>QueueProfile</code> determines how an internal queue for a service will
 * behave
 */

public class QueueProfile
{
    private int maxOutstandingMessages = 0;
    private QueueObjectStore<Serializable> objectStore;
    
    public static QueueProfile newInstancePersistingToDefaultMemoryQueueStore(MuleContext muleContext)
    {
        QueueObjectStore<Serializable> defaultMemoryObjectStore =
            muleContext.getRegistry().lookupObject(MuleProperties.QUEUE_STORE_DEFAULT_IN_MEMORY_NAME);
        return new QueueProfile(defaultMemoryObjectStore);
    }

    public QueueProfile(QueueObjectStore<Serializable> objectStore)
    {
        this.objectStore = objectStore;
    }

    // TODO DO: is this ctor required at all? It's not used anywhere in the code base
    public QueueProfile(QueueProfile queueProfile)
    {
        this.maxOutstandingMessages = queueProfile.getMaxOutstandingMessages();
        this.objectStore = queueProfile.objectStore;
    }

    public QueueProfile(int maxOutstandingMessages, QueueObjectStore<Serializable> objectStore)
    {
        this.maxOutstandingMessages = maxOutstandingMessages;
        this.objectStore = objectStore;
    }
    
    /**
     * This specifies the number of messages that can be queued before it starts
     * blocking.
     * 
     * @return the max number of messages that will be queued
     */
    public int getMaxOutstandingMessages()
    {
        return maxOutstandingMessages;
    }

    /**
     * This specifies the number of messages that can be queued before it starts
     * blocking.
     * 
     * @param maxOutstandingMessages the max number of messages that will be queued
     */
    public void setMaxOutstandingMessages(int maxOutstandingMessages)
    {
        this.maxOutstandingMessages = maxOutstandingMessages;
    }

    public QueueConfiguration configureQueue(MuleContext context, String component, QueueManager queueManager) throws InitialisationException
    {
        if (objectStore instanceof MuleContextAware)
        {
            ((MuleContextAware) objectStore).setMuleContext(context);
        }
        QueueConfiguration qc = new QueueConfiguration(context, maxOutstandingMessages, objectStore);
        queueManager.setQueueConfiguration(component, qc);
        return qc;
    }

    public ListableObjectStore<Serializable> getObjectStore()
    {
        return objectStore;
    }

    public void setQueueStore(QueueObjectStore<Serializable> objectStore)
    {
        this.objectStore = objectStore;
    }

    public void addQueueStore(QueueObjectStore<Serializable> objectStore)
    {
        this.objectStore = objectStore;
    }

    @Override
    public String toString()
    {
        return "QueueProfile{maxOutstandingMessage=" + maxOutstandingMessages + ", storeType="
               + objectStore.getClass() + "}";
    }
}
