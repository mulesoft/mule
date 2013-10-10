/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.stdio.i18n;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;
import org.mule.transport.stdio.StdioConnector;

public class StdioMessages extends MessageFactory
{
    private static final StdioMessages factory = new StdioMessages();
    
    private static final String BUNDLE_PATH = getBundlePath(StdioConnector.STDIO);

    public static Message couldNotFindStreamWithName(ImmutableEndpoint endpoint)
    {
        String address = endpoint.getEndpointURI().getAddress();
        return factory.createMessage(BUNDLE_PATH, 1, address);
    }
}


