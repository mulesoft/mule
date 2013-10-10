/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.quartz.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

public class QuartzMessages extends MessageFactory
{
    private static final QuartzMessages factory = new QuartzMessages();
    
    private static final String BUNDLE_PATH = getBundlePath("quartz");

    public static Message cronExpressionOrIntervalMustBeSet()
    {
        return factory.createMessage(BUNDLE_PATH, 1);
    }

    public static Message invalidPayloadType()
    {
        return factory.createMessage(BUNDLE_PATH, 2);
    }

    public static Message invalidJobObject()
    {
        return factory.createMessage(BUNDLE_PATH, 3);
    }

    public static Message missingJobDetail(String detail)
    {
        return factory.createMessage(BUNDLE_PATH, 4, detail);
    }

    public static Message receiverNotInJobDataMap()
    {
        return factory.createMessage(BUNDLE_PATH, 5);
    }

    public static Message connectorNotInJobDataMap()
    {
        return factory.createMessage(BUNDLE_PATH, 6);
    }

    public static Message noConnectorFound(String connectorName)
    {
        return factory.createMessage(BUNDLE_PATH, 7, connectorName);
    }

    public static Message noReceiverInConnector(String receiver, String connector)
    {
        return factory.createMessage(BUNDLE_PATH, 8, receiver, connector);
    }
}
