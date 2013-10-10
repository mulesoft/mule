/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.construct;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.integration.tck.WeatherForecaster;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class WSProxyTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    @Rule
    public DynamicPort port2 = new DynamicPort("port2");

    @Rule
    public DynamicPort port3 = new DynamicPort("port3");

    private MuleClient muleClient;

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/construct/ws-proxy-config.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        muleClient = new MuleClient(muleContext);
    }

    @Test
    public void testDynamicWsdl() throws Exception
    {
        testWsdlAndWebServiceRequests(0);
    }

    @Test
    public void testFileContentsWsdl() throws Exception
    {
        testWsdlAndWebServiceRequests(1);
    }

    @Test
    public void testStaticUriWsdl() throws Exception
    {
        testWsdlAndWebServiceRequests(2);
    }

    @Test
    public void testGlobalEndpoints() throws Exception
    {
        testWsdlAndWebServiceRequests(3);
    }

    @Test
    public void testTransformers() throws Exception
    {
        testWsdlAndWebServiceRequests(4);
    }

    @Test
    public void testExceptionStrategy() throws Exception
    {
        testWsdlAndWebServiceRequests(5);
    }

    @Test
    public void testInheritance() throws Exception
    {
        testWsdlAndWebServiceRequests(6);
    }

    @Test
    public void testEndpointChildren() throws Exception
    {
        testWsdlAndWebServiceRequests(7);
    }

    @Test
    public void testInheritanceAndEndpointChildren() throws Exception
    {
        testWsdlAndWebServiceRequests(8);
    }

    @Test
    public void testExpressionEndpoint() throws Exception
    {
        testWsdlAndWebServiceRequests(9);
    }

    @Test
    public void testResponsePropertiesPropagation() throws Exception
    {
        MuleMessage reply = performWebServiceRequest(10);
        // Test if Content-Encoding is present as an inbound property because HTTP response headers are translated
        // into inbound properties when the response is transformed into the MuleMessage.
        assertNotNull(reply.getInboundProperty("Content-Encoding"));
        assertEquals(reply.getInboundProperty("Content-Encoding"), "gzip");
    }

    private void testWsdlAndWebServiceRequests(int proxyId) throws Exception
    {
        testWsdlRequest(proxyId);
        testWebServiceRequest(proxyId);
    }

    private void testWsdlRequest(int proxyId) throws Exception
    {
        final String wsdl = muleClient.request(
                "http://localhost:" + port1.getNumber() + "/weather-forecast/" + proxyId + "?wsdl",
                getTestTimeoutSecs() * 1000L).getPayloadAsString();
        assertTrue(wsdl.contains("GetWeatherByZipCode"));
    }

    private void testWebServiceRequest(int proxyId) throws Exception
    {
        final String weatherForecast = performWebServiceRequest(proxyId).getPayloadAsString();
        assertEquals(new WeatherForecaster().getByZipCode("95050"), weatherForecast);
    }

    /**
     * Performs a request to the web-service.
     * @param proxyId The proxy id.
     * @return The {@link org.mule.api.MuleMessage} with the web-service response.
     * @throws org.mule.api.MuleException If there is a problem with the request.
     */
    private MuleMessage performWebServiceRequest(final int proxyId) throws MuleException
    {
        return muleClient.send("wsdl-cxf:http://localhost:" + port1.getNumber() + "/weather-forecast/" + proxyId
                               + "?wsdl&method=GetWeatherByZipCode", "95050", null, getTestTimeoutSecs() * 1000);
    }
}
