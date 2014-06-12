/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class TcpSendNoDelayConfigurationTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "send-tcp-no-delay-configuration-test.xml";
    }

    @Test
    public void tcpNoDelay() throws Exception
    {
        TcpConnector tcpConnector = (TcpConnector) muleContext.getRegistry().lookupConnector("tcpConnector");
        assertEquals(getDefaultSendTcpNoDelay(), tcpConnector.isSendTcpNoDelay());
    }

    @Test
    public void tcpNoDelayTrue() throws Exception
    {
        TcpConnector tcpConnector = (TcpConnector) muleContext.getRegistry().lookupConnector(
            "tcpConnectorSendTcpNoDelayTrue");
        assertTrue(tcpConnector.isSendTcpNoDelay());
    }

    @Test
    public void tcpNoDelayFalse() throws Exception
    {
        TcpConnector tcpConnector = (TcpConnector) muleContext.getRegistry().lookupConnector(
            "tcpConnectorSendTcpNoDelayFalse");
        assertFalse(tcpConnector.isSendTcpNoDelay());
    }

    protected boolean getDefaultSendTcpNoDelay()
    {
        return false;
    }

}
