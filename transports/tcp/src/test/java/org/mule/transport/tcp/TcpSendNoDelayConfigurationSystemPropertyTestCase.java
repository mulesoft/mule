/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp;

import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;

public class TcpSendNoDelayConfigurationSystemPropertyTestCase extends TcpSendNoDelayConfigurationTestCase
{

    private static boolean defaultSendTcpNoDelay = true;

    @Rule
    public SystemProperty SendTcpNoDelaySystemProperty = new SystemProperty(
        TcpConnector.SEND_TCP_NO_DELAY_SYSTEM_PROPERTY, Boolean.toString(defaultSendTcpNoDelay));

    @Override
    protected String getConfigFile()
    {
        return "send-tcp-no-delay-configuration-test.xml";
    }

    protected boolean getDefaultSendTcpNoDelay()
    {
        return defaultSendTcpNoDelay;
    }

}
