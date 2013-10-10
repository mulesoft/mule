/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
