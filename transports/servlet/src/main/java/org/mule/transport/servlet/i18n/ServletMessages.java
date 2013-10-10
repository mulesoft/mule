/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.servlet.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

public class ServletMessages extends MessageFactory
{
    private static final ServletMessages factory = new ServletMessages();

    private static final String BUNDLE_PATH = getBundlePath("servlet");

    public static Message failedToReadPayload(String string)
    {
        return factory.createMessage(BUNDLE_PATH, 3, string);
    }

    public static Message noConnectorForProtocolServlet()
    {
        return factory.createMessage(BUNDLE_PATH, 4);
    }

    public static Message noServletConnectorFound(String name)
    {
        return factory.createMessage(BUNDLE_PATH, 5, name);
    }

    public static Message failedToProcessRequest()
    {
        return factory.createMessage(BUNDLE_PATH, 6);
    }

}


