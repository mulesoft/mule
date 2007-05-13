/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.pgp.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

public class PGPMessages extends MessageFactory
{
    private static final String BUNDLE_PATH = getBundlePath("pgp");

    public static Message noPublicKeyForUser(String userId)
    {
        return createMessage(BUNDLE_PATH, 1, userId);
    }

    public static Message noSignedMessageFound()
    {
        return createMessage(BUNDLE_PATH, 2);
    }

    public static Message invalidSignature()
    {
        return createMessage(BUNDLE_PATH, 3);
    }

    public static Message errorVerifySignature()
    {
        return createMessage(BUNDLE_PATH, 4);
    }

    public static Message encryptionStrategyNotSet()
    {
        return createMessage(BUNDLE_PATH, 5);
    }
}


