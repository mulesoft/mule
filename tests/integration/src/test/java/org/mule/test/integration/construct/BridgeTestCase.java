/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.construct;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.NullPayload;
import org.mule.util.concurrent.Latch;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BridgeTestCase extends FunctionalTestCase
{
    private MuleClient muleClient;

    public BridgeTestCase()
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
        return "org/mule/test/integration/construct/bridge-config.xml";
    }

    @Test
    public void testSynchronous() throws Exception
    {
        doTestMathsService("vm://synchronous-bridge.in");
    }

    @Test
    public void testAsynchronous() throws Exception
    {
        final MuleMessage result = muleClient.send("vm://asynchronous-bridge.in", "foobar", null);
        assertEquals(NullPayload.getInstance(), result.getPayload());
    }

    @Test
    public void testTransformers() throws Exception
    {
        doTestStringMassager("vm://transforming-bridge.in");
    }

    @Test
    public void testEndpointReferences() throws Exception
    {
        doTestMathsService("vm://endpoint-ref-bridge.in");
    }

    @Test
    public void testChildEndpoints() throws Exception
    {
        doTestMathsService("vm://child-endpoint-bridge.in");
    }

    @Test
    public void testExceptionHandler() throws Exception
    {
        doTestMathsService("vm://exception-bridge.in");
    }

    @Test
    public void testVmTransacted() throws Exception
    {
        doTestMathsService("vm://transacted-bridge.in");
    }

    @Test
    public void testInheritance() throws Exception
    {
        doTestMathsService("vm://concrete-child-bridge.in");
    }

    @Test
    public void testHeterogeneousTransports() throws Exception
    {
        doJmsBasedTest("jms://myDlq", "dlq-file-picker");
    }

    @Test
    public void testJmsTransactions() throws Exception
    {
        doJmsBasedTest("jms://myQueue", "topic-listener");
    }

    @Test
    public void testDynamicEndpoint() throws Exception
    {
        doTestMathsService("vm://child-dynamic-endpoint-bridge.in", Collections.singletonMap("bridgeTarget", "maths-service.in"));
    }

    @Test
    public void testDynamicAddress() throws Exception
    {
        doTestMathsService("vm://address-dynamic-endpoint-bridge.in", Collections.singletonMap("bridgeTarget", "maths-service.in"));
    }

    private void doJmsBasedTest(final String jmsDestinationUri, final String ftcName) throws Exception, MuleException, InterruptedException
    {
        final FunctionalTestComponent ftc = getFunctionalTestComponent(ftcName);
        final Latch latch = new Latch();
        ftc.setEventCallback(new EventCallback()
        {
            @Override
            public void eventReceived(final MuleEventContext context, final Object component) throws Exception
            {
                latch.countDown();
            }
        });

        final String payload = RandomStringUtils.randomAlphabetic(10);
        muleClient.dispatch(jmsDestinationUri, payload, null);
        latch.await(getTestTimeoutSecs(), TimeUnit.SECONDS);
        assertEquals(1, ftc.getReceivedMessagesCount());
        assertEquals(payload, byteArrayOrStringtoString(ftc.getReceivedMessage(1)));
    }

    private void doTestMathsService(final String url) throws MuleException
    {
        doTestMathsService(url, null);
    }

    private void doTestMathsService(final String url, final Map<?, ?> messageProperties) throws MuleException
    {
        final int a = RandomUtils.nextInt(100);
        final int b = RandomUtils.nextInt(100);
        final int result = (Integer) muleClient.send(url, new int[] { a, b }, messageProperties).getPayload();
        assertEquals(a + b, result);
    }

    private void doTestStringMassager(final String url) throws Exception, MuleException
    {
        final String payload = RandomStringUtils.randomAlphabetic(10);
        final String result = muleClient.send(url, payload.getBytes(), null).getPayloadAsString();
        assertEquals(payload + "barbaz", result);
    }

    private String byteArrayOrStringtoString(final Object o)
    {
        if (o instanceof String)
        {
            return (String) o;
        }

        return new String((byte[]) o);
    }
}
