/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.email;

/**
 * Receives messages from an IMAP mailbox
 */
public class ImapConnector extends AbstractRetrieveMailConnector
{

    public static final String IMAP = "imap";
    public static final int DEFAULT_IMAP_PORT = 143;

    public ImapConnector()
    {
        super(DEFAULT_IMAP_PORT);
    }
    
    public String getProtocol()
    {
        return IMAP;
    }

}
