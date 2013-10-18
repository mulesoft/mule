/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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


