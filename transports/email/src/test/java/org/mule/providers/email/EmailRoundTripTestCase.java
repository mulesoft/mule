/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.email;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;

import javax.mail.internet.MimeMessage;

/**
 * This demonstrates "round trip" processing of email - an email is pulled from a POP
 * server and then sent to an SMTP server.  While within Mule the message is serialized
 * as RFC822 encoded bytes (this would let the message be transmitted over JMS etc).
 *
 * <p>The email servers for the test are managed by the greenMailSupport instance.
 * The Mule services (defined in round-trip-test.xml) are started by the test framework.
 * So all we need to do here is test that the message is handled correctly.</p>
 */
public class EmailRoundTripTestCase extends FunctionalTestCase
{

    public static final long WAIT_MS = 3000L;

    // this places the SMTP server at 62000 and POP at 62002
    private AbstractGreenMailSupport greenMailSupport = new FixedPortGreenMailSupport(62000);

    protected String getConfigResources()
    {
        return "round-trip-test.xml";
    }

    public void testRoundTrip() throws UMOException, InterruptedException
    {
        // first, check that the conversion happened - we should have a copy of
        // the message as rfc822 encoded bytes on vm://rfc822
        MuleClient client = new MuleClient();
        UMOMessage message = client.receive("vm://rfc822?connector=queue", WAIT_MS);
        assertTrue(message.getPayload() instanceof byte[]);

        // next, check that the email is received in the server
        greenMailSupport.getServers().waitForIncomingEmail(WAIT_MS, 1);
        MimeMessage[] messages = greenMailSupport.getServers().getReceivedMessages();
        assertNotNull("did not receive any messages", messages);
        assertEquals("did not receive 1 mail", 1, messages.length);
    }

    /**
     * Start the servers when the test starts
     * @throws Exception
     */
    protected void suitePreSetUp() throws Exception
    {
        greenMailSupport.startServers();
        greenMailSupport.createBobAndStoreEmail(greenMailSupport.getValidMessage(AbstractGreenMailSupport.ALICE_EMAIL));
    }

    /**
     * Stop the servers when the test ends
     * @throws Exception
     */
    protected void suitePostTearDown() throws Exception
    {
        greenMailSupport.stopServers();
    }

}
