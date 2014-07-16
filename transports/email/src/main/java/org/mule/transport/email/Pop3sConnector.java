/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email;

import org.mule.api.MuleContext;


/**
 * Creates a secure connection to a POP3 mailbox
 */
public class Pop3sConnector extends AbstractTlsRetrieveMailConnector
{

    public static final String POP3S = "pop3s";
    public static final int DEFAULT_POP3S_PORT = 995;

    public Pop3sConnector(MuleContext context)
    {
        super(DEFAULT_POP3S_PORT, Pop3sSocketFactory.MULE_POP3S_NAMESPACE, Pop3sSocketFactory.class, context);
    }
    
    public String getProtocol()
    {
        return "pop3s";
    }
    
    public String getBaseProtocol()
    {
        return "pop3";
    }

}
