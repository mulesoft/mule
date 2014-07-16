/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email;

import org.mule.api.MuleContext;

/**
 * Receives messages from an IMAP mailbox
 */
public class ImapConnector extends AbstractRetrieveMailConnector
{

    public static final String IMAP = "imap";
    public static final int DEFAULT_IMAP_PORT = 143;

    public ImapConnector(MuleContext context)
    {
        super(DEFAULT_IMAP_PORT, context);
    }
    
    public String getProtocol()
    {
        return IMAP;
    }

}
