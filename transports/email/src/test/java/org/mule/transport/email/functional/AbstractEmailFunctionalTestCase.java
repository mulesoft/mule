/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.config.i18n.LocaleMessageHandler;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.email.GreenMailUtilities;
import org.mule.transport.email.ImapConnector;
import org.mule.transport.email.Pop3Connector;
import org.mule.util.SystemUtils;

import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.junit.Rule;

public abstract class AbstractEmailFunctionalTestCase extends AbstractServiceAndFlowTestCase
{
    public static final long DELIVERY_DELAY_MS = 10000;

    protected static final String CONFIG_BASE = "-functional-test.xml";
    protected static final boolean MIME_MESSAGE = true;
    protected static final boolean STRING_MESSAGE = false;

    protected static final String DEFAULT_EMAIL = "bob@example.com";
    protected static final String DEFAULT_USER = "bob";
    protected static final String DEFAULT_MESSAGE = "Test email message";
    protected static final String DEFAULT_PASSWORD = "password";
    protected static final String DEFAULT_PROCESSED_MAILBOX = "processed";

    private String protocol;
    private boolean isMimeMessage;
    private int port;
    protected GreenMail server;
    private String email;
    private String user;
    private String message;
    private String password;
    private String charset;
    private boolean addAttachments;
    protected ServerSetup setup = null;
    // for tests which need to send emails in addition to receiving them
    protected ServerSetup smtpSetup = null;
    private int smtpPort;
    private boolean addSmtp = false;

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");


    protected AbstractEmailFunctionalTestCase(ConfigVariant variant, boolean isMimeMessage, String protocol)
    {
        this(variant, isMimeMessage, protocol, protocol + CONFIG_BASE, null, null);
    }

    protected AbstractEmailFunctionalTestCase(ConfigVariant variant, boolean isMimeMessage, String protocol, Locale locale, String charset)
    {
        this(variant, isMimeMessage, protocol, protocol + CONFIG_BASE, locale, charset);
    }

    protected AbstractEmailFunctionalTestCase(ConfigVariant variant, boolean isMimeMessage, String protocol, String configResources)
    {
        this(variant, isMimeMessage, protocol, configResources, null, null);
    }

    protected AbstractEmailFunctionalTestCase(ConfigVariant variant, boolean isMimeMessage, String protocol, String configResources, String message)
    {
        this(variant, isMimeMessage, protocol, configResources, DEFAULT_EMAIL, DEFAULT_USER, message, DEFAULT_PASSWORD, null);
    }

    protected AbstractEmailFunctionalTestCase(ConfigVariant variant, boolean isMimeMessage, String protocol, String configResources, boolean addSmtp)
    {
        this(variant, isMimeMessage, protocol, configResources, null, null);
        this.addSmtp = addSmtp;
    }

    protected AbstractEmailFunctionalTestCase(ConfigVariant variant, boolean isMimeMessage, String protocol, String configResources, Locale locale, String charset)
    {
        this(variant, isMimeMessage, protocol, configResources,
                DEFAULT_EMAIL, DEFAULT_USER, (locale == null ? DEFAULT_MESSAGE : getMessage(locale)), DEFAULT_PASSWORD, charset);
    }

    protected AbstractEmailFunctionalTestCase(ConfigVariant variant, boolean isMimeMessage, String protocol,
        String configResources, String email, String user, String message, String password, String charset)
    {
        super(variant, configResources);
        this.isMimeMessage = isMimeMessage;
        this.protocol = protocol;
        this.email = email;
        this.user = user;
        this.message = message;
        this.password = password;
        this.charset = charset;
    }

    @Override
    protected MuleContext createMuleContext() throws Exception
    {
        this.port = dynamicPort1.getNumber();
        this.smtpPort = dynamicPort2.getNumber();
        startServer();
        initDefaultCommandMap();

        return super.createMuleContext();
    }

    /**
     * This is required to make all tests work on JDK5.
     */
    private void initDefaultCommandMap()
    {
        if (SystemUtils.JAVA_VERSION_FLOAT < 1.6f)
        {
            MailcapCommandMap commandMap = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
            commandMap.addMailcap("application/xml;;  x-java-content-handler=com.sun.mail.handlers.text_plain");
            commandMap.addMailcap("application/text;; x-java-content-handler=com.sun.mail.handlers.text_plain");
            CommandMap.setDefaultCommandMap(commandMap);
        }
    }

    @Override
    public void doTearDown()
    {
        server.stop();
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

        MuleClient client = muleContext.getClient();
        if (addAttachments)
        {
            MuleMessage muleMessage = new DefaultMuleMessage(msg, muleContext);
            createOutboundAttachments(muleMessage);
            client.dispatch("vm://send", muleMessage);
        }
        else
        {
            client.dispatch("vm://send", msg, null);
        }

        server.waitForIncomingEmail(DELIVERY_DELAY_MS, 1);

        MimeMessage[] messages = server.getReceivedMessages();
        assertNotNull("did not receive any messages", messages);
        assertEquals("did not receive 1 mail", 1, messages.length);
        verifyMessage(messages[0]);
    }

    protected void verifyMessage(MimeMessage received) throws Exception
    {
        if (addAttachments)
        {
            assertTrue("Did not receive a multipart message",
                received.getContent() instanceof MimeMultipart);
            verifyMessage((MimeMultipart) received.getContent());
        }
        else
        {
            assertTrue("Did not receive a message with String contents",
                received.getContent() instanceof String);
            verifyMessage((String) received.getContent());
        }

        Address[] recipients = received.getRecipients(Message.RecipientType.TO);
        assertNotNull(recipients);
        assertEquals("number of recipients", 1, recipients.length);
        assertEquals("recipient", email, recipients[0].toString());
    }

    protected void verifyMessage(MimeMultipart mimeMultipart) throws Exception
    {
        fail("multipart message was not expected");
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

        MuleClient client = muleContext.getClient();
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

        setup = new ServerSetup(port, null, protocol);
        if(addSmtp)
        {
            smtpSetup = new ServerSetup(smtpPort, null, "smtp");
            server = new GreenMail(new ServerSetup[]{setup, smtpSetup});
        }
        else
        {
            server = new GreenMail(setup);
        }

        server.getManagers().getUserManager().createUser(email, user, password);
        GreenMailUser gmUser = server.getManagers().getUserManager().getUser(user);
        assert null != gmUser;
        server.getManagers().getImapHostManager().createMailbox(
                server.getManagers().getUserManager().getUser(DEFAULT_USER),
                DEFAULT_PROCESSED_MAILBOX);

        server.start();
        if (protocol.startsWith(Pop3Connector.POP3) || protocol.startsWith(ImapConnector.IMAP))
        {
            generateAndStoreEmail();
        }
        logger.debug("server started for protocol " + protocol);
    }

    /**
     * Generates and store emails on the server.
     *
     * @throws Exception If there's a problem with the storing of the messages in the server.
     */
    protected void generateAndStoreEmail() throws Exception
    {
        List<MimeMessage> messages = new ArrayList<MimeMessage>();
        messages.add(GreenMailUtilities.toMessage(message, email, charset));
        storeEmail(messages);
    }

    /**
     * Helper method to store email on the server. Can be overriden by subclasses if other tests want to store
     * a different list of messages.
     *
     * @param messages The list of messages to be stored.
     * @throws Exception If there's a problem with the storing of the messages in the server.
     */
    protected void storeEmail(List<MimeMessage> messages) throws Exception
    {
        for (MimeMessage message : messages)
        {
            GreenMailUtilities.storeEmail(server.getManagers().getUserManager(),
                                          email, user, password, message);
        }
    }

    private void stopServer()
    {
        server.stop();
    }

    private static String getMessage(Locale locale)
    {
        return LocaleMessageHandler.getString("test-data", locale, "AbstractEmailFunctionalTestCase.getMessage", new Object[] {});
    }

    public void setAddAttachments(boolean addAttachments)
    {
        this.addAttachments = addAttachments;
    }

    private void createOutboundAttachments(MuleMessage msg) throws Exception
    {
        msg.addOutboundAttachment("hello", "hello", "text/plain");
        msg.addOutboundAttachment("goodbye", "<a/>", "text/xml");
    }
}
