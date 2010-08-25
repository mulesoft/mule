/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.email;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.email.functional.AbstractEmailFunctionalTestCase;

import javax.mail.internet.MimeMessage;

import org.junit.Test;

/**
 * This demonstrates "round trip" processing of email - an email is pulled from a POP
 * server and then sent to an SMTP server.  While within Mule the message is serialized
 * as RFC822 encoded bytes (this would let the message be transmitted over JMS etc).
 *
 * <p>The email servers for the test are managed by the greenMailSupport instance.
 * The Mule services (defined in email-round-trip-test.xml) are started by the test framework.
 * So all we need to do here is test that the message is handled correctly.</p>
 */
public class EmailRoundTripTestCase extends FunctionalTestCase
{
    // this places the SMTP server at 62000 and POP at 62002
    private AbstractGreenMailSupport greenMailSupport = new FixedPortGreenMailSupport(62000);

    protected String getConfigResources()
    {
        return "email-round-trip-test.xml";
    }

    /**
     * Start the servers when the test starts
     * @throws Exception
     */
    @Override
    protected void suitePreSetUp() throws Exception
    {
        greenMailSupport.startServers();
        greenMailSupport.createBobAndStoreEmail(greenMailSupport.getValidMessage(AbstractGreenMailSupport.ALICE_EMAIL));
    }

    /**
     * Stop the servers when the test ends
     * @throws Exception
     */
    @Override
    protected void suitePostTearDown() throws Exception
    {
        greenMailSupport.stopServers();
    }

    @Test
    public void testRoundTrip() throws MuleException, InterruptedException
    {
        // first, check that the conversion happened - we should have a copy of
        // the message as rfc822 encoded bytes on vm://rfc822
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.request("vm://rfc822", RECEIVE_TIMEOUT);
        assertTrue(message.getPayload() instanceof byte[]);

        // next, check that the email is received in the server
        greenMailSupport.getServers().waitForIncomingEmail(AbstractEmailFunctionalTestCase.DELIVERY_DELAY_MS, 1);
        MimeMessage[] messages = greenMailSupport.getServers().getReceivedMessages();
        assertNotNull("did not receive any messages", messages);
        assertEquals("did not receive 1 mail", 1, messages.length);
    }
}
