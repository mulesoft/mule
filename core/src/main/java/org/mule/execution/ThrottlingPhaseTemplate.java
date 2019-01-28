/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.execution;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;

/**
 * Template that a {@link org.mule.api.source.MessageSource} must implement
 * if it wants to participate in the throttling phase when processing a message
 */
public interface ThrottlingPhaseTemplate extends MessageProcessTemplate
{

    /**
     * @return a {@link org.mule.api.MuleEvent} created from the original message
     */
    MuleEvent getMuleEvent() throws MuleException;

    /**
     * Discards the message due to ThrottlingPolicy configured for the {@link org.mule.api.source.MessageSource} is exceeded
     *
     * @throws MuleException
     */
    void discardMessageOnThrottlingExceeded() throws MuleException;

    /**
     * Set up throttling policy state to be used by the {@link org.mule.api.source.MessageSource} for debugging or
     * information purpose.
     *
     * Not all throttling policy supports statistics so this method may not be called
     *
     * @param remainingRequestInCurrentPeriod the remaining allowed messages in the current period
     * @param maximumRequestAllowedPerPeriod the maximum allowed messages in a period
     * @param timeUntilNextPeriodInMillis time in milliseconds until the next period starts
     */
    /*
       This should no change in the future. The other option was to send the ThrottlingPolicyStatistics object but
       as CE transports are using this behavior it would involve moving the ThrottlingPolicyStatistics to CE.
    */
    void setThrottlingPolicyStatistics(long remainingRequestInCurrentPeriod, long maximumRequestAllowedPerPeriod, long timeUntilNextPeriodInMillis);

    void addExtraHeader(String headerName, String value);
}
