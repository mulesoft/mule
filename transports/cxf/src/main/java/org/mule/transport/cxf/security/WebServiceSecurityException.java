/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf.security;

import org.mule.api.MuleEvent;
import org.mule.config.i18n.MessageFactory;

public class WebServiceSecurityException extends org.mule.api.security.SecurityException
{
    public WebServiceSecurityException(MuleEvent event, Throwable cause)
    {
        super(MessageFactory.createStaticMessage(
            "Security exception occurred invoking web service\nEndpoint = " 
            + event.getEndpoint().getEndpointURI() 
            + "\nSecurity provider(s) = " + event.getMuleContext().getSecurityManager().getProviders()
            + "\nEvent = " + event),
            event.getMessage(), cause);
    }
}


