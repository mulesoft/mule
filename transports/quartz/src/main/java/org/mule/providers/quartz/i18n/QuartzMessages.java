/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.quartz.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

public class QuartzMessages extends MessageFactory
{
    private static final String BUNDLE_PATH = getBundlePath("quartz");

    public static Message cronExpressionOrIntervalMustBeSet()
    {
        return createMessage(BUNDLE_PATH, 1);
    }

    public static Message invalidPayloadType()
    {
        return createMessage(BUNDLE_PATH, 2);
    }

    public static Message invalidJobObject()
    {
        return createMessage(BUNDLE_PATH, 3);
    }

    public static Message missingJobDetail(String detail)
    {
        return createMessage(BUNDLE_PATH, 4, detail);
    }

    public static Message receiverNotInJobDataMap()
    {
        return createMessage(BUNDLE_PATH, 5);
    }

    public static Message connectorNotInJobDataMap()
    {
        return createMessage(BUNDLE_PATH, 6);
    }

    public static Message noConnectorFound(String connectorName)
    {
        return createMessage(BUNDLE_PATH, 7, connectorName);
    }

    public static Message noReceiverInConnector(String receiver, String connector)
    {
        return createMessage(BUNDLE_PATH, 8, receiver, connector);
    }
}
