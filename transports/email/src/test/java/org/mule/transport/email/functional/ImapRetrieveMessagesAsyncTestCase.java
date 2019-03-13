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

public class ImapRetrieveMessagesAsyncTestCase extends AbstractImapRetrieveMessagesTestCase
{

    public ImapRetrieveMessagesAsyncTestCase(int initialReadMessages)
    {
        super(FLOW, "imap-reconnect-forever-non-blocking.xml", initialReadMessages);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {{READ_MESSAGES_GREATER_THAN_BATCH_SIZE}});
    }
}
