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

import org.apache.commons.lang.math.RandomUtils;
import org.mule.api.MuleException;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.FunctionalTestComponent;

public class ValidatorTestCase extends FunctionalTestCase
{
    private MuleClient muleClient;

    @Override
    protected void doSetUp() throws Exception
    {
        super.setDisposeManagerPerSuite(true);
        super.doSetUp();
        muleClient = new MuleClient(muleContext);
        // System.err.println(((SimpleService)
        // muleContext.getRegistry().lookupObject("test-service")).getMessageSource());
    }

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/construct/validator-config.xml";
    }

    public void testChildFilter() throws Exception
    {
        doTestValidator("validator");
    }

    public void testFilterAndEndpointReferences() throws Exception
    {
        doTestValidator("validator-with-refs");
    }

    public void testChildEndpoints() throws Exception
    {
        doTestValidator("validator-with-child-endpoints");
    }

    public void testExceptionStrategy() throws Exception
    {
        doTestValidMessage("validator-with-exception-strategy");

        // ensure the exception strategy kicked in
        assertEquals("Ka-boom!", muleClient.send("vm://validator-with-exception-strategy.in", "abc", null)
            .getPayload());
    }

    public void testInheritance() throws Exception
    {
        doTestValidator("concrete-validator");
    }

    private void doTestValidator(String serviceName) throws Exception
    {
        doTestValidMessage(serviceName);
        doTestInvalidMessage(serviceName);
    }

    private void doTestValidMessage(String serviceName) throws MuleException, Exception, InterruptedException
    {
        final Integer payload = RandomUtils.nextInt();

        assertEquals("GOOD:" + payload + "@" + serviceName, muleClient.send("vm://" + serviceName + ".in",
            payload, null).getPayload());

        int i = 0;
        final FunctionalTestComponent ftc = getFunctionalTestComponent("test-service");
        while (ftc.getReceivedMessagesCount() == 0 && i++ < 10)
        {
            Thread.sleep(250L);
            System.err.print(".");
        }

        assertEquals(1, ftc.getReceivedMessagesCount());
        assertEquals(payload, ftc.getLastReceivedMessage());
        ftc.initialise();
    }

    private void doTestInvalidMessage(String serviceName) throws MuleException
    {
        assertEquals("BAD:abc@" + serviceName, muleClient.send("vm://" + serviceName + ".in", "abc", null)
            .getPayload());
    }
}
