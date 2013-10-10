/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.xmpp;

import org.mule.api.MuleException;
import org.mule.api.service.Service;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public abstract class AbstractXmppTestCase extends XmppEnableDisableTestCase
{

    private static final long STARTUP_TIMEOUT = 5000;

    private CountDownLatch jabberLatch;
    protected JabberClient jabberClient;
    protected String conversationPartner;
    protected String muleJabberUserId;
    protected static final String COMMON_CONFIG = "xmpp-connector-config.xml";

    public AbstractXmppTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        jabberLatch = new CountDownLatch(1);
        createAndConnectJabberClient();
    }

    private void createAndConnectJabberClient() throws Exception
    {
        // do not hardcode host/user etc here, look it up from the registry so the
        // only place
        // that this info is stored is in the config
        Properties properties = (Properties) muleContext.getRegistry().lookupObject("properties");
        String host = properties.getProperty("host");
        conversationPartner = properties.getProperty("conversationPartner");
        String password = properties.getProperty("conversationPartnerPassword");

        // also save the jid that is used to connect to the jabber server
        muleJabberUserId = properties.getProperty("user") + "@" + host;

        jabberClient = new JabberClient(host, conversationPartner, password);
        configureJabberClient(jabberClient);
        jabberClient.connect(jabberLatch);

        assertTrue(jabberLatch.await(STARTUP_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    protected void configureJabberClient(JabberClient client) throws Exception
    {
        // template method
    }

    @Override
    protected void doTearDown() throws Exception
    {
        if (jabberClient != null)
        {
            jabberClient.disconnect();
        }
        super.doTearDown();
    }

    protected void startService(String serviceName) throws MuleException
    {
        Service service = muleContext.getRegistry().lookupService(serviceName);
        assertNotNull(service);

        service.start();
    }

    protected void startSendThread(JabberSender sender)
    {
        Thread sendThread = new Thread(sender);
        sendThread.setName("Jabber send");
        sendThread.start();
    }

    protected void assertReceivedPacketEqualsMessageSent(Packet packet)
    {
        assertNotNull(packet);
        assertTrue(packet instanceof Message);
        Message messageFromJabber = (Message) packet;
        assertEquals(TEST_MESSAGE, messageFromJabber.getBody());
    }
}
