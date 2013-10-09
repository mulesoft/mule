/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.security;

import org.mule.api.MuleException;
import org.mule.config.i18n.CoreMessages;

/**
 * <code>SecurityProviderNotFoundException</code> is thrown by the
 * SecurityManager when an authentication request is made but no suitable security
 * provider can be found to process the authentication
 */
public class SecurityProviderNotFoundException extends MuleException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 124630897095610595L;

    public SecurityProviderNotFoundException(String providerName)
    {
        super(CoreMessages.authNoSecurityProvider(providerName));
    }

    public SecurityProviderNotFoundException(String providerName, Throwable cause)
    {
        super(CoreMessages.authNoSecurityProvider(providerName), cause);
    }
}
