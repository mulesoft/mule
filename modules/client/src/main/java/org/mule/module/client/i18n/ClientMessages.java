/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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


