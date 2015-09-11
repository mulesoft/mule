/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mule.api.security.tls.TlsConfiguration.DISABLE_SYSTEM_PROPERTIES_MAPPING_PROPERTY;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.transport.http.HttpConstants;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class HttpSecurityFilterFunctionalTestCase extends AbstractHttpSecurityTestCase
{
    @Rule
    public SystemProperty disablePropertiesMapping = new SystemProperty(DISABLE_SYSTEM_PROPERTIES_MAPPING_PROPERTY, "false");

    private static String soapRequest =
        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:unk=\"http://unknown.namespace/\">" +
           "<soapenv:Header/>" +
           "<soapenv:Body>" +
              "<unk:echo>" +         
                 "<arg0>asdf</arg0>" +
              "</unk:echo>" +
           "</soapenv:Body>" +
        "</soapenv:Envelope>";

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");
    
    public HttpSecurityFilterFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "http-security-filter-test-service.xml"},
            {ConfigVariant.FLOW, "http-security-filter-test-flow.xml"},
            {ConfigVariant.FLOW, "http-security-filter-test-flow-httpn.xml"}
        });
    }      


    /**
     * By putting this test method that uses https first we can test MULE-4558
     * 
     * @throws Exception
     */
    @Test
    public void testAuthenticationFailureBadCredentialsGetHttps() throws Exception
    {
        doGet(null, "localhost", "anonX", "anonX", "https://localhost:" + dynamicPort2.getNumber() + "/services/Echo", true, 401);
    }

    @Test
    public void testAuthenticationFailureNoContextGet() throws Exception
    {
        HttpClient client = new HttpClient();
        client.getParams().setAuthenticationPreemptive(true);
        GetMethod get = new GetMethod("http://localhost:" + dynamicPort1.getNumber() + "/services/Echo");

        get.setDoAuthentication(false);

        try
        {
            int status = client.executeMethod(get);
            assertEquals(HttpConstants.SC_UNAUTHORIZED, status);
            assertThat(get.getResponseBodyAsString(), startsWith("Registered authentication is set to org.mule.module.spring.security.filters.http.HttpBasicAuthenticationFilter "
                                                                 + "but there was no security context on the session. Authentication denied on endpoint" ));
        }
        finally
        {
            get.releaseConnection();
        }
    }

    @Test
    public void testAuthenticationFailureNoContextPost() throws Exception
    {
        HttpClient client = new HttpClient();
        client.getParams().setAuthenticationPreemptive(true);
        PostMethod post = new PostMethod("http://localhost:" + dynamicPort1.getNumber() + "/services/Echo");

        post.setDoAuthentication(false);

        StringRequestEntity requestEntity = new StringRequestEntity(soapRequest, "text/xml", "UTF-8");
        post.setRequestEntity(requestEntity);

        try
        {
            int status = client.executeMethod(post);
            assertEquals(HttpConstants.SC_UNAUTHORIZED, status);
            assertThat(post.getResponseBodyAsString(), startsWith("Registered authentication is set to org.mule.module.spring.security.filters.http.HttpBasicAuthenticationFilter "
                                                                 + "but there was no security context on the session. Authentication denied on endpoint" ));
        }
        finally
        {
            post.releaseConnection();
        }
    }

    @Test
    public void testAuthenticationFailureBadCredentialsGet() throws Exception
    {
        doGet(null, "localhost", "anonX", "anonX", "http://localhost:" + dynamicPort1.getNumber() + "/services/Echo/echo/echo/hello", true, 401);
    }

    @Test
    public void testAuthenticationFailureBadCredentialsPost() throws Exception
    {
        doPost(null, "localhost", "anonX", "anonX", "http://localhost:" + dynamicPort1.getNumber() + "/services/Echo", true, 401);
    }

    @Test
    public void testAuthenticationFailureBadCredentialsPostHttps() throws Exception
    {
        doPost(null, "localhost", "anonX", "anonX", "https://localhost:" + dynamicPort2.getNumber() + "/services/Echo", true, 401);
    }

    @Test
    public void testAuthenticationAuthorisedGet() throws Exception
    {
        doGet(null, "localhost", "anon", "anon", "http://localhost:" + dynamicPort1.getNumber() + "/services/Echo/echo/echo/hello", false, 200);
    }

    @Test
    public void testAuthenticationAuthorisedGetHttps() throws Exception
    {
        doGet(null, "localhost", "anon", "anon", "https://localhost:" + dynamicPort2.getNumber() + "/services/Echo/echo/echo/hello", false, 200);
    }

    @Test
    public void testAuthenticationAuthorisedPost() throws Exception
    {
        doPost(null, "localhost", "anon", "anon", "http://localhost:" + dynamicPort1.getNumber() + "/services/Echo", false, 200);
    }

    @Test
    public void testAuthenticationAuthorisedPostHttps() throws Exception
    {
        doPost(null, "localhost", "anon", "anon", "https://localhost:" + dynamicPort2.getNumber() + "/services/Echo", false, 200);
    }

    @Test
    public void testAuthenticationAuthorisedWithHandshakeGet() throws Exception
    {
         doGet(null, "localhost", "anon", "anon", "http://localhost:" + dynamicPort1.getNumber() + "/services/Echo/echo/echo/hello", true, 200);
    }

    @Test
    public void testAuthenticationAuthorisedWithHandshakePost() throws Exception
    {
        doPost(null, "localhost", "anon", "anon", "http://localhost:" + dynamicPort1.getNumber() + "/services/Echo", true, 200);
    }

    // TODO Realm validation seems to be completely ignored
    @Ignore
    @Test
    public void testAuthenticationAuthorisedWithHandshakeAndBadRealmGet() throws Exception
    {
        doGet("blah", "localhost", "anon", "anon", "http://localhost:" + dynamicPort1.getNumber() + "/services/Echo/echo/echo/hello", true, 401);
    }

    // TODO Realm validation seems to be completely ignored
    @Ignore
    @Test
    public void testAuthenticationAuthorisedWithHandshakeAndBadRealmPost() throws Exception
    {
        doPost("blah", "localhost", "anon", "anon", "http://localhost:" + dynamicPort1.getNumber() + "/services/Echo", true, 401);
    }

    @Test
    public void testAuthenticationAuthorisedWithHandshakeAndRealmGet() throws Exception
    {
        doGet("mule-realm", "localhost", "ross", "ross", "http://localhost:" + dynamicPort1.getNumber() + "/services/Echo/echo/echo/hello", true, 200);
    }

    @Test
    public void testAuthenticationAuthorisedWithHandshakeAndRealmPost() throws Exception
    {
        doPost("mule-realm", "localhost", "ross", "ross", "http://localhost:" + dynamicPort1.getNumber() + "/services/Echo", true, 200);
    }

    private void doGet(String realm,
                       String host,
                       String user,
                       String pass,
                       String url,
                       boolean handshake,
                       int result) throws Exception
    {
        HttpClient client = new HttpClient();
        client.getParams().setAuthenticationPreemptive(true);
        client.getState().setCredentials(new AuthScope(host, -1, realm),
            new UsernamePasswordCredentials(user, pass));
        GetMethod get = new GetMethod(url);
        get.setDoAuthentication(handshake);

        try
        {
            int status = client.executeMethod(get);
            if (status == HttpConstants.SC_UNAUTHORIZED && handshake == true)
            {
                // doAuthentication = true means that if the request returns 401, 
                // the HttpClient will resend the request with credentials
                status = client.executeMethod(get);
            }
            assertEquals(result, status);
        }
        finally
        {
            get.releaseConnection();
        }
    }

    private void doPost(String realm,
                        String host,
                        String user,
                        String pass,
                        String url,
                        boolean handshake,
                        int result) throws Exception
    {
        HttpClient client = new HttpClient();
        client.getParams().setAuthenticationPreemptive(true);
        client.getState().setCredentials(new AuthScope(host, -1, realm),
            new UsernamePasswordCredentials(user, pass));
        PostMethod post = new PostMethod(url);
        post.setDoAuthentication(handshake);
        StringRequestEntity requestEntity = new StringRequestEntity(soapRequest, "text/xml", "UTF-8");
        post.setRequestEntity(requestEntity);
        try
        {
            int status = client.executeMethod(post);
            if (status == HttpConstants.SC_UNAUTHORIZED && handshake == true)
            {
                // doAuthentication = true means that if the request returns 401, 
                // the HttpClient will resend the request with credentials
                status = client.executeMethod(post);
            }
            assertEquals(result, status);
            assertNotNull(post.getResponseBodyAsString());
        }
        finally
        {
            post.releaseConnection();
        }
    }
}
