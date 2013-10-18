/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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


