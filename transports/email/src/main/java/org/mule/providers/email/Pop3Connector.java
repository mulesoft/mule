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
 * <code>Pop3Connector</code> is used to connect and receive mail from a POP3
 * mailbox.
 */
public class Pop3Connector extends AbstractRetrieveMailConnector
{

    public static final String POP3 = "pop3";
    public static final int DEFAULT_POP3_PORT = 110;

    public Pop3Connector()
    {
        super(DEFAULT_POP3_PORT);
    }
    
    public String getProtocol()
    {
        return POP3;
    }

}
