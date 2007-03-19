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
 * Creates a secure IMAP connection
 */
public class ImapsConnector extends Pop3sConnector
{
    public static final int DEFAULT_IMAPS_PORT = 993;

    private String mailboxFolder = Pop3sConnector.MAILBOX;

    public String getProtocol()
    {
        return "imaps";
    }

    public int getDefaultPort()
    {
        return DEFAULT_IMAPS_PORT;
    }

    public String getMailboxFolder()
    {
        return mailboxFolder;
    }

    public void setMailboxFolder(String mailboxFolder)
    {
        this.mailboxFolder = mailboxFolder;
    }

}
