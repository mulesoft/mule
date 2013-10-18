/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
