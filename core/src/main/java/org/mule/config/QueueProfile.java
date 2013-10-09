/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config;

import org.mule.api.MuleContext;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.store.ListableObjectStore;
import org.mule.api.store.QueueStore;
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
    private QueueStore<Serializable> objectStore;
    
    public static QueueProfile newInstancePersistingToDefaultMemoryQueueStore(MuleContext muleContext)
    {
        QueueStore<Serializable> defaultMemoryObjectStore =
            muleContext.getRegistry().lookupObject(MuleProperties.QUEUE_STORE_DEFAULT_IN_MEMORY_NAME);
        return new QueueProfile(defaultMemoryObjectStore);
    }

    public QueueProfile(QueueStore<Serializable> objectStore)
    {
        this.objectStore = objectStore;
    }

    // TODO DO: is this ctor required at all? It's not used anywhere in the code base
    public QueueProfile(QueueProfile queueProfile)
    {
        this.maxOutstandingMessages = queueProfile.getMaxOutstandingMessages();
        this.objectStore = queueProfile.objectStore;
    }

    public QueueProfile(int maxOutstandingMessages, QueueStore<Serializable> objectStore)
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

    public void setQueueStore(QueueStore<Serializable> objectStore)
    {
        this.objectStore = objectStore;
    }

    public void addQueueStore(QueueStore<Serializable> objectStore)
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
