/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
