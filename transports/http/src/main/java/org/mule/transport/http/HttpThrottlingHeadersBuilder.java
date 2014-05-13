/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import java.util.HashMap;
import java.util.Map;

/**
 * Creates the corresponding http headers based on the throttling statistics.
 */
public class HttpThrottlingHeadersBuilder
{

    public static final String X_RATE_LIMIT_LIMIT_HEADER = "X-RateLimit-Limit";
    public static final String X_RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
    public static final String X_RATE_LIMIT_RESET_HEADER = "X-RateLimit-Reset";

    private Long remainingRequestInCurrentPeriod;
    private Long maximumRequestAllowedPerPeriod;
    private Long timeUntilNextPeriodInMillis;

    public void setThrottlingPolicyStatistics(long remainingRequestInCurrentPeriod, long maximumRequestAllowedPerPeriod, long timeUntilNextPeriodInMillis)
    {
        this.remainingRequestInCurrentPeriod = remainingRequestInCurrentPeriod;
        this.maximumRequestAllowedPerPeriod = maximumRequestAllowedPerPeriod;
        this.timeUntilNextPeriodInMillis = timeUntilNextPeriodInMillis;
    }

    public Map<String, String> getThrottlingHeaders()
    {
        Map<String, String> throttlingHeaders = new HashMap<String, String>();
        addToMapIfNotNull(throttlingHeaders, X_RATE_LIMIT_LIMIT_HEADER, this.maximumRequestAllowedPerPeriod);
        addToMapIfNotNull(throttlingHeaders, X_RATE_LIMIT_REMAINING_HEADER, this.remainingRequestInCurrentPeriod);
        addToMapIfNotNull(throttlingHeaders, X_RATE_LIMIT_RESET_HEADER, this.timeUntilNextPeriodInMillis);
        return throttlingHeaders;
    }

    private void addToMapIfNotNull(Map<String, String> map, String key, Long value)
    {
        if (value != null)
        {
            map.put(key, String.valueOf(value));
        }
    }

}
