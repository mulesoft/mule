/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config;

import org.mule.MuleManager;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.util.queue.QueueConfiguration;

/**
 * <code>QueueProfile</code> determines how an internal queue for a component
 * will behave
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */

public class QueueProfile
{
    private int maxOutstandingMessages = 0;
    private boolean persistent = false;

    public QueueProfile()
    {
        super();
    }

    public QueueProfile(int maxOutstandingMessages, boolean persistent)
    {
        this.maxOutstandingMessages = maxOutstandingMessages;
        this.persistent = persistent;
    }

    public QueueProfile(QueueProfile queueProfile)
    {
        this.maxOutstandingMessages = queueProfile.getMaxOutstandingMessages();
        this.persistent = queueProfile.isPersistent();
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
     * @param maxOutstandingMessages the max number of messages that will be
     *            queued
     */
    public void setMaxOutstandingMessages(int maxOutstandingMessages)
    {
        this.maxOutstandingMessages = maxOutstandingMessages;
    }

    public boolean isPersistent()
    {
        return persistent;
    }

    public void setPersistent(boolean persistent)
    {
        this.persistent = persistent;
    }

    public void configureQueue(String component) throws InitialisationException
    {
        QueueConfiguration qc = new QueueConfiguration(maxOutstandingMessages, persistent);
        MuleManager.getInstance().getQueueManager().setQueueConfiguration(component, qc);
    }

}
