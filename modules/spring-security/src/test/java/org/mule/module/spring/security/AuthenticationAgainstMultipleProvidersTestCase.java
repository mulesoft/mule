/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.spring.security;

import static org.junit.Assert.assertEquals;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class AuthenticationAgainstMultipleProvidersTestCase extends AbstractServiceAndFlowTestCase
{

    @Rule
    public DynamicPort httpPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort httpPort2 = new DynamicPort("port2");

    @Rule
    public DynamicPort httpPort3 = new DynamicPort("port3");

    public AuthenticationAgainstMultipleProvidersTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE, "mule-multiple-providers-config-service.xml"},
            {ConfigVariant.FLOW, "mule-multiple-providers-config-flow.xml"}});
    }

    @Test
    public void testProvider1() throws Exception
    {
        HttpClient httpClient = new HttpClient();
        Credentials credentials = new UsernamePasswordCredentials("admin1", "admin1");
        httpClient.getState().setCredentials(AuthScope.ANY, credentials);
        httpClient.getParams().setAuthenticationPreemptive(true);

        PostMethod postMethod = new PostMethod("http://localhost:" + httpPort1.getNumber());
        postMethod.setDoAuthentication(true);
        postMethod.setRequestEntity(new StringRequestEntity("hello", "text/html", "UTF-8"));

        assertEquals(HttpStatus.SC_OK, httpClient.executeMethod(postMethod));
        assertEquals("hello", postMethod.getResponseBodyAsString());

        credentials = new UsernamePasswordCredentials("asdf", "asdf");
        httpClient.getState().setCredentials(AuthScope.ANY, credentials);
        assertEquals(HttpStatus.SC_UNAUTHORIZED, httpClient.executeMethod(postMethod));

        credentials = new UsernamePasswordCredentials("admin2", "admin2");
        httpClient.getState().setCredentials(AuthScope.ANY, credentials);
        assertEquals(HttpStatus.SC_UNAUTHORIZED, httpClient.executeMethod(postMethod));
    }

    @Test
    public void testProvider2() throws Exception
    {
        HttpClient httpClient = new HttpClient();
        Credentials credentials = new UsernamePasswordCredentials("admin2", "admin2");
        httpClient.getState().setCredentials(AuthScope.ANY, credentials);
        httpClient.getParams().setAuthenticationPreemptive(true);

        PostMethod postMethod = new PostMethod("http://localhost:" + httpPort2.getNumber());
        postMethod.setDoAuthentication(true);
        postMethod.setRequestEntity(new StringRequestEntity("hello", "text/html", "UTF-8"));

        assertEquals(HttpStatus.SC_OK, httpClient.executeMethod(postMethod));
        assertEquals("hello", postMethod.getResponseBodyAsString());

        credentials = new UsernamePasswordCredentials("asdf", "asdf");
        httpClient.getState().setCredentials(AuthScope.ANY, credentials);
        assertEquals(HttpStatus.SC_UNAUTHORIZED, httpClient.executeMethod(postMethod));

        credentials = new UsernamePasswordCredentials("admin", "admin");
        httpClient.getState().setCredentials(AuthScope.ANY, credentials);
        assertEquals(HttpStatus.SC_UNAUTHORIZED, httpClient.executeMethod(postMethod));
    }

    @Test
    public void testMultipleProviders() throws Exception
    {
        HttpClient httpClient = new HttpClient();
        Credentials credentials = new UsernamePasswordCredentials("admin1", "admin1");
        httpClient.getState().setCredentials(AuthScope.ANY, credentials);
        httpClient.getParams().setAuthenticationPreemptive(true);

        PostMethod postMethod = new PostMethod("http://localhost:" + httpPort3.getNumber());
        postMethod.setDoAuthentication(true);
        postMethod.setRequestEntity(new StringRequestEntity("hello", "text/html", "UTF-8"));

        assertEquals(HttpStatus.SC_OK, httpClient.executeMethod(postMethod));
        assertEquals("hello", postMethod.getResponseBodyAsString());

        credentials = new UsernamePasswordCredentials("asdf", "asdf");
        httpClient.getState().setCredentials(AuthScope.ANY, credentials);
        assertEquals(HttpStatus.SC_UNAUTHORIZED, httpClient.executeMethod(postMethod));

        credentials = new UsernamePasswordCredentials("admin2", "admin2");
        httpClient.getState().setCredentials(AuthScope.ANY, credentials);
        assertEquals(HttpStatus.SC_OK, httpClient.executeMethod(postMethod));
        assertEquals("hello", postMethod.getResponseBodyAsString());
    }

}
