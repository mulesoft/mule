/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
