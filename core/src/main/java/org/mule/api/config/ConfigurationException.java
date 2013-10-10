/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.config;

import org.mule.api.MuleException;
import org.mule.config.i18n.Message;

public class ConfigurationException extends MuleException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 3658822340943734960L;

    public ConfigurationException(Message message)
    {
        super(message);
    }

    public ConfigurationException(Message message, Throwable cause)
    {
        super(message, cause);
    }

    public ConfigurationException(Throwable cause)
    {
        super(cause);
    }
}
