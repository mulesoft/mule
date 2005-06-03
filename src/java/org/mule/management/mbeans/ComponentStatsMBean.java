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
package org.mule.management.mbeans;

import javax.management.ObjectName;

/**
 * <code>ComponentStatsMBean</code> TODO
 * 
 * @author Guillaume Nodet
 * @version $Revision$
 */
public interface ComponentStatsMBean
{

    void clear();

    ObjectName getRouterInbound();

    ObjectName getRouterOutbound();

    long getAverageExecutionTime();

    long getAverageQueueSize();

    long getMaxQueueSize();

    long getMaxExecutionTime();

    long getFatalErrors();

    long getMinExecutionTime();

    long getTotalExecutionTime();

    long getQueuedEvents();

    long getAsyncEventsReceived();

    long getSyncEventsReceived();

    long getReplyToEventsSent();

    long getSyncEventsSent();

    long getAsyncEventsSent();

    long getTotalEventsSent();

    long getTotalEventsReceived();

    long getExecutedEvents();

    long getExecutionErrors();
}
