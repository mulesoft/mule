/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import static org.junit.Assert.assertEquals;
import static org.mule.transport.sftp.config.SftpProxyConfig.HOST_PROPERTY;
import static org.mule.transport.sftp.config.SftpProxyConfig.PORT_PROPERTY;
import static org.mule.transport.sftp.config.SftpProxyConfig.PROTOCOL_PROPERTY;

import org.mule.api.client.MuleClient;
import org.mule.module.http.functional.TestProxyServer;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.ByteArrayInputStream;

import org.junit.Rule;
import org.junit.Test;

public class SftpProxyTestCase extends AbstractSftpFunctionalTestCase
{
    @Rule
    public DynamicPort proxyPort = new DynamicPort("proxyPort");

    TestProxyServer proxyServer = new TestProxyServer(proxyPort.getNumber(), sftpPort.getNumber());

    @Rule
    public SystemProperty proxyHostDef = new SystemProperty(HOST_PROPERTY, "localhost");

    @Rule
    public SystemProperty proxyPortDef = new SystemProperty(PORT_PROPERTY, Integer.toString(proxyPort.getNumber()));

    @Rule
    public SystemProperty proxyProtocolDef = new SystemProperty(PROTOCOL_PROPERTY, "HTTP");

    @Override
    protected String getConfigFile()
    {
        return "sftp-proxy-config.xml";
    }

    @Override
    protected void doSetUpBeforeMuleContextCreation() throws Exception
    {
        super.doSetUpBeforeMuleContextCreation();

        proxyServer.setHttpVersion("1.0");
        proxyServer.start();
    }

    @Override
    protected void doTearDownAfterMuleContextDispose() throws Exception
    {
        proxyServer.stop();

        super.doTearDownAfterMuleContextDispose();
    }

    @Test
    public void connectsThroughProxy() throws Exception
    {
        MuleClient client = muleContext.getClient();
        sftpClient.storeFile("blah", new ByteArrayInputStream(TEST_MESSAGE.getBytes()));
        assertEquals(TEST_MESSAGE, client.request("vm://out", RECEIVE_TIMEOUT).getPayloadAsString());
    }
}
