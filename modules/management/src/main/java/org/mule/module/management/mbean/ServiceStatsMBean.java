/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.management.mbean;

/**
 * <code>ServiceStatsMBean</code> TODO
 */
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
