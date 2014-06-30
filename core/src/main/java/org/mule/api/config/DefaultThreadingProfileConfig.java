/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.config;

public class DefaultThreadingProfileConfig
{

    public static final String MAX_THREADS_ACTIVE_PROPERTY = MuleProperties.SYSTEM_PROPERTY_PREFIX + "defaultThreadingProfile.maxThreadsActive";
    public static final String MAX_THREADS_IDLE_PROPERTY = MuleProperties.SYSTEM_PROPERTY_PREFIX + "defaultThreadingProfile.maxThreadsIdle";
    public static final String MAX_BUFFER_SIZE_PROPERTY = MuleProperties.SYSTEM_PROPERTY_PREFIX + "defaultThreadingProfile.maxBufferSize";
    public static final String MAX_THREAD_TTL_PROPERTY = MuleProperties.SYSTEM_PROPERTY_PREFIX + "defaultThreadingProfile.maxThreadTTL";
    public static final String MAX_WAIT_TIMEOUT_PROPERTY = MuleProperties.SYSTEM_PROPERTY_PREFIX + "defaultThreadingProfile.maxWaitTimeout";

    /**
     * Default value for MAX_THREADS_ACTIVE
     */
    public static final int DEFAULT_MAX_THREADS_ACTIVE = Integer.parseInt(System.getProperty(MAX_THREADS_ACTIVE_PROPERTY, "16"));

    /**
     * Default value for MAX_THREADS_IDLE
     */
    public static final int DEFAULT_MAX_THREADS_IDLE = Integer.parseInt(System.getProperty(MAX_THREADS_IDLE_PROPERTY, "1"));

    /**
     * Default value for MAX_BUFFER_SIZE
     */
    public static final int DEFAULT_MAX_BUFFER_SIZE = Integer.parseInt(System.getProperty(MAX_BUFFER_SIZE_PROPERTY, "0"));

    /**
     * Default value for MAX_THREAD_TTL
     */
    public static final long DEFAULT_MAX_THREAD_TTL = Integer.parseInt(System.getProperty(MAX_THREAD_TTL_PROPERTY, "60000"));

    /**
     * Default value for DEFAULT_THREAD_WAIT_TIMEOUT
     */
    public static final long DEFAULT_THREAD_WAIT_TIMEOUT = Long.parseLong(System.getProperty(MAX_WAIT_TIMEOUT_PROPERTY, "30000"));

    private DefaultThreadingProfileConfig()
    {
    }
}
