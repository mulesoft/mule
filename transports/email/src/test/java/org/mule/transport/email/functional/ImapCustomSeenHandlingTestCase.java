/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.functional;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.tck.functional.CountdownCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.transport.email.GreenMailUtilities;
import org.mule.transport.email.ImapConnector;

import com.icegreen.greenmail.store.StoredMessage;
import com.icegreen.greenmail.user.UserManager;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class ImapCustomSeenHandlingTestCase extends AbstractEmailFunctionalTestCase
{
    private CountdownCallback messageReceived = new CountdownCallback(1);

    public ImapCustomSeenHandlingTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, false, ImapConnector.IMAP, configResources);

        // do not start Mule the IMAP server must be filled with appropriate test data first
        setStartContext(false);
    }

    @Override
    protected String getConfigFile()
    {
        return "imap-custom-seen-flag.xml";
    }
    
    @Parameters
    public static Collection<Object[]> parameters()
    {
        // the (second) config resources parameter must be null - we override
        // getConfigFile() which provides the actual config that's used
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, null},
            {ConfigVariant.FLOW, null}
        });
    }

    @Test
    public void testMessagesMatchingFilterGetCustomFlagSet() throws Exception
    {
        putMailMessageMatchingFilterIntoImapServer();
        setupTestComponentListener();

        // now that everything is set up, start the context and let the polling begin
        muleContext.start();

        // wait for the message to be processed
        assertTrue(messageReceived.await(RECEIVE_TIMEOUT));
        assertMessageIsFlagged();
    }

    private void putMailMessageMatchingFilterIntoImapServer() throws Exception
    {
        String email = "barney@mule.org";

        UserManager userManager = server.getManagers().getUserManager();
        MimeMessage message = GreenMailUtilities.toMessage(TEST_MESSAGE, email, null);
        GreenMailUtilities.storeEmail(userManager, email, DEFAULT_USER, DEFAULT_PASSWORD, message);
    }

    private void setupTestComponentListener() throws Exception
    {
        FunctionalTestComponent component = getFunctionalTestComponent("custom-flags");
        assertNotNull(component);

        component.setEventCallback(messageReceived);
    }

    private void assertMessageIsFlagged() throws Exception
    {
        // our superclass puts one message per default so we definitely have more than one message
        // here. Just check if one is there with the required flag
        boolean flaggedMessageFound = anyMessageIsFlagged();
        assertTrue("no FLAGGED message found", flaggedMessageFound);
    }

    private boolean anyMessageIsFlagged()
    {
        for (StoredMessage message : allImapMessages())
        {
            if (message.getFlags().contains(Flags.Flag.FLAGGED))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * We cannot use <code>server.getReceivedMessages()</code> to obtain all messages from the
     * IMAP server, the {@link MimeMessage} instances that are returned by that method do not
     * have all the flags set.
     */
    @SuppressWarnings("unchecked")
    private List<StoredMessage> allImapMessages()
    {
        return server.getManagers().getImapHostManager().getAllMessages();
    }
}
