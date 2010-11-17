/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.management.mbean;

/**
 * <code>FlowConstructStatsMBean</code> defines the management interface for information common to services and flows.
 */
public interface AbstractFlowConstructStatsMBean
{
    void clearStatistics();

    long getAsyncEventsReceived();

    long getSyncEventsReceived();

    long getTotalEventsReceived();
}