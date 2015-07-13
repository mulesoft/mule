/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.functional;

import static org.mule.tck.AbstractServiceAndFlowTestCase.ConfigVariant.FLOW;

import java.util.Collection;

import org.junit.runners.Parameterized;

public class ImapRetrieveMessagesDeleteTestCase extends AbstractImapRetrieveMessagesTestCase
{

    public ImapRetrieveMessagesDeleteTestCase(int initialReadMessages)
    {
        super(FLOW, "imap-delete-messages-test.xml", initialReadMessages);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return READ_MESSAGES_PARAMETERS;
    }
}
