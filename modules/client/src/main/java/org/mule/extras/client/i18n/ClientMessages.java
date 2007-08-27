/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.client.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

public class ClientMessages extends MessageFactory
{
    private static final String BUNDLE_PATH = getBundlePath("client");

    public static Message failedToDispatchClientEvent()
    {
        return createMessage(BUNDLE_PATH, 1);
    }

    public static Message noArgsForProxy()
    {
        return createMessage(BUNDLE_PATH, 2);
    }

    public static Message managerIsAlreadyConfigured()
    {
        return createMessage(BUNDLE_PATH, 3);
    }
}


