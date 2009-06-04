/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.config;

/**
 * Configuration info. which can be set when creating the MuleContext but becomes
 * immutable after startup.
 */
public interface MuleConfiguration
{
    /**
     * The prefix for any Mule-specific properties set in the system properties
     */
    String SYSTEM_PROPERTY_PREFIX = "mule.";

    boolean isDefaultSynchronousEndpoints();

    int getDefaultResponseTimeout();

    String getWorkingDirectory();

    String getMuleHomeDirectory();

    int getDefaultTransactionTimeout();

    boolean isClientMode();

    String getDefaultEncoding();

    String getId();

    String getClusterId();

    String getDomainId();

    String getSystemModelType();

    String getSystemName();

    boolean isAutoWrapMessageAwareTransform();

    boolean isCacheMessageAsBytes();

    boolean isCacheMessageOriginalPayload();

    boolean isEnableStreaming();
    
    int getDefaultQueueTimeout();
    
    int getShutdownTimeout();
}
