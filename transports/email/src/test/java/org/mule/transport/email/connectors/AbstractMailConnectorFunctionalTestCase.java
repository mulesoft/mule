/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.connectors;

import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.transformer.NoActionTransformer;
import org.mule.transport.AbstractConnectorTestCase;
import org.mule.transport.email.GreenMailUtilities;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

import java.util.concurrent.atomic.AtomicInteger;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.internet.MimeMessage;

import org.junit.Rule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Start a (greenmail) mail server with a known message, for use in subclasses.
 */
public abstract class AbstractMailConnectorFunctionalTestCase extends AbstractConnectorTestCase
{

    public static final String LOCALHOST = "127.0.0.1";
    public static final String USER = "bob";
    public static final String PROVIDER = "example.com";
    public static final String EMAIL = USER + "@" + PROVIDER;
    public static final String PASSWORD = "secret";
    public static final String MESSAGE = "Test Email Message";

    public static final int START_ATTEMPTS = 3;
    public static final int TEST_ATTEMPTS = 5;
    public static final long STARTUP_PERIOD_MS = 1000;

    // for constructor
    public static final boolean SEND_INITIAL_EMAIL = true;
    public static final boolean NO_INITIAL_EMAIL = false;

    private static final AtomicInteger nameCount = new AtomicInteger(0);

    private MimeMessage message;
    private GreenMail servers;
    private final boolean initialEmail;
    private String protocol;
    private int port;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    protected AbstractMailConnectorFunctionalTestCase(boolean initialEmail, String protocol)
    {
        super();
        this.initialEmail = initialEmail;
        this.protocol = protocol;
    }

    @Override
    protected synchronized void doSetUp() throws Exception
    {
        super.doSetUp();
        //TODO(pablo.kraan): looks like port is redundant. Remove it
        this.port = dynamicPort.getNumber();
        startServers();
        muleContext.getRegistry().registerObject("noActionTransformer", new NoActionTransformer());
    }

    @Override
    protected synchronized void doTearDown() throws Exception
    {
        stopServers();
        super.doTearDown();
    }

    private synchronized void storeEmail() throws Exception
    {
        GreenMailUtilities.storeEmail(servers.getManagers().getUserManager(), EMAIL, USER,
                                      PASSWORD, (MimeMessage) getValidMessage());
        assertEquals(1, servers.getReceivedMessages().length);
    }

    private synchronized void startServers() throws Exception
    {
        servers = new GreenMail(getSetups());
        GreenMailUtilities.robustStartup(servers, LOCALHOST, port, START_ATTEMPTS, TEST_ATTEMPTS, STARTUP_PERIOD_MS);
        if (initialEmail)
        {
            storeEmail();
        }
    }

    private ServerSetup[] getSetups()
    {
        return new ServerSetup[] {new ServerSetup(port, null, protocol)};
    }

    private synchronized void stopServers() throws Exception
    {
        if (null != servers)
        {
            try
            {
                servers.stop();
            }
            catch (RuntimeException e)
            {
                e.printStackTrace();
            }
        }
    }

    protected synchronized GreenMail getServers()
    {
        return servers;
    }

    @Override
    public Object getValidMessage() throws Exception
    {
        if (null == message)
        {
            message = GreenMailUtilities.toMessage(MESSAGE, EMAIL, null);
        }
        return message;
    }

    @Override
    public String getTestEndpointURI()
    {
        String uri = protocol + "://" + USER + ":" + PASSWORD + "@" + LOCALHOST + ":" + port + "?connector="
                     + connectorName;
        if (!transformInboundMessage())
        {
            uri = uri + "&transformers=noActionTransformer";
        }
        return uri;
    }

    protected boolean transformInboundMessage()
    {
        return false;
    }

    protected void assertMessageOk(Object mailMessage) throws Exception
    {
        assertTrue("Did not receive a MimeMessage", mailMessage instanceof MimeMessage);

        MimeMessage received = (MimeMessage) mailMessage;

        // for some reason, something is adding a newline at the end of messages
        // so we need to strip that out for comparison
        assertTrue("Did not receive a message with String contents",
                   received.getContent() instanceof String);

        String receivedText = ((String) received.getContent()).trim();
        assertEquals(MESSAGE, receivedText);

        Address[] recipients = received.getRecipients(Message.RecipientType.TO);
        assertNotNull(recipients);
        assertEquals("recipients", 1, recipients.length);
        assertEquals("recipient", EMAIL, recipients[0].toString());
    }

    protected String uniqueName(String root)
    {
        return root + nameCount.incrementAndGet();
    }
}
