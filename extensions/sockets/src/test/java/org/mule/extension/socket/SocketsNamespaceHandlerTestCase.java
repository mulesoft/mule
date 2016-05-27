/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.module.socket.api.SocketsExtension;
import org.mule.module.socket.api.TcpClientSocketProperties;
import org.mule.module.socket.api.TcpServerSocketProperties;

import org.junit.Test;

public class SocketsNamespaceHandlerTestCase extends ExtensionFunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "sockets-config.xml";
    }

    @Override
    protected Class<?>[] getAnnotatedExtensionClasses()
    {
        return new Class<?>[] {SocketsExtension.class};
    }

    @Test
    public void testDefaultServerSocketProperties()
    {
        TcpServerSocketProperties properties = muleContext.getRegistry().get("defaultServerSocketProperties");

        assertThat(properties.getKeepAlive(), is(nullValue()));
        assertThat(properties.getReceiveBacklog(), is(nullValue()));
        assertThat(properties.getReceiveBufferSize(), is(nullValue()));
        assertThat(properties.getReuseAddress(), equalTo(true));
        assertThat(properties.getSendBufferSize(), is(nullValue()));
        assertThat(properties.getSendTcpNoDelay(), equalTo(true));
        assertThat(properties.getServerTimeout(), equalTo(0));
        assertThat(properties.getTimeout(), equalTo(0));
        assertThat(properties.getLinger(), equalTo(-1));
    }

    @Test
    public void testServerSocketProperties()
    {
        TcpServerSocketProperties properties = muleContext.getRegistry().get("serverSocketProperties");

        assertThat(properties.getKeepAlive(), equalTo(true));
        assertThat(properties.getReceiveBacklog(), equalTo(200));
        assertThat(properties.getReceiveBufferSize(), equalTo(1024));
        assertThat(properties.getReuseAddress(), equalTo(true));
        assertThat(properties.getSendBufferSize(), equalTo(2048));
        assertThat(properties.getSendTcpNoDelay(), equalTo(true));
        assertThat(properties.getServerTimeout(), equalTo(600));
        assertThat(properties.getTimeout(), equalTo(800));
        assertThat(properties.getLinger(), equalTo(700));
    }

    @Test
    public void testDefaultClientSocketProperties()
    {
        TcpClientSocketProperties properties = muleContext.getRegistry().get("defaultClientSocketProperties");

        assertThat(properties.getKeepAlive(), is(nullValue()));
        assertThat(properties.getReceiveBufferSize(), is(nullValue()));
        assertThat(properties.getSendBufferSize(), is(nullValue()));
        assertThat(properties.getSendTcpNoDelay(), equalTo(true));
        assertThat(properties.getTimeout(), equalTo(0));
        assertThat(properties.getLinger(), equalTo(-1));
        assertThat(properties.getConnectionTimeout(), equalTo(30000));
    }

    @Test
    public void testClientSocketProperties()
    {
        TcpClientSocketProperties properties = muleContext.getRegistry().get("clientSocketProperties");

        assertThat(properties.getConnectionTimeout(), equalTo(500));
        assertThat(properties.getKeepAlive(), equalTo(true));
        assertThat(properties.getReceiveBufferSize(), equalTo(1024));
        assertThat(properties.getSendBufferSize(), equalTo(2048));
        assertThat(properties.getSendTcpNoDelay(), equalTo(true));
        assertThat(properties.getTimeout(), equalTo(600));
        assertThat(properties.getLinger(), equalTo(700));
    }
}
