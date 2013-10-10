/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
