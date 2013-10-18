/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.client.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

public class ClientMessages extends MessageFactory
{
    private static final ClientMessages factory = new ClientMessages();
    
    private static final String BUNDLE_PATH = getBundlePath("client");

    public static Message failedToDispatchClientEvent()
    {
        return factory.createMessage(BUNDLE_PATH, 1);
    }

    public static Message noArgsForProxy()
    {
        return factory.createMessage(BUNDLE_PATH, 2);
    }

    public static Message managerIsAlreadyConfigured()
    {
        return factory.createMessage(BUNDLE_PATH, 3);
    }

    public static Message unsupportedServerWireForat(String wireFormat)
    {
        return factory.createMessage(BUNDLE_PATH, 4, wireFormat);
    }

    public static Message failedToDispatchActionNoResponseFromServer(String action, int timeout)
    {
        return factory.createMessage(BUNDLE_PATH, 5, action, new Integer(timeout));
    }

    public static Message failedToDeserializeHandshakeFromServer()
    {
        return factory.createMessage(BUNDLE_PATH, 6);
    }

    public static Message noSuchFlowConstruct(String name)
    {
        return factory.createMessage(BUNDLE_PATH, 7, name);
    }
}


