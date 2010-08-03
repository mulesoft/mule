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

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.NullPayload;

public class BridgeTestCase extends FunctionalTestCase
{

    private MuleClient muleClient;

    @Override
    protected void doSetUp() throws Exception
    {
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

    // TODO (DDO) add inheritance test

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
