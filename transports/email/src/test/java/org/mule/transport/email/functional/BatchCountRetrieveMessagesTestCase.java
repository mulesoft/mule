/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.functional;

import static org.mule.tck.AbstractServiceAndFlowTestCase.ConfigVariant.FLOW;

import java.util.Arrays;
import java.util.Collection;

import org.junit.runners.Parameterized;

public class BatchCountRetrieveMessagesTestCase extends AbstractImapRetrieveMessagesTestCase
{

    protected static final int INITIAL_READ_MESSAGES = 0;

    private static final int INITIAL_UNREAD_MESSAGES = 15;
    
    protected static final Collection<Object[]> READ_MESSAGES_PARAMETERS = Arrays.asList(new Object[][] {
        {INITIAL_READ_MESSAGES, INITIAL_UNREAD_MESSAGES}});
    
    public BatchCountRetrieveMessagesTestCase(int initialReadMessages, int initialUnreadMessages)
    {
        super(FLOW, "imap-batch-size-messages-test.xml", initialReadMessages, initialUnreadMessages);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return READ_MESSAGES_PARAMETERS;
    }
}
