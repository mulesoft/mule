/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;
import org.mule.transport.tcp.TcpConnector;

import java.net.URI;

public class TcpMessages extends MessageFactory
{
    private static final TcpMessages factory = new TcpMessages();
    
    private static final String BUNDLE_PATH = getBundlePath(TcpConnector.TCP);

    public static Message failedToBindToUri(URI uri)
    {
        return factory.createMessage(BUNDLE_PATH, 1, uri);
    }

    public static Message failedToCloseSocket()
    {
        return factory.createMessage(BUNDLE_PATH, 2);
    }

    public static Message failedToInitMessageReader()
    {
        return factory.createMessage(BUNDLE_PATH, 3);
    }

    public static Message invalidStreamingOutputType(Class c)
    {
        return factory.createMessage(BUNDLE_PATH, 4, c.getName());
    }

    public static Message pollingReceiverCannotbeUsed()
    {
        return factory.createMessage(BUNDLE_PATH, 5);
    }
}


