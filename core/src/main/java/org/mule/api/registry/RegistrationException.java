/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.registry;

import org.mule.api.MuleException;
import org.mule.config.i18n.Message;

public class RegistrationException extends MuleException
{
    private static final long serialVersionUID = 9143114426140546637L;

    public RegistrationException(Message message)
    {
        super(message);
    }
    
    public RegistrationException(Throwable cause)
    {
        super(cause);
    }

    public RegistrationException(Message message, Throwable cause)
    {
        super(message, cause);
    }
    
}

