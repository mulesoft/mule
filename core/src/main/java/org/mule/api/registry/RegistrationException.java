/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

