/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
