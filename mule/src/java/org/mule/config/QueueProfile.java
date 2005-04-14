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
package org.mule.config;

import org.mule.umo.lifecycle.InitialisationException;
import org.mule.util.queue.BoundedPersistentQueue;
import org.mule.util.queue.PersistenceStrategy;

/**
 * <code>QueueProfile</code> determines how an internal queue for a component will
 * behave
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class QueueProfile
{
    private int maxOutstandingMessages = 100;
    private long blockWait = 2000;
    private PersistenceStrategy persistenceStrategy;

    public QueueProfile()
    {
    }

    public QueueProfile(int maxOutstandingMessages, PersistenceStrategy persistenceStrategy)
    {
        this.maxOutstandingMessages = maxOutstandingMessages;
        this.persistenceStrategy = persistenceStrategy;
    }

    public QueueProfile(QueueProfile queueProfile)
    {
        this.maxOutstandingMessages = queueProfile.getMaxOutstandingMessages();
        this.persistenceStrategy = queueProfile.getPersistenceStrategy();
    }

    /**
     * This specifies the number of messages that can be queued for this component before it starts
     * blocking.
     *
     * @return the max number of messages that will be queued
     */
    public int getMaxOutstandingMessages()
    {
        return maxOutstandingMessages;
    }

    /**
     * This specifies the number of messages that can be queued for this component before it starts
     * blocking.
     *
     * @param maxOutstandingMessages the max number of messages that will be queued
     */
    public void setMaxOutstandingMessages(int maxOutstandingMessages)
    {
        this.maxOutstandingMessages = maxOutstandingMessages;
    }

    public PersistenceStrategy getPersistenceStrategy()
    {
        return persistenceStrategy;
    }

    public void setPersistenceStrategy(PersistenceStrategy persistenceStrategy)
    {
        this.persistenceStrategy = persistenceStrategy;
    }

    public BoundedPersistentQueue createQueue(String component) throws InitialisationException
    {
        return new BoundedPersistentQueue(maxOutstandingMessages, persistenceStrategy, component, true);
    }

    public long getBlockWait()
    {
        return blockWait;
    }

    public void setBlockWait(long blockWait)
    {
        this.blockWait = blockWait;
    }
}
