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
import org.mule.api.lifecycle.InitialisationException;
import org.mule.util.queue.QueueConfiguration;
import org.mule.util.queue.QueueManager;

/**
 * <code>QueueProfile</code> determines how an internal queue for a service will
 * behave
 */

public class QueueProfile
{
    private int maxOutstandingMessages = 0;
    private String storeName;

    public QueueProfile()
    {
        this(0, false);
    }

    public QueueProfile(int maxOutstandingMessages, boolean persistent)
    {
        this.maxOutstandingMessages = maxOutstandingMessages;
        this.storeName = persistent ? MuleProperties.OBJECT_STORE_PERSISTENT_NAME : MuleProperties.OBJECT_STORE_IN_MEMORY_NAME;
    }

    public QueueProfile(QueueProfile queueProfile)
    {
        this.maxOutstandingMessages = queueProfile.getMaxOutstandingMessages();
        this.storeName = queueProfile.storeName;
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
        QueueConfiguration qc = new QueueConfiguration(context, maxOutstandingMessages, storeName);
        queueManager.setQueueConfiguration(component, qc);
        return qc;
    }

    public String getStoreName()
    {
        return storeName;
    }

    public void setStoreName(String storeName)
    {
        this.storeName = storeName;
    }

    public String toString()
    {
        return "QueueProfile{maxOutstandingMessage=" + maxOutstandingMessages + ", storeName="
               + storeName + "}";
    }
}
