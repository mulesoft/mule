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

import java.util.HashMap;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.transport.NullPayload;
import org.mule.util.concurrent.Latch;

import edu.emory.mathcs.backport.java.util.Collections;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class BridgeTestCase extends FunctionalTestCase
{

    private MuleClient muleClient;

    @Override
    protected void doSetUp() throws Exception
    {
        super.setDisposeManagerPerSuite(true);
        super.doSetUp();
        muleClient = new MuleClient(muleContext);
    }

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/construct/bridge-config.xml";
    }

    public void testSynchronousBridge() throws Exception
    {
        doTestMathsService("vm://synchronous-bridge.in");
    }

    public void testAsynchronousBridge() throws Exception
    {
        final MuleMessage result = muleClient.send("vm://asynchronous-bridge.in", "foobar", null);
        assertEquals(NullPayload.getInstance(), result.getPayload());
    }

    public void testBridgeWithTransformers() throws Exception
    {
        doTestStringMassager("vm://transforming-bridge.in");
    }

    public void testBridgeWithEndpointReferences() throws Exception
    {
        doTestMathsService("vm://endpoint-ref-bridge.in");
    }

    public void testBridgeWithChildEndpoints() throws Exception
    {
        doTestMathsService("vm://child-endpoint-bridge.in");
    }

    public void testBridgeWithExceptionHandler() throws Exception
    {
        doTestMathsService("vm://exception-bridge.in");
    }

    public void testTransactedBridge() throws Exception
    {
        doTestMathsService("vm://transacted-bridge.in");
    }

    public void testInheritance() throws Exception
    {
        doTestMathsService("vm://concrete-child-bridge.in");
    }

    @SuppressWarnings("unchecked")
    public void testHeterogeneousTransports() throws Exception
    {
        final FunctionalTestComponent ftc = getFunctionalTestComponent("dlg-file-picker");
        final Latch latch = new Latch();
        ftc.setEventCallback(new EventCallback()
        {
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                latch.countDown();
            }
        });

        final String payload = RandomStringUtils.randomAlphabetic(10);
        muleClient.dispatch("jms://myDlq", payload, new HashMap<Object, Object>(Collections.singletonMap(
            "aKey", 123)));
        latch.await(getTestTimeoutSecs(), TimeUnit.SECONDS);
        assertEquals(1, ftc.getReceivedMessagesCount());
        assertEquals(payload, new String((byte[]) ftc.getReceivedMessage(1)));
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
        final String payload = RandomStringUtils.randomAlphabetic(10);
        final String result = muleClient.send(url, payload.getBytes(), null).getPayloadAsString();
        assertEquals(payload + "barbaz", result);
    }
}
