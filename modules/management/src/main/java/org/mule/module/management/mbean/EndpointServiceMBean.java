/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.management.mbean;

import org.mule.MessageExchangePattern;

/**
 * The EndpointServiceMBean allows you to check the confiugration of an endpoint and
 * conect/disconnect endpoints manually.
 */
public interface EndpointServiceMBean
{
    String getAddress();

    String getName();

    boolean isConnected();

    void connect() throws Exception;

    void disconnect() throws Exception;

    boolean isInbound();

    boolean isOutbound();

    MessageExchangePattern getMessageExchangePattern();

    String getComponentName();
}
