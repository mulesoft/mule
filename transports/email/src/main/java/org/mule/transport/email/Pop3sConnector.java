/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
