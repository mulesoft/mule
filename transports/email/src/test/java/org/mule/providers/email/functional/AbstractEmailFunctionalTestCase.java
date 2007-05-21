/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.email.functional;

import org.mule.tck.FunctionalTestCase;
import org.mule.extras.client.MuleClient;
import org.mule.providers.email.GreenMailUtilities;
import org.mule.umo.UMOMessage;

import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.Servers;

import java.io.IOException;

import javax.mail.internet.MimeMessage;
import javax.mail.Message;
import javax.mail.MessagingException;

public abstract class AbstractEmailFunctionalTestCase extends FunctionalTestCase
{

    protected static final String CONFIG_BASE = "-functional-test.xml";
    protected static final long DELIVERY_DELAY_MS = 1000l;
    protected static final boolean MIME_MESSAGE = true;
    protected static final boolean STRING_MESSAGE = false;

    protected static final int DEFAULT_PORT = 65432;
    protected static final String DEFAULT_EMAIL = "bob@example.com";
    protected static final String DEFAULT_USER = "bob";
    protected static final String DEFAULT_MESSAGE = "Test email message";
    protected static final String DEFAULT_PASSWORD = "password";

    private String protocol;
    private boolean isMimeMessage;
    private int port;
    private String configFile;
    private Servers server;
    private String email;
    private String user;
    private String message;
    private String password;

    protected AbstractEmailFunctionalTestCase(boolean isMimeMessage, String protocol)
    {
        this(isMimeMessage, protocol, protocol + CONFIG_BASE);
    }

    protected AbstractEmailFunctionalTestCase(boolean isMimeMessage, String protocol, String configFile)
    {
        this(isMimeMessage, protocol, configFile, DEFAULT_PORT, 
                DEFAULT_EMAIL, DEFAULT_USER, DEFAULT_MESSAGE, DEFAULT_PASSWORD);
    }

    protected AbstractEmailFunctionalTestCase(boolean isMimeMessage, String protocol, String configFile, int port,
                                              String email, String user, String message,
                                              String password)
    {
        this.isMimeMessage = isMimeMessage;
        this.protocol = protocol;
        this.port = port;
        this.configFile = configFile;
        this.email = email;
        this.user = user;
        this.message = message;
        this.password = password;
    }

    protected String getConfigResources()
    {
        return configFile;
    }

    // @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        startServer();
    }

    // @Override
    protected void doTearDown() throws Exception
    {
        stopServer();
        super.doTearDown();
    }

    protected void doSend() throws Exception
    {
        Object msg;
        if (isMimeMessage)
        {
            msg = GreenMailUtilities.toMessage(message, email);
        }
        else
        {
            msg = message;
        }

        MuleClient client = new MuleClient();
        client.send("vm://send", msg, null);

        server.waitForIncomingEmail(DELIVERY_DELAY_MS, 1);

        MimeMessage[] messages = server.getReceivedMessages();
        assertNotNull("did not receive any messages", messages);
        assertEquals("did not receive 1 mail", 1, messages.length);
        verifyMessage(messages[0]);
    }

    protected void verifyMessage(MimeMessage received) throws IOException, MessagingException
    {
        assertTrue("Did not receive a message with String contents",
            received.getContent() instanceof String);
        // for some reason, something is adding a newline at the end of messages
        // so we need to strip that out for comparison
        String receivedText = ((String) received.getContent()).trim();
        assertEquals(message, receivedText);
        assertNotNull(received.getRecipients(Message.RecipientType.TO));
        assertEquals(1, received.getRecipients(Message.RecipientType.TO).length);
        assertEquals(received.getRecipients(Message.RecipientType.TO)[0].toString(), email);
    }

    protected void doReceive() throws Exception
    {
        GreenMailUtilities.storeEmail(server.getManagers().getUserManager(),
                email, user, password,
                GreenMailUtilities.toMessage(message, email));
        assertEquals(1, server.getReceivedMessages().length);

        MuleClient client = new MuleClient();
        UMOMessage message = client.receive("vm://receive", 1000);
        
        assertNotNull(message);
        Object payload = message.getPayload();
        assertTrue(payload instanceof MimeMessage);
        verifyMessage((MimeMessage) payload);
    }

    private void startServer()
    {
        ServerSetup setup = new ServerSetup(port, null, protocol);
        server = new Servers(setup);
        server.start();
    }

    private void stopServer()
    {
        server.stop();
    }

}
