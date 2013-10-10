/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.email;

import org.mule.api.MuleContext;

/**
 * Creates a secure IMAP connection
 */
public class ImapsConnector extends AbstractTlsRetrieveMailConnector
{
    
    public static final String IMAPS = "imaps";
    public static final int DEFAULT_IMAPS_PORT = 993;

    public ImapsConnector(MuleContext context)
    {
        super(DEFAULT_IMAPS_PORT, ImapsSocketFactory.MULE_IMAPS_NAMESPACE, ImapsSocketFactory.class, context);
    }
  
    public String getProtocol()
    {
        return IMAPS;
    }
    
    public String getBaseProtocol()
    {
        return ImapConnector.IMAP;
    }

}
