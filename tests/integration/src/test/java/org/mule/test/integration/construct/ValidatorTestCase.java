/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.construct;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.module.client.MuleClient;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.concurrent.Latch;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ValidatorTestCase extends FunctionalTestCase
{
    private MuleClient muleClient;

    public ValidatorTestCase()
    {
        setDisposeContextPerClass(true);
    }
    
    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        muleClient = new MuleClient(muleContext);
    }

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/construct/validator-config.xml";
    }

    @Test
    public void testChildFilter() throws Exception
    {
        doTestValidator("validator");
    }

    @Test
    public void testFilterAndEndpointReferences() throws Exception
    {
        doTestValidator("validator-with-refs");
    }

    @Test
    public void testChildEndpoints() throws Exception
    {
        doTestValidator("validator-with-child-endpoints");
    }

    @Test
    public void testExceptionStrategy() throws Exception
    {
        doTestValidMessage("validator-with-exception-strategy");

        // ensure the exception strategy kicked in
        assertEquals("Ka-boom!", muleClient.send("vm://validator-with-exception-strategy.in", "abc", null)
            .getPayload());
    }

    @Test
    public void testInheritance() throws Exception
    {
        doTestValidator("concrete-validator");
    }

    @Test
    public void testDispatchError() throws Exception
    {
        doTestValidMessageError("dispatch-error");
    }

    private void doTestValidator(String serviceName) throws Exception
    {
        doTestValidMessage(serviceName);
        doTestInvalidMessageNack(serviceName);
    }

    private void doTestValidMessage(String serviceName) throws MuleException, Exception, InterruptedException
    {
        final FunctionalTestComponent ftc = getFunctionalTestComponent("test-service");
        final Latch latch = new Latch();
        ftc.setEventCallback(new EventCallback()
        {
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                latch.countDown();
            }
        });

        final Object validPayload = doTestValidMessageAck(serviceName);

        latch.await(getTestTimeoutSecs(), TimeUnit.SECONDS);
        assertEquals(1, ftc.getReceivedMessagesCount());
        assertEquals(validPayload, ftc.getLastReceivedMessage());
        ftc.initialise();
    }

    private Object doTestValidMessageAck(String serviceName) throws MuleException
    {
        final Integer payload = RandomUtils.nextInt();
        assertEquals("GOOD:" + payload + "@" + serviceName, muleClient.send("vm://" + serviceName + ".in",
            payload, null).getPayload());
        return payload;
    }

    private Object doTestValidMessageError(String serviceName) throws MuleException
    {
        final Integer payload = RandomUtils.nextInt();
        assertEquals("ERROR:" + payload + "@" + serviceName, muleClient.send("vm://" + serviceName + ".in",
            payload, null).getPayload());
        return payload;
    }

    private void doTestInvalidMessageNack(String serviceName) throws MuleException
    {
        assertEquals("BAD:abc@" + serviceName, muleClient.send("vm://" + serviceName + ".in", "abc", null)
            .getPayload());
    }
}
