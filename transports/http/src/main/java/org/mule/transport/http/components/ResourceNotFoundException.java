/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.components;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.config.i18n.Message;

/**
 * Thrown when a static file is requested but not found
 */
public class ResourceNotFoundException extends MessagingException
{

    private static final long serialVersionUID = -6693780652453067693L;

    public ResourceNotFoundException(Message message, MuleEvent event)
    {
        super(message, event);
    }

}
