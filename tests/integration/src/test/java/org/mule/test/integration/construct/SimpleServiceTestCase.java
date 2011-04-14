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

import java.io.InputStream;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.mule.api.MuleException;
import org.mule.api.client.LocalMuleClient;
import org.mule.construct.SimpleService;
import org.mule.tck.DynamicPortTestCase;
import org.mule.test.integration.tck.WeatherForecaster;
import org.mule.util.StringUtils;
import org.springframework.util.FileCopyUtils;

public class SimpleServiceTestCase extends DynamicPortTestCase
{
    private LocalMuleClient muleClient;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        muleClient = muleContext.getClient();
    }

    @Override
    protected int getNumPortsToFind()
    {
        return 2;
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

    public void testTransformerReferences() throws Exception
    {
        doTestStringMassager("vm://bam1.in");
    }

    public void testConcreteInheritence() throws Exception
    {
        doTestStringMassager("vm://bam2.in");
    }

    public void testComponentWithEntryPointResolver() throws Exception
    {
        doTestMathsService("vm://maths6.in");
    }

    public void testChildEndpoint() throws Exception
    {
        doTestMathsService("vm://maths7.in");
    }

    public void testInheritedExceptionStrategy() throws Exception
    {
        final String result = muleClient.send("vm://iexst.in", "ignored", null).getPayloadAsString();
        assertEquals("Ka-boom!", result);
    }

    public void testJaxWsService() throws Exception
    {
        doTestJaxWsService(1);
    }

    public void testJaxbConsumer() throws Exception
    {
        final String result = muleClient.send(
            "vm://weather-consumer.in",
            Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("org/mule/test/integration/construct/weather-report.xml"), null)
            .getPayload()
            .toString();

        assertTrue(StringUtils.isNotBlank(result));
    }

    public void testXpathConsumer() throws Exception
    {
        final String result = muleClient.send(
            "vm://weather-xpath-consumer.in",
            Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("org/mule/test/integration/construct/weather-report.xml"), null)
            .getPayload()
            .toString();

        assertTrue(StringUtils.isNotBlank(result));
    }

    public void testFunctionalTestComponent() throws Exception
    {
        doTestFunctionalTestComponent("vm://ftc1.in", "functional-test-component-1");
    }

    public void testInheritedType() throws Exception
    {
        doTestJaxWsService(2);
    }

    private void doTestFunctionalTestComponent(final String ftcUri, final String ftcName)
        throws MuleException, Exception
    {
        final String s = RandomStringUtils.randomAlphabetic(10);
        muleClient.send(ftcUri, s, null);
        assertEquals(s, getFunctionalTestComponent(ftcName).getLastReceivedMessage());
    }

    private void doTestMathsService(final String url) throws MuleException
    {
        final int a = RandomUtils.nextInt(100);
        final int b = RandomUtils.nextInt(100);
        final int result = (Integer) muleClient.send(url, new int[]{a, b}, null).getPayload();
        assertEquals(a + b, result);
    }

    private void doTestStringMassager(final String url) throws Exception, MuleException
    {
        final String s = RandomStringUtils.randomAlphabetic(10);
        final String result = new String((byte[]) muleClient.send(url, s.getBytes(), null).getPayload());
        assertEquals(s + "barbaz", result);
    }

    private void doTestJaxWsService(final int portId) throws Exception
    {
        final Integer port = getPorts().get(portId - 1);

        final String wsdl = new String(
            FileCopyUtils.copyToByteArray((InputStream) muleClient.request(
                "http://localhost:" + port + "/weather-forecast?wsdl", getTestTimeoutSecs() * 1000L)
                .getPayload()));

        assertTrue(wsdl.contains("GetWeatherByZipCode"));

        final String weatherForecast = muleClient.send(
            "wsdl-cxf:http://localhost:" + port + "/weather-forecast?wsdl&method=GetWeatherByZipCode",
            "95050", null, getTestTimeoutSecs() * 1000).getPayloadAsString();

        assertEquals(new WeatherForecaster().getByZipCode("95050"), weatherForecast);
    }

    public void testInheritedElementsUnique() throws Exception
    {
        final SimpleService child1 = (SimpleService) getFlowConstruct("child-service-1");
        final SimpleService child2 = (SimpleService) getFlowConstruct("child-service-2");
        assertNotSame(child1.getMessageSource(), child2.getMessageSource());
        assertNotSame(child1.getComponent(), child2.getComponent());
        assertNotSame(child1.getExceptionListener(), child2.getExceptionListener());
    }

}
