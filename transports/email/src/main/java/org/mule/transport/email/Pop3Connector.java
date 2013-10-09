/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.email;

import org.mule.api.MuleContext;


/**
 * <code>Pop3Connector</code> is used to connect and receive mail from a POP3
 * mailbox.
 */
public class Pop3Connector extends AbstractRetrieveMailConnector
{

    public static final String POP3 = "pop3";
    public static final int DEFAULT_POP3_PORT = 110;

    public Pop3Connector(MuleContext context)
    {
        super(DEFAULT_POP3_PORT, context);
    }
    
    public String getProtocol()
    {
        return POP3;
    }

}
