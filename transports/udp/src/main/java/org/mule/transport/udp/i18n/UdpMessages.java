/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.udp.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;
import org.mule.transport.udp.UdpConnector;

import java.net.URI;

public class UdpMessages extends MessageFactory
{
    private static final UdpMessages factory = new UdpMessages();
    
    private static final String BUNDLE_PATH = getBundlePath(UdpConnector.UDP);

    public static Message failedToBind(URI uri)
    {
        return factory.createMessage(BUNDLE_PATH, 1, uri);
    }

    public static Message failedToLocateHost(URI uri)
    {
        return factory.createMessage(BUNDLE_PATH, 2, uri);
    }
}


