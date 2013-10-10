/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.construct;

import org.mule.api.MuleException;
import org.mule.api.client.LocalMuleClient;
import org.mule.construct.SimpleService;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.test.integration.tck.WeatherForecaster;
import org.mule.util.StringUtils;

import java.io.InputStream;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;
import org.springframework.util.FileCopyUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class SimpleServiceTestCase extends FunctionalTestCase
{
    private LocalMuleClient muleClient;

    public SimpleServiceTestCase()
    {
        setDisposeContextPerClass(true);
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

    @Test
    public void testPureAttributes() throws Exception
    {
        doTestMathsService("vm://maths1.in");
    }

    @Test
    public void testAbstractInheritence() throws Exception
    {
        doTestMathsService("vm://maths2.in");
    }

    @Test
    public void testEndpointReference() throws Exception
    {
        doTestMathsService("vm://maths3.in");
    }

    @Test
    public void testComponentReference() throws Exception
    {
        doTestMathsService("vm://maths4.in");
    }

    @Test
    public void testChildComponent() throws Exception
    {
        doTestMathsService("vm://maths5.in");
    }

    @Test
    public void testTransformerReferences() throws Exception
    {
        doTestStringMassager("vm://bam1.in");
    }

    @Test
    public void testConcreteInheritence() throws Exception
    {
        doTestStringMassager("vm://bam2.in");
    }

    @Test
    public void testComponentWithEntryPointResolver() throws Exception
    {
        doTestMathsService("vm://maths6.in");
    }

    @Test
    public void testChildEndpoint() throws Exception
    {
        doTestMathsService("vm://maths7.in");
    }

    @Test
    public void testInheritedExceptionStrategy() throws Exception
    {
        final String result = muleClient.send("vm://iexst.in", "ignored", null).getPayloadAsString();
        assertEquals("Ka-boom!", result);
    }

    @Test
    public void testJaxWsService() throws Exception
    {
        doTestJaxWsService(6099);
    }

    @Test
    public void testJaxbConsumer() throws Exception
    {
        final String result = muleClient.send(
            "vm://weather-consumer.in",
            Thread.currentThread().getContextClassLoader().getResourceAsStream(
                "org/mule/test/integration/construct/weather-report.xml"), null).getPayload().toString();

        assertTrue(StringUtils.isNotBlank(result));
    }

    @Test
    public void testXpathConsumer() throws Exception
    {
        final String result = muleClient.send(
            "vm://weather-xpath-consumer.in",
            Thread.currentThread().getContextClassLoader().getResourceAsStream(
                "org/mule/test/integration/construct/weather-report.xml"), null).getPayload().toString();

        assertTrue(StringUtils.isNotBlank(result));
    }

    @Test
    public void testFunctionalTestComponent() throws Exception
    {
        doTestFunctionalTestComponent("vm://ftc1.in", "functional-test-component-1");
    }

    @Test
    public void testInheritedType() throws Exception
    {
        doTestJaxWsService(6098);
    }

    private void doTestFunctionalTestComponent(final String ftcUri, final String ftcName)
        throws MuleException, Exception
    {
        final String s = RandomStringUtils.randomAlphabetic(10);
        muleClient.send(ftcUri, s, null);
        assertEquals(s, getFunctionalTestComponent(ftcName).getLastReceivedMessage());
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
        final String result = new String((byte[]) muleClient.send(url, s.getBytes(), null).getPayload());
        assertEquals(s + "barbaz", result);
    }

    private void doTestJaxWsService(int portNumber) throws Exception
    {
        final String wsdl = new String(FileCopyUtils.copyToByteArray((InputStream) muleClient.request(
            "http://localhost:" + portNumber + "/weather-forecast?wsdl", getTestTimeoutSecs() * 1000L)
            .getPayload()));

        assertTrue(wsdl.contains("GetWeatherByZipCode"));

        final String weatherForecast = muleClient.send(
            "wsdl-cxf:http://localhost:" + portNumber + "/weather-forecast?wsdl&method=GetWeatherByZipCode",
            "95050", null, getTestTimeoutSecs() * 1000).getPayloadAsString();

        assertEquals(new WeatherForecaster().getByZipCode("95050"), weatherForecast);
    }
    
    @Test
    public void testInheritedElementsUnique() throws Exception
    {
        SimpleService child1 = (SimpleService) getFlowConstruct("child-service-1");
        SimpleService child2 = (SimpleService) getFlowConstruct("child-service-2");
        assertNotSame(child1.getMessageSource(), child2.getMessageSource());
        assertNotSame(child1.getComponent(), child2.getComponent());
        assertNotSame(child1.getExceptionListener(), child2.getExceptionListener());
    }

}
