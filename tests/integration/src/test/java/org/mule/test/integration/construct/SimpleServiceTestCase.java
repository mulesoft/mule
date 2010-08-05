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

import org.mule.api.MuleException;
import org.mule.api.client.LocalMuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.test.integration.tck.WeatherForecaster;
import org.mule.util.StringUtils;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;

public class SimpleServiceTestCase extends FunctionalTestCase
{
    private LocalMuleClient muleClient;

    public SimpleServiceTestCase()
    {
        setDisposeManagerPerSuite(true);
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        muleClient = muleContext.getClient();
    }

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/construct/simple-service-config.xml";
    }

    public void testPureAttributes() throws Exception
    {
        doTestMathsService("vm://maths1.in");
    }

    public void testAbstractInheritence() throws Exception
    {
        doTestMathsService("vm://maths2.in");
    }

    public void testEndpointReference() throws Exception
    {
        doTestMathsService("vm://maths3.in");
    }

    public void testComponentReference() throws Exception
    {
        doTestMathsService("vm://maths4.in");
    }

    public void testChildComponent() throws Exception
    {
        doTestMathsService("vm://maths5.in");
    }

    public void testComponentWithEntryPointResolver() throws Exception
    {
        doTestMathsService("vm://maths6.in");
    }

    public void testChildEndpoint() throws Exception
    {
        doTestMathsService("vm://maths7.in");
    }

    public void testTransformerReferences() throws Exception
    {
        doTestStringMassager("vm://bam1.in");
    }

    public void testConcreteInheritence() throws Exception
    {
        doTestStringMassager("vm://bam2.in");
    }

    public void testJaxRsService() throws Exception
    {
        final String wsdl = muleClient.request("http://localhost:6099/weather-forecast?wsdl",
            getTestTimeoutSecs() * 1000L).getPayloadAsString();

        assertTrue(wsdl.contains("GetWeatherByZipCode"));

        final String weatherForecast = muleClient.send(
            "wsdl-cxf:http://localhost:6099/weather-forecast?wsdl&method=GetWeatherByZipCode", "95050", null,
            getTestTimeoutSecs() * 1000).getPayloadAsString();

        assertEquals(new WeatherForecaster().getByZipCode("95050"), weatherForecast);
    }

    public void testJaxbConsumer() throws Exception
    {
        final String result = muleClient.send(
            "vm://weather-consumer.in",
            Thread.currentThread().getContextClassLoader().getResourceAsStream(
                "org/mule/test/integration/construct/weather-report.xml"), null).getPayloadAsString();

        assertTrue(StringUtils.isNotBlank(result));
    }

    public void testXpathConsumer() throws Exception
    {
        final String result = muleClient.send(
            "vm://weather-xpath-consumer.in",
            Thread.currentThread().getContextClassLoader().getResourceAsStream(
                "org/mule/test/integration/construct/weather-report.xml"), null).getPayloadAsString();

        assertTrue(StringUtils.isNotBlank(result));
    }

    private void doTestMathsService(String url) throws MuleException
    {
        final int a = RandomUtils.nextInt(100);
        final int b = RandomUtils.nextInt(100);
        final int result = (Integer) muleClient.send(url, new int[]{a, b}, null).getPayload();
        assertEquals(a + b, result);
    }

    private void doTestStringMassager(String url) throws Exception, MuleException
    {
        final String s = RandomStringUtils.randomAlphabetic(10);
        final String result = muleClient.send(url, s.getBytes(), null).getPayloadAsString();
        assertEquals(s + "barbaz", result);
    }
}
