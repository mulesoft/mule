/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.axis.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

public class AxisMessages extends MessageFactory
{
    private static final String BUNDLE_PATH = getBundlePath("axis");

    public static Message objectMustImplementAnInterface(String name)
    {
        return createMessage(BUNDLE_PATH, 1, name);
    }

    public static String serverProviderAndServerConfigConfigured()
    {
        return getString(BUNDLE_PATH, 2);
    }

    public static String clientProviderAndClientConfigConfigured()
    {
        return getString(BUNDLE_PATH, 3);
    }
}


