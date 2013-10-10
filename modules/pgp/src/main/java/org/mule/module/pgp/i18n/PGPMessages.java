/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

public class PGPMessages extends MessageFactory
{
    private static final PGPMessages factory = new PGPMessages();
    
    private static final String BUNDLE_PATH = getBundlePath("pgp");

    public static Message noPublicKeyForUser(String userId)
    {
        return factory.createMessage(BUNDLE_PATH, 1, userId);
    }

    public static Message noSignedMessageFound()
    {
        return factory.createMessage(BUNDLE_PATH, 2);
    }

    public static Message invalidSignature()
    {
        return factory.createMessage(BUNDLE_PATH, 3);
    }

    public static Message errorVerifySignature()
    {
        return factory.createMessage(BUNDLE_PATH, 4);
    }

    public static Message encryptionStrategyNotSet()
    {
        return factory.createMessage(BUNDLE_PATH, 5);
    }
    
    public static Message pgpPublicKeyExpired()
    {
        return factory.createMessage(BUNDLE_PATH, 6);
    }
    
    public static Message noSecretKeyFoundButAvailable(String availableKeys)
    {
        return factory.createMessage(BUNDLE_PATH, 7, availableKeys);
    }

}


