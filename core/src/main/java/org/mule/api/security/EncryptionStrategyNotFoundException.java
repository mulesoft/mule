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
 * <code>EncryptionStrategyNotFoundException</code> is thrown by the
 * SecurityManager when an encryption scheme is set in a property or header that
 * has not been registered witrh the manager
 */
public class EncryptionStrategyNotFoundException extends MuleException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 3916371211189075139L;

    public EncryptionStrategyNotFoundException(String strategyName)
    {
        super(CoreMessages.authNoEncryptionStrategy(strategyName));
    }

    public EncryptionStrategyNotFoundException(String strategyName, Throwable cause)
    {
        super(CoreMessages.authNoEncryptionStrategy(strategyName), cause);
    }
}
