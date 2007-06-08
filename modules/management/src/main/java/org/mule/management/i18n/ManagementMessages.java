/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

public class ManagementMessages extends MessageFactory
{
    private static final String BUNDLE_PATH = getBundlePath("management");

    public static Message createOrLocateShouldBeSet()
    {
        return createMessage(BUNDLE_PATH, 1);
    }

    public static Message cannotLocateOrCreateServer()
    {
        return createMessage(BUNDLE_PATH, 2);
    }

    public static Message noMBeanServerAvailable()
    {
        return createMessage(BUNDLE_PATH, 3);
    }
}


