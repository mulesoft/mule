/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.email.functional.AbstractEmailFunctionalTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.mail.internet.MimeMessage;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/**
 * This demonstrates "round trip" processing of email - an email is pulled from a POP
 * server and then sent to an SMTP server.  While within Mule the message is serialized
 * as RFC822 encoded bytes (this would let the message be transmitted over JMS etc).
 *
 * <p>The email servers for the test are managed by the greenMailSupport instance.
 * The Mule services (defined in email-round-trip-test.xml) are started by the test framework.
 * So all we need to do here is test that the message is handled correctly.</p>
 */
public class EmailRoundTripTestCase extends AbstractServiceAndFlowTestCase
{
    public EmailRoundTripTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    private AbstractGreenMailSupport greenMailSupport = null;

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    @Rule
    public DynamicPort dynamicPort3 = new DynamicPort("port3");

    @Rule
    public DynamicPort dynamicPort4 = new DynamicPort("port4");

    @Rule
    public DynamicPort dynamicPort5 = new DynamicPort("port5");

    @Rule
    public DynamicPort dynamicPort6 = new DynamicPort("port6");

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "email-round-trip-test-service.xml"},
            {ConfigVariant.FLOW, "email-round-trip-test-flow.xml"}
        });
    }

    @Override
    protected MuleContext createMuleContext() throws Exception
    {
        startServer();
        return super.createMuleContext();
    }

    /**
     * Start the servers when the test starts
     * @throws Exception
     */
    public void startServer() throws Exception
    {
        // see AbstractGreenMailSupport for all the ports this test uses and their order
        //portsForClass = PortUtil.findFreePorts(6);
        greenMailSupport = new FixedPortGreenMailSupport(dynamicPort2.getNumber());

        List<Integer> ports = new ArrayList<Integer>(6);
        ports.add(dynamicPort1.getNumber());
        ports.add(dynamicPort2.getNumber());
        ports.add(dynamicPort3.getNumber());
        ports.add(dynamicPort4.getNumber());
        ports.add(dynamicPort5.getNumber());
        ports.add(dynamicPort6.getNumber());

        greenMailSupport.startServers(ports);
        greenMailSupport.createBobAndStoreEmail(greenMailSupport.getValidMessage(AbstractGreenMailSupport.ALICE_EMAIL));
    }

    /**
     * Stop the servers when the test ends
     * @throws Exception
     */
    @Override
    protected void doTearDown() throws Exception
    {
        greenMailSupport.stopServers();
        super.doTearDown();
    }

    @Test
    public void testRoundTrip() throws MuleException, InterruptedException
    {
        // first, check that the conversion happened - we should have a copy of
        // the message as rfc822 encoded bytes on vm://rfc822
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.request("vm://rfc822", RECEIVE_TIMEOUT);
        assertTrue(message.getPayload() instanceof byte[]);

        // next, check that the email is received in the server
        greenMailSupport.getServers().waitForIncomingEmail(AbstractEmailFunctionalTestCase.DELIVERY_DELAY_MS, 1);
        MimeMessage[] messages = greenMailSupport.getServers().getReceivedMessages();
        assertNotNull("did not receive any messages", messages);
        assertEquals("did not receive 1 mail", 1, messages.length);
    }
}
