/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ajax.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

/**
 * i18n messages for the Ajax transport
 */
public class AjaxMessages extends MessageFactory
{
    private static final AjaxMessages FACTORY = new AjaxMessages();

    private static final String BUNDLE_PATH = getBundlePath("ajax");

    public static Message failedToStartAjaxServlet()
    {
        return FACTORY.createMessage(BUNDLE_PATH, 1);
    }

    public static Message noConnectorForProtocol(String protocol)
    {
        return FACTORY.createMessage(BUNDLE_PATH, 2, protocol);
    }

    public static Message noAjaxConnectorWithName(String name, String param)
    {
        return FACTORY.createMessage(BUNDLE_PATH, 3, name, param);
    }

    public static Message serverUrlNotDefined()
    {
        return FACTORY.createMessage(BUNDLE_PATH, 4);
    }
}
