/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.spring.security;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.tck.AbstractServiceAndFlowTestCase;

public class AuthenticationAgainstMultipleProvidersTestCase extends AbstractServiceAndFlowTestCase
{

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

        PostMethod postMethod = new PostMethod("http://localhost:4445");
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

        PostMethod postMethod = new PostMethod("http://localhost:4446");
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

        PostMethod postMethod = new PostMethod("http://localhost:4447");
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
