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

import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.test.integration.tck.WeatherForecaster;

public class WSProxyTestCase extends FunctionalTestCase
{
    private MuleClient muleClient;

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/construct/ws-proxy-config.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.setDisposeManagerPerSuite(true);
        super.doSetUp();
        muleClient = new MuleClient(muleContext);
    }

    public void testDynamicWsdl() throws Exception
    {
        testWsdlAndWebServiceRequests(0);
    }

    public void testFileContentsWsdl() throws Exception
    {
        testWsdlAndWebServiceRequests(1);
    }

    public void testStaticUriWsdl() throws Exception
    {
        testWsdlAndWebServiceRequests(2);
    }

    public void testGlobalEndpoints() throws Exception
    {
        testWsdlAndWebServiceRequests(3);
    }

    public void testTransformers() throws Exception
    {
        testWsdlAndWebServiceRequests(4);
    }

    public void testExceptionStrategy() throws Exception
    {
        testWsdlAndWebServiceRequests(5);
    }

    public void testInheritance() throws Exception
    {
        testWsdlAndWebServiceRequests(6);
    }

    public void testEndpointChildren() throws Exception
    {
        testWsdlAndWebServiceRequests(7);
    }

    public void testInheritanceAndEndpointChildren() throws Exception
    {
        testWsdlAndWebServiceRequests(8);
    }

    private void testWsdlAndWebServiceRequests(int proxyId) throws Exception
    {
        testWsdlRequest(proxyId);
        testWebServiceRequest(proxyId);
    }

    private void testWsdlRequest(int proxyId) throws Exception
    {
        final String wsdl = muleClient.request("http://localhost:8090/weather-forecast/" + proxyId + "?wsdl",
            getTestTimeoutSecs() * 1000L).getPayloadAsString();
        assertTrue(wsdl.contains("GetWeatherByZipCode"));
    }

    private void testWebServiceRequest(int proxyId) throws Exception
    {
        final String weatherForecast = muleClient.send(
            "wsdl-cxf:http://localhost:8090/weather-forecast/" + proxyId + "?wsdl&method=GetWeatherByZipCode",
            "95050", null, getTestTimeoutSecs() * 1000)
            .getPayloadAsString();

        assertEquals(new WeatherForecaster().getByZipCode("95050"), weatherForecast);
    }

}
