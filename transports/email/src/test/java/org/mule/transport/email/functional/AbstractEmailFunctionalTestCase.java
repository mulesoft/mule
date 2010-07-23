/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.email.functional;

import org.mule.api.MuleMessage;
import org.mule.config.i18n.LocaleMessageHandler;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.email.GreenMailUtilities;
import org.mule.transport.email.ImapConnector;
import org.mule.transport.email.MailProperties;
import org.mule.transport.email.Pop3Connector;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

public abstract class AbstractEmailFunctionalTestCase extends FunctionalTestCase
{
    public static final long DELIVERY_DELAY_MS = 10000;

    protected static final String CONFIG_BASE = "-functional-test.xml";
    protected static final boolean MIME_MESSAGE = true;
    protected static final boolean STRING_MESSAGE = false;

    protected static final String DEFAULT_EMAIL = "bob@example.com";
    protected static final String DEFAULT_USER = "bob";
    protected static final String DEFAULT_MESSAGE = "Test email message";
    protected static final String DEFAULT_PASSWORD = "password";

    private String protocol;
    private boolean isMimeMessage;
    private int port;
    private String configFile;
    private GreenMail server;
    private String email;
    private String user;
    private String message;
    private String password;
    private String charset;

    protected AbstractEmailFunctionalTestCase(int port, boolean isMimeMessage, String protocol)
    {
        this(port, isMimeMessage, protocol, protocol + CONFIG_BASE, null, null);
    }

    protected AbstractEmailFunctionalTestCase(int port, boolean isMimeMessage, String protocol, Locale locale, String charset)
    {
        this(port, isMimeMessage, protocol, protocol + CONFIG_BASE, locale, charset);
    }

    protected AbstractEmailFunctionalTestCase(int port, boolean isMimeMessage, String protocol, String configFile)
    {
        this(port, isMimeMessage, protocol, configFile, null, null);
    }

    protected AbstractEmailFunctionalTestCase(int port, boolean isMimeMessage, String protocol, String configFile, Locale locale, String charset)
    {
        this(port, isMimeMessage, protocol, configFile,
                DEFAULT_EMAIL, DEFAULT_USER, (locale == null ? DEFAULT_MESSAGE : getMessage(locale)), DEFAULT_PASSWORD, charset);
    }

    protected AbstractEmailFunctionalTestCase(int port, boolean isMimeMessage, String protocol, 
        String configFile, String email, String user, String message, String password, String charset)
    {
        this.isMimeMessage = isMimeMessage;
        this.protocol = protocol;
        this.port = port;
        this.configFile = configFile;
        this.email = email;
        this.user = user;
        this.message = message;
        this.password = password;
        this.charset = charset;
    }

    @Override
    protected String getConfigResources()
    {
        return configFile;
    }

    @Override
    protected void suitePreSetUp() throws Exception
    {
        startServer();
    }

    @Override
    protected void suitePostTearDown() throws Exception
    {
        stopServer();
    }

    protected void doSend() throws Exception
    {
        Object msg;
        if (isMimeMessage)
        {
            msg = GreenMailUtilities.toMessage(message, email, charset);
        }
        else
        {
            msg = message;
        }

        MuleClient client = new MuleClient(muleContext);
        Map<String, Object> props = null;
        if (charset != null)
        {
            props = new HashMap<String, Object>();
            props.put(MailProperties.CONTENT_TYPE_PROPERTY, "text/plain; charset=" + charset);
        } 
        client.dispatch("vm://send", msg, props);

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
        verifyMessage((String) received.getContent());
        
        Address[] recipients = received.getRecipients(Message.RecipientType.TO);
        assertNotNull(recipients);
        assertEquals("number of recipients", 1, recipients.length);
        assertEquals("recipient", email, recipients[0].toString());
    }

    protected void verifyMessage(String receivedText)
    {
        // for some reason, something is adding a newline at the end of messages
        // so we need to strip that out for comparison
        assertEquals(message, receivedText.trim());
    }

    protected void doRequest() throws Exception
    {
        assertEquals(1, server.getReceivedMessages().length);

        MuleClient client = new MuleClient(muleContext);
        MuleMessage reply = client.request("vm://receive", RECEIVE_TIMEOUT);
        
        assertNotNull(reply);
        Object payload = reply.getPayload();
        if (isMimeMessage)
        {
            assertTrue("payload is " + payload.getClass().getName(), payload instanceof MimeMessage);
            verifyMessage((MimeMessage) payload);
        }
        else
        {
            assertTrue(payload instanceof String);
            verifyMessage((String) payload);
        }
    }

    private void startServer() throws Exception
    {
        logger.debug("starting server on port " + port);
        ServerSetup setup = new ServerSetup(port, null, protocol);
        server = new GreenMail(setup);
        server.start();
        if (protocol.startsWith(Pop3Connector.POP3) || protocol.startsWith(ImapConnector.IMAP))
        {
            GreenMailUtilities.storeEmail(server.getManagers().getUserManager(),
                    email, user, password,
                    GreenMailUtilities.toMessage(message, email, charset));
        }
        logger.debug("server started for protocol " + protocol);
    }

    private void stopServer()
    {
        server.stop();
    }

    private static String getMessage(Locale locale) {
        return LocaleMessageHandler.getString("test-data", locale, "AbstractEmailFunctionalTestCase.getMessage", new Object[] {});
    }
}
