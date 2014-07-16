/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.management.mbean;

/**
 * <code>ServiceStatsMBean</code> TODO
 */
@Deprecated
public interface ServiceStatsMBean extends FlowConstructStatsMBean
{

    long getAverageExecutionTime();

    long getAverageQueueSize();

    long getMaxQueueSize();

    long getMaxExecutionTime();

    long getMinExecutionTime();

    long getTotalExecutionTime();

    long getQueuedEvents();

    long getReplyToEventsSent();

    long getSyncEventsSent();

    long getAsyncEventsSent();

    long getTotalEventsSent();

    long getExecutedEvents();

}
