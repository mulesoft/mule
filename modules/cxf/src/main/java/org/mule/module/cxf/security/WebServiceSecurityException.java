/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.security;

import org.mule.api.MuleEvent;
import org.mule.config.i18n.MessageFactory;

public class WebServiceSecurityException extends org.mule.api.security.SecurityException
{
    public WebServiceSecurityException(MuleEvent event, Throwable cause)
    {
        super(MessageFactory.createStaticMessage(
            "Security exception occurred invoking web service\nEndpoint = " 
            + event.getMessageSourceURI()
            + "\nSecurity provider(s) = " + event.getMuleContext().getSecurityManager().getProviders()
            + "\nEvent = " + event),
            event, cause);
    }
}


