/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.construct;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.module.client.MuleClient;
import org.mule.tck.DynamicPortTestCase;
import org.mule.test.integration.tck.WeatherForecaster;

public class WSProxyTestCase extends DynamicPortTestCase
{
    public WSProxyTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);

    }

    private MuleClient muleClient;

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/construct/ws-proxy-config.xml";
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE,
            "org/mule/test/integration/construct/ws-proxy-config.xml"}

        });
    }

    @Override
    protected int getNumPortsToFind()
    {
        return 2;
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

    private void testWsdlAndWebServiceRequests(final int proxyId) throws Exception
    {
        testWsdlRequest(proxyId);
        testWebServiceRequest(proxyId);
    }

    private void testWsdlRequest(final int proxyId) throws Exception
    {
        final String wsdl = muleClient.request(
            "http://localhost:" + getPorts().get(0) + "/weather-forecast/" + proxyId + "?wsdl",
            getTestTimeoutSecs() * 1000L).getPayloadAsString();
        assertTrue(wsdl.contains("GetWeatherByZipCode"));
    }

    private void testWebServiceRequest(final int proxyId) throws Exception
    {
        final String weatherForecast = muleClient.send(
            "wsdl-cxf:http://localhost:" + getPorts().get(0) + "/weather-forecast/" + proxyId
                            + "?wsdl&method=GetWeatherByZipCode", "95050", null, getTestTimeoutSecs() * 1000)
            .getPayloadAsString();

        assertEquals(new WeatherForecaster().getByZipCode("95050"), weatherForecast);
    }

}
