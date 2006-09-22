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
 * Receives messages from an Imap mailbox
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ImapConnector extends Pop3Connector {

    public static final int DEFAULT_IMAP_PORT = 143;

    /** Default is INBOX. */
    private String mailboxFolder = Pop3Connector.MAILBOX;

    public String getProtocol()
    {
        return "imap";
    }

    public int getDefaultPort() {
        return DEFAULT_IMAP_PORT;
    }


    public String getMailboxFolder() {
        return mailboxFolder;
    }

    public void setMailboxFolder(String mailboxFolder) {
        this.mailboxFolder = mailboxFolder;
    }
}
