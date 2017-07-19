/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.api.requester.proxy;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

public class ProxyConfigBuilderTest extends AbstractMuleTestCase
{

    public static final String HOST = "host";
    public static final int PORT = 8080;
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String NON_PROXY_HOSTS = "host1|host2";

    private ProxyConfigBuilder proxyConfigBuilder = new ProxyConfigBuilder();

    @Test(expected = IllegalArgumentException.class)
    public void onlyHost()
    {
        proxyConfigBuilder.setHost(HOST).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void onlyPort()
    {
        proxyConfigBuilder.setPort(PORT).build();
    }

    @Test
    public void minimalConfig()
    {
        ProxyConfig config =  proxyConfigBuilder
                .setHost(HOST).setPort(PORT).build();
        assertThat(config.getHost(), is(HOST));
        assertThat(config.getPort(), is(PORT));
        assertThat(config.getPassword(), nullValue());
        assertThat(config.getUsername(), nullValue());
    }

    @Test
    public void fullConfig()
    {
        ProxyConfig config = proxyConfigBuilder
                .setHost(HOST).setPort(PORT).setUsername(USERNAME).setPassword(PASSWORD).setNonProxyHosts(NON_PROXY_HOSTS).build();
        assertThat(config.getHost(), is(HOST));
        assertThat(config.getPort(), is(PORT));
        assertThat(config.getPassword(), is(PASSWORD));
        assertThat(config.getUsername(), is(USERNAME));
        assertThat(config.getNonProxyHosts(), is(NON_PROXY_HOSTS));
    }
}