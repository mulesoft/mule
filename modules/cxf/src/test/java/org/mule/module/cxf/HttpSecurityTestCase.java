/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeThat;
import static org.mule.api.security.tls.TlsConfiguration.DISABLE_SYSTEM_PROPERTIES_MAPPING_PROPERTY;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.transport.http.HttpConnector;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;


public class HttpSecurityTestCase extends AbstractHttpSecurityTestCase
{

    @Rule
    public SystemProperty disablePropertiesMapping = new SystemProperty(DISABLE_SYSTEM_PROPERTIES_MAPPING_PROPERTY, "false");

    private static final String HTTP_SECURITY_CONF_FLOW_HTTPN_XML = "http-security-conf-flow-httpn.xml";

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

    public HttpSecurityTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "http-security-conf-service.xml"},
            {ConfigVariant.FLOW, "http-security-conf-flow.xml"},
            {ConfigVariant.FLOW, HTTP_SECURITY_CONF_FLOW_HTTPN_XML}
        });
    }

    /**
     * This test doesn't work in Maven because Mule can't load the keystores from the jars
     * @throws Exception
     */
    @Test
    public void testBasicAuth() throws Exception
    {
        HttpClient client = new HttpClient();
        Credentials credentials = new UsernamePasswordCredentials("admin", "admin");
        client.getState().setCredentials(AuthScope.ANY, credentials);
        client.getParams().setAuthenticationPreemptive(true);

        PostMethod method = new PostMethod("https://localhost:" + dynamicPort2.getNumber() + "/services/Echo");
        method.setDoAuthentication(true);
        StringRequestEntity requestEntity = new StringRequestEntity(soapRequest, "text/plain", "UTF-8");
        method.setRequestEntity(requestEntity);

        int result = client.executeMethod(method);

        assertEquals(200, result);

        credentials = new UsernamePasswordCredentials("admin", "adminasd");
        client.getState().setCredentials(AuthScope.ANY, credentials);
        client.getParams().setAuthenticationPreemptive(true);

        result = client.executeMethod(method);
        assertEquals(401, result);
    }

    @Test
    public void testBasicAuthWithCxfClient() throws Exception
    {
        assumeThat(configResources, is(not(equalTo(HTTP_SECURITY_CONF_FLOW_HTTPN_XML)))); // New http module doesn't support urls like "cxf:*"

        MuleClient client = muleContext.getClient();

        MuleMessage result = client.send("cxf:http://admin:admin@localhost:" + dynamicPort1.getNumber() + "/services/Echo?method=echo", getTestMuleMessage("Hello"));

        final int status = result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0);
        assertEquals(200, status);
    }
}
