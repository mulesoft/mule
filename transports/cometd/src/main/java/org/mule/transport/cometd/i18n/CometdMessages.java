/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.cometd.i18n;

import org.mule.config.i18n.MessageFactory;
import org.mule.config.i18n.Message;

/**
 * TODO
 */
public class CometdMessages extends MessageFactory
{
    private static final CometdMessages factory = new CometdMessages();

    private static final String BUNDLE_PATH = getBundlePath("cometd");

    public static Message failedToStartCometdServlet()
    {
        return factory.createMessage(BUNDLE_PATH, 1);
    }

    public static Message noConnectorForProtocol(String protocol)
    {
        return factory.createMessage(BUNDLE_PATH, 2, protocol);
    }

    public static Message noCometdConnectorWithName(String name, String param)
    {
        return factory.createMessage(BUNDLE_PATH, 3, name, param);
    }
}